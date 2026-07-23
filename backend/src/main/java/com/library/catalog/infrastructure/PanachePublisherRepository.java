package com.library.catalog.infrastructure;

import com.library.catalog.application.PublisherRepository;
import com.library.catalog.application.dto.PublisherResponse;
import com.library.catalog.domain.Book;
import com.library.catalog.domain.Publisher;
import com.library.shared.exception.NotFoundException;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanachePublisherRepository implements PublisherRepository, PanacheRepositoryBase<Publisher, UUID> {

    @Override
    public Optional<Publisher> findByNormalizedName(String name) {
        return find("LOWER(TRIM(name)) = ?1", name.toLowerCase().trim()).firstResultOptional();
    }

    @Override
    public Optional<Publisher> findByIdOptional(UUID id) {
        return Optional.ofNullable(getEntityManager().find(Publisher.class, id));
    }

    @Override
    public Publisher findByIdOrThrow(UUID id) {
        return findByIdOptional(id)
                .orElseThrow(() -> NotFoundException.of("Publisher", id));
    }

    @Override
    public boolean hasBooks(UUID publisherId) {
        return Book.count("publisher.id", publisherId) > 0;
    }

    @Override
    public PageResponse<PublisherResponse> findAll(PageRequest page) {
        var pager = findAll().page(page.page(), page.size());
        List<Publisher> publishers = pager.list();
        long total = pager.count();
        List<PublisherResponse> content = publishers.stream()
                .map(p -> new PublisherResponse(p.id, p.name, p.createdAt, p.updatedAt))
                .toList();
        return PageResponse.of(content, total, page);
    }

    @Override
    public void persist(Publisher publisher) {
        if (publisher.id == null) {
            getEntityManager().persist(publisher);
        } else {
            getEntityManager().merge(publisher);
        }
    }

    @Override
    public void delete(Publisher publisher) {
        getEntityManager().remove(
                getEntityManager().contains(publisher) ? publisher : getEntityManager().merge(publisher));
    }
}
