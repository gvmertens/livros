package com.library.reading.application;

import com.library.catalog.domain.Book;
import com.library.reading.application.dto.BookSummary;
import com.library.reading.application.dto.CreateReadingRequest;
import com.library.reading.application.dto.ReadingResponse;
import com.library.reading.application.dto.UpdateReadingRequest;
import com.library.reading.application.event.RatingUpdatedPayload;
import com.library.reading.application.event.ReadingCreatedPayload;
import com.library.reading.application.event.ReviewSubmittedPayload;
import com.library.reading.domain.Reading;
import com.library.reading.domain.ReadingStatus;
import com.library.shared.event.DomainEventEnvelope;
import com.library.shared.event.EventBus;
import com.library.shared.exception.ConflictException;
import com.library.shared.exception.ForbiddenException;
import com.library.shared.exception.UnprocessableEntityException;
import com.library.shared.exception.ValidationException;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ReadingServiceImpl implements ReadingService {

    @Inject
    ReadingRepository readingRepository;

    @Inject
    EventBus eventBus;

    @Override
    @Transactional
    public ReadingResponse create(UUID userId, CreateReadingRequest request) {
        // Check for duplicate reading
        readingRepository.findByUserIdAndBookId(userId, request.bookId()).ifPresent(existing -> {
            throw new ConflictException("Reading already exists for this book");
        });

        // Validate book exists
        Book book = (Book) Book.findByIdOptional(request.bookId())
                .orElseThrow(() -> new UnprocessableEntityException("Book not found: " + request.bookId()));

        Reading reading = new Reading();
        reading.userId = userId;
        reading.bookId = request.bookId();
        reading.status = ReadingStatus.WANT_TO_READ;
        readingRepository.persist(reading);

        eventBus.publish(new DomainEventEnvelope(
                UUID.randomUUID(),
                "reading.created",
                Instant.now(),
                new ReadingCreatedPayload(reading.id, reading.userId, reading.bookId, reading.status.name())));

        return toResponse(reading, new BookSummary(book.id, book.title));
    }

    @Override
    @Transactional
    public ReadingResponse update(UUID requestingUserId, UUID readingId, UpdateReadingRequest request, boolean isAdmin) {
        Reading reading = readingRepository.findByIdOrThrow(readingId);

        // Ownership check
        if (!requestingUserId.equals(reading.userId) && !isAdmin) {
            throw ForbiddenException.accessDenied();
        }

        // Validate rating if provided
        if (request.rating() != null) {
            BigDecimal rating = request.rating();
            if (rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(new BigDecimal("10.0")) > 0) {
                throw new ValidationException("Rating must be between 0.0 and 10.0");
            }
        }

        // Determine effective status after update
        ReadingStatus newStatus = reading.status;
        if (request.status() != null) {
            try {
                newStatus = ReadingStatus.valueOf(request.status());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid status: " + request.status());
            }
        }

        // Determine effective startedAt after update
        Instant effectiveStartedAt = request.startedAt() != null ? request.startedAt() : reading.startedAt;

        // Validate FINISHED requires startedAt
        if (newStatus == ReadingStatus.FINISHED && effectiveStartedAt == null) {
            throw new ValidationException("startedAt is required when status is FINISHED");
        }

        // Track changes for events
        boolean ratingChanged = request.rating() != null
                && !request.rating().equals(reading.rating);
        boolean reviewChanged = request.review() != null
                && !request.review().equals(reading.review);

        // Apply updates
        reading.status = newStatus;
        if (request.rating() != null) {
            reading.rating = request.rating();
        }
        if (request.review() != null) {
            reading.review = request.review();
        }
        if (request.startedAt() != null) {
            reading.startedAt = request.startedAt();
        }
        if (request.finishedAt() != null) {
            reading.finishedAt = request.finishedAt();
        }

        readingRepository.persist(reading);

        // Publish events conditionally
        if (ratingChanged) {
            eventBus.publish(new DomainEventEnvelope(
                    UUID.randomUUID(),
                    "rating.updated",
                    Instant.now(),
                    new RatingUpdatedPayload(reading.id, reading.userId, reading.bookId, reading.rating)));
        }
        if (reviewChanged) {
            eventBus.publish(new DomainEventEnvelope(
                    UUID.randomUUID(),
                    "review.submitted",
                    Instant.now(),
                    new ReviewSubmittedPayload(reading.id, reading.userId, reading.bookId, reading.review)));
        }

        Book book = (Book) Book.findByIdOptional(reading.bookId)
                .orElse(null);
        BookSummary bookSummary = book != null
                ? new BookSummary(book.id, book.title)
                : new BookSummary(reading.bookId, null);

        return toResponse(reading, bookSummary);
    }

    @Override
    @Transactional
    public void delete(UUID requestingUserId, UUID readingId, boolean isAdmin) {
        Reading reading = readingRepository.findByIdOrThrow(readingId);

        if (!requestingUserId.equals(reading.userId) && !isAdmin) {
            throw ForbiddenException.accessDenied();
        }

        readingRepository.delete(reading);
    }

    @Override
    public PageResponse<ReadingResponse> listForUser(UUID userId, ReadingStatus statusFilter, PageRequest page) {
        PageResponse<Reading> readingsPage = readingRepository.findByUserId(userId, statusFilter, page);

        List<ReadingResponse> content = readingsPage.content().stream()
                .map(reading -> {
                    Book book = (Book) Book.findByIdOptional(reading.bookId).orElse(null);
                    BookSummary bookSummary = book != null
                            ? new BookSummary(book.id, book.title)
                            : new BookSummary(reading.bookId, null);
                    return toResponse(reading, bookSummary);
                })
                .toList();

        return new PageResponse<>(content, readingsPage.totalElements(), readingsPage.totalPages(),
                readingsPage.page(), readingsPage.size());
    }

    @Override
    public ReadingResponse getById(UUID requestingUserId, UUID readingId) {
        Reading reading = readingRepository.findByIdOrThrow(readingId);

        if (!requestingUserId.equals(reading.userId)) {
            throw ForbiddenException.accessDenied();
        }

        Book book = (Book) Book.findByIdOptional(reading.bookId).orElse(null);
        BookSummary bookSummary = book != null
                ? new BookSummary(book.id, book.title)
                : new BookSummary(reading.bookId, null);

        return toResponse(reading, bookSummary);
    }

    private ReadingResponse toResponse(Reading reading, BookSummary bookSummary) {
        return new ReadingResponse(
                reading.id,
                bookSummary,
                reading.status.name(),
                reading.rating,
                reading.review,
                reading.startedAt,
                reading.finishedAt,
                reading.createdAt,
                reading.updatedAt);
    }
}
