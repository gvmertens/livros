package com.library.catalog.infrastructure;

import com.library.catalog.application.BookRepository;
import com.library.catalog.application.dto.AuthorResponse;
import com.library.catalog.application.dto.BookResponse;
import com.library.catalog.application.dto.PublisherResponse;
import com.library.catalog.domain.Book;
import com.library.shared.exception.NotFoundException;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PanacheBookRepository implements BookRepository, PanacheRepositoryBase<Book, UUID> {

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return find("isbn", isbn).firstResultOptional();
    }

    @Override
    public Optional<Book> findByGoogleBooksId(String googleBooksId) {
        return find("googleBooksId", googleBooksId).firstResultOptional();
    }

    @Override
    public Optional<Book> findByIsbn10(String isbn10) {
        return find("isbn10", isbn10).firstResultOptional();
    }

    @Override
    public Optional<Book> findByIsbn13(String isbn13) {
        return find("isbn13", isbn13).firstResultOptional();
    }

    @Override
    public Optional<Book> findByIdOptional(UUID id) {
        return Optional.ofNullable(getEntityManager().find(Book.class, id));
    }

    @Override
    public Book findByIdOrThrow(UUID id) {
        return findByIdOptional(id)
                .orElseThrow(() -> NotFoundException.of("Book", id));
    }

    @Override
    public PageResponse<BookResponse> search(String query, PageRequest page) {
        io.quarkus.hibernate.orm.panache.PanacheQuery<Book> pager;
        if (query == null || query.isBlank()) {
            pager = findAll(Sort.ascending("title")).page(page.page(), page.size());
        } else {
            String q = "%" + query.toLowerCase() + "%";
            pager = find(
                    "SELECT DISTINCT b FROM Book b LEFT JOIN b.authors a " +
                            "WHERE LOWER(b.title) LIKE :q OR LOWER(a.name) LIKE :q ORDER BY b.title ASC",
                    Parameters.with("q", q))
                    .page(page.page(), page.size());
        }
        List<Book> books = pager.list();
        long total = pager.count();
        List<BookResponse> content = books.stream()
                .map(this::toBookResponse)
                .toList();
        return PageResponse.of(content, total, page);
    }

    @Override
    public void persist(Book book) {
        if (book.id == null) {
            getEntityManager().persist(book);
        } else {
            getEntityManager().merge(book);
        }
    }

    @Override
    public void delete(Book book) {
        getEntityManager().remove(
                getEntityManager().contains(book) ? book : getEntityManager().merge(book));
    }

    private BookResponse toBookResponse(Book book) {
        AuthorResponse authorResponse = book.author == null ? null : new AuthorResponse(
                book.author.id, book.author.name, book.author.createdAt, book.author.updatedAt);
        PublisherResponse publisherResponse = book.publisher == null ? null : new PublisherResponse(
                book.publisher.id, book.publisher.name, book.publisher.createdAt, book.publisher.updatedAt);
        return new BookResponse(
                book.id, book.isbn, book.title,
                authorResponse, publisherResponse,
                book.createdAt, book.updatedAt);
    }
}
