package com.library.catalog.application;

import com.library.catalog.application.dto.CatalogBook;
import com.library.catalog.application.dto.CatalogSearchResult;
import com.library.catalog.infrastructure.googlebooks.GoogleBooksClient;
import com.library.catalog.infrastructure.googlebooks.GoogleBooksMapper;
import com.library.catalog.infrastructure.googlebooks.GoogleBooksProperties;
import com.library.shared.exception.NotFoundException;
import com.library.shared.exception.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;

@ApplicationScoped
public class CatalogSearchService {

    @Inject
    GoogleBooksClient client;

    @Inject
    GoogleBooksMapper mapper;

    @Inject
    GoogleBooksProperties properties;

    public CatalogSearchResult search(String query, String title, String author, String isbn,
            int startIndex, Integer requestedLimit) {
        String googleQuery = buildQuery(query, title, author, isbn);
        if (startIndex < 0) {
            throw new ValidationException("startIndex must be greater than or equal to zero");
        }
        int limit = requestedLimit == null ? properties.maxResults() : requestedLimit;
        if (limit < 1 || limit > Math.min(properties.maxResults(), 40)) {
            throw new ValidationException("maxResults must be between 1 and "
                    + Math.min(properties.maxResults(), 40));
        }

        var response = client.search(googleQuery, startIndex, limit);
        var deduplicated = new LinkedHashMap<String, CatalogBook>();
        if (response.items() != null) {
            response.items().stream()
                    .map(mapper::toCatalogBook)
                    .filter(book -> book != null && book.googleBooksId() != null && book.title() != null)
                    .sorted(Comparator
                            .comparing((CatalogBook book) -> !isPortuguese(book.language()))
                            .thenComparing(book -> !book.metadataComplete()))
                    .forEach(book -> deduplicated.putIfAbsent(deduplicationKey(book), book));
        }

        var items = deduplicated.values().stream().limit(limit).toList();
        return new CatalogSearchResult(items, items.size());
    }

    public CatalogBook findByGoogleBooksId(String googleBooksId) {
        validateText("googleBooksId", googleBooksId, 255);
        CatalogBook book = mapper.toCatalogBook(client.findById(googleBooksId.trim()));
        if (book == null || book.googleBooksId() == null || book.title() == null) {
            throw NotFoundException.of("Google Books volume", googleBooksId);
        }
        return book;
    }

    private String buildQuery(String query, String title, String author, String isbn) {
        boolean empty = isBlank(query) && isBlank(title) && isBlank(author) && isBlank(isbn);
        if (empty) {
            throw new ValidationException("At least one search criterion must be provided");
        }
        var parts = new java.util.ArrayList<String>();
        if (!isBlank(query)) {
            validateText("query", query, 200);
            parts.add(query.trim());
        }
        if (!isBlank(title)) {
            validateText("title", title, 200);
            parts.add("intitle:" + title.trim());
        }
        if (!isBlank(author)) {
            validateText("author", author, 200);
            parts.add("inauthor:" + author.trim());
        }
        if (!isBlank(isbn)) {
            validateText("isbn", isbn, 32);
            String normalized = isbn.replace("-", "").replace(" ", "");
            if (!normalized.matches("[0-9Xx]{10,13}")) {
                throw new ValidationException("ISBN must contain 10 to 13 digits");
            }
            parts.add("isbn:" + normalized);
        }
        return String.join(" ", parts);
    }

    private String deduplicationKey(CatalogBook book) {
        if (book.isbn13() != null) return "isbn13:" + book.isbn13();
        if (book.isbn10() != null) return "isbn10:" + book.isbn10();
        if (book.googleBooksId() != null) return "google:" + book.googleBooksId();
        String author = book.authors().isEmpty() ? "" : normalize(book.authors().get(0));
        return "metadata:" + normalize(book.title()) + ":" + author + ":" + normalize(book.publisher());
    }

    private boolean isPortuguese(String language) {
        return language != null && language.toLowerCase(Locale.ROOT).startsWith("pt");
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private void validateText(String field, String value, int maxLength) {
        if (isBlank(value)) {
            throw new ValidationException(field + " must not be blank");
        }
        if (value.trim().length() > maxLength) {
            throw new ValidationException(field + " must not exceed " + maxLength + " characters");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
