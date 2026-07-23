package com.library.catalog.infrastructure.googlebooks;

import com.library.catalog.application.dto.CatalogBook;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@ApplicationScoped
public class GoogleBooksMapper {

    public CatalogBook toCatalogBook(GoogleBooksResponseDTO.Volume volume) {
        if (volume == null || volume.volumeInfo() == null) {
            return null;
        }

        var info = volume.volumeInfo();
        List<String> authors = cleanList(info.authors());
        List<String> categories = cleanList(info.categories());
        String isbn10 = identifier(info.industryIdentifiers(), "ISBN_10");
        String isbn13 = identifier(info.industryIdentifiers(), "ISBN_13");
        String thumbnail = image(info.imageLinks(), false);
        String largeCover = image(info.imageLinks(), true);
        String title = clean(info.title());
        String language = clean(info.language());

        boolean hasIsbn = isbn10 != null || isbn13 != null;
        boolean complete = title != null && !authors.isEmpty() && hasIsbn
                && language != null && thumbnail != null;

        return new CatalogBook(
                clean(volume.id()),
                title,
                clean(info.subtitle()),
                authors,
                clean(info.publisher()),
                clean(info.publishedDate()),
                isbn10,
                isbn13,
                language == null ? null : language.toLowerCase(Locale.ROOT),
                categories,
                sanitizeDescription(info.description()),
                thumbnail,
                largeCover,
                secureUrl(info.infoLink()),
                info.pageCount(),
                clean(info.printType()),
                hasIsbn,
                complete);
    }

    private String identifier(List<GoogleBooksResponseDTO.IndustryIdentifier> identifiers, String type) {
        if (identifiers == null) {
            return null;
        }
        return identifiers.stream()
                .filter(Objects::nonNull)
                .filter(identifier -> type.equalsIgnoreCase(identifier.type()))
                .map(GoogleBooksResponseDTO.IndustryIdentifier::identifier)
                .map(this::normalizeIsbn)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String normalizeIsbn(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        cleaned = cleaned.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        return cleaned.length() == 10 || cleaned.length() == 13 ? cleaned : null;
    }

    private String image(GoogleBooksResponseDTO.ImageLinks links, boolean preferLarge) {
        if (links == null) {
            return null;
        }
        String selected = preferLarge
                ? first(links.extraLarge(), links.large(), links.medium(), links.thumbnail(), links.smallThumbnail())
                : first(links.thumbnail(), links.smallThumbnail(), links.medium(), links.large(), links.extraLarge());
        return secureUrl(selected);
    }

    private String first(String... values) {
        for (String value : values) {
            if (clean(value) != null) {
                return value;
            }
        }
        return null;
    }

    private String secureUrl(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        return cleaned.startsWith("http://") ? "https://" + cleaned.substring(7) : cleaned;
    }

    private String sanitizeDescription(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        return cleaned.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }

    private List<String> cleanList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(this::clean).filter(Objects::nonNull).distinct().toList();
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
