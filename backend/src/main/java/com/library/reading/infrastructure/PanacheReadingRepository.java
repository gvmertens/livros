package com.library.reading.infrastructure;

import com.library.reading.application.ReadingRepository;
import com.library.reading.domain.Reading;
import com.library.reading.domain.ReadingStatus;
import com.library.shared.exception.NotFoundException;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheReadingRepository implements ReadingRepository, PanacheRepositoryBase<Reading, UUID> {

    @Override
    public Optional<Reading> findByUserIdAndBookId(UUID userId, UUID bookId) {
        return find("userId = ?1 AND bookId = ?2", userId, bookId).firstResultOptional();
    }

    @Override
    public Reading findByIdOrThrow(UUID id) {
        return findByIdOptional(id)
                .orElseThrow(() -> NotFoundException.of("Reading", id));
    }

    @Override
    public PageResponse<Reading> findByUserId(UUID userId, ReadingStatus status, PageRequest page) {
        io.quarkus.hibernate.orm.panache.PanacheQuery<Reading> query;
        if (status == null) {
            query = find("userId", userId).page(page.page(), page.size());
        } else {
            query = find("userId = ?1 AND status = ?2", userId, status).page(page.page(), page.size());
        }
        List<Reading> readings = query.list();
        long total = query.count();
        return PageResponse.of(readings, total, page);
    }

    @Override
    public void persist(Reading reading) {
        if (reading.id == null) {
            getEntityManager().persist(reading);
        } else {
            getEntityManager().merge(reading);
        }
    }

    @Override
    public void delete(Reading reading) {
        getEntityManager().remove(
                getEntityManager().contains(reading) ? reading : getEntityManager().merge(reading));
    }
}
