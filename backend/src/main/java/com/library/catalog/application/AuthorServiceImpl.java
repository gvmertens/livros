package com.library.catalog.application;

import com.library.catalog.application.dto.AuthorRequest;
import com.library.catalog.application.dto.AuthorResponse;
import com.library.catalog.domain.Author;
import com.library.shared.exception.ConflictException;
import com.library.shared.exception.ValidationException;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class AuthorServiceImpl implements AuthorService {

    @Inject
    AuthorRepository authorRepository;

    @Override
    @Transactional
    public AuthorResponse create(AuthorRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Author name must not be blank");
        }
        Author author = new Author();
        author.name = request.name();
        authorRepository.persist(author);
        return toResponse(author);
    }

    @Override
    @Transactional
    public AuthorResponse update(UUID id, AuthorRequest request) {
        Author author = authorRepository.findByIdOrThrow(id);
        if (request.name() == null || request.name().isBlank()) {
            throw new ValidationException("Author name must not be blank");
        }
        author.name = request.name();
        authorRepository.persist(author);
        return toResponse(author);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Author author = authorRepository.findByIdOrThrow(id);
        if (authorRepository.hasBooks(id)) {
            throw new ConflictException("Author has associated books");
        }
        authorRepository.delete(author);
    }

    @Override
    public PageResponse<AuthorResponse> list(PageRequest page) {
        return authorRepository.findAll(page);
    }

    @Override
    public AuthorResponse getById(UUID id) {
        Author author = authorRepository.findByIdOrThrow(id);
        return toResponse(author);
    }

    private AuthorResponse toResponse(Author author) {
        return new AuthorResponse(author.id, author.name, author.createdAt, author.updatedAt);
    }
}
