package com.library.catalog.application;

import com.library.catalog.application.dto.PublisherRequest;
import com.library.catalog.application.dto.PublisherResponse;
import com.library.catalog.domain.Publisher;
import com.library.shared.exception.ConflictException;
import com.library.shared.exception.ValidationException;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class PublisherServiceImpl implements PublisherService {

    @Inject
    PublisherRepository publisherRepository;

    @Override
    @Transactional
    public PublisherResponse create(PublisherRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Publisher name must not be blank");
        }
        Publisher publisher = new Publisher();
        publisher.name = request.name();
        publisherRepository.persist(publisher);
        return toResponse(publisher);
    }

    @Override
    @Transactional
    public PublisherResponse update(UUID id, PublisherRequest request) {
        Publisher publisher = publisherRepository.findByIdOrThrow(id);
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Publisher name must not be blank");
        }
        publisher.name = request.name();
        publisherRepository.persist(publisher);
        return toResponse(publisher);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Publisher publisher = publisherRepository.findByIdOrThrow(id);
        if (publisherRepository.hasBooks(id)) {
            throw new ConflictException("Publisher has associated books");
        }
        publisherRepository.delete(publisher);
    }

    @Override
    public PageResponse<PublisherResponse> list(PageRequest page) {
        return publisherRepository.findAll(page);
    }

    @Override
    public PublisherResponse getById(UUID id) {
        Publisher publisher = publisherRepository.findByIdOrThrow(id);
        return toResponse(publisher);
    }

    private PublisherResponse toResponse(Publisher publisher) {
        return new PublisherResponse(publisher.id, publisher.name, publisher.createdAt, publisher.updatedAt);
    }
}
