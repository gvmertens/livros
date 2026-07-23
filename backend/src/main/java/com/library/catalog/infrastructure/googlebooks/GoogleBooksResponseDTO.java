package com.library.catalog.infrastructure.googlebooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleBooksResponseDTO(Integer totalItems, List<Volume> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Volume(String id, VolumeInfo volumeInfo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VolumeInfo(
            String title,
            String subtitle,
            List<String> authors,
            String publisher,
            String publishedDate,
            String description,
            List<IndustryIdentifier> industryIdentifiers,
            Integer pageCount,
            List<String> categories,
            ImageLinks imageLinks,
            String language,
            String infoLink,
            String printType) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IndustryIdentifier(String type, String identifier) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImageLinks(
            String smallThumbnail,
            String thumbnail,
            String small,
            String medium,
            String large,
            String extraLarge) {}
}
