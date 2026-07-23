package com.library.catalog.application.dto;

import java.util.List;

public record CatalogBook(
        String googleBooksId,
        String title,
        String subtitle,
        List<String> authors,
        String publisher,
        String publishedDate,
        String isbn10,
        String isbn13,
        String language,
        List<String> originalCategories,
        String description,
        String thumbnailUrl,
        String largeCoverUrl,
        String infoUrl,
        Integer pageCount,
        String printType,
        boolean hasIsbn,
        boolean metadataComplete) {}
