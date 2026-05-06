package com.library.catalog.infrastructure;

import com.library.catalog.application.AuthorRepository;
import com.library.catalog.application.dto.AuthorResponse;
import com.library.catalog.domain.Author;
import com.library.catalog.domain.Book;
import com.library.shared.exception.NotFoundException;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheAuthorRepository implements AuthorRepository, PanacheRepositoryBase<Author, UUID> {

    @Override
    public Optional<Author> findByIdOptional(UUID id) {
        return Optional.ofNullable(getEntityManager().find(Author.class, id));
    }

    @Override
    public Author findByIdOrThrow(UUID id) {
        return findByIdOptional(id)
                .orElseThrow(() -> NotFoundException.of("Author", id));
    }

    @Override
    public boolean hasBooks(UUID authorId) {
        return Book.count("author.id", authorId) > 0;
    }

    @Override
    public PageResponse<AuthorResponse> findAll(PageRequest page) {
        var pager = findAll().page(page.page(), page.size());
        List<Author> authors = pager.list();
        long total = pager.count();
        List<AuthorResponse> content = authors.stream()
                .map(a -> new AuthorResponse(a.id, a.name, a.createdAt, a.updatedAt))
                .toList();
        return PageResponse.of(content, total, page);
    }

    @Override
    public void persist(Author author) {
        if (author.id == null) {
            getEntityManager().persist(author);
        } else {
            getEntityManager().merge(author);
        }
    }

    @Override
    public void delete(Author author) {
        getEntityManager().remove(
                getEntityManager().contains(author) ? author : getEntityManager().merge(author));
    }
}
