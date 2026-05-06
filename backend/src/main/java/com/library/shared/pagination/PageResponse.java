package com.library.shared.pagination;

import java.util.List;

/**
 * Paginated response wrapper returned by all list endpoints.
 *
 * @param <T>           the type of items in the page
 * @param content       items on the current page
 * @param totalElements total number of items across all pages
 * @param totalPages    total number of pages
 * @param page          current zero-based page index
 * @param size          page size used for this response
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {

    public static <T> PageResponse<T> of(List<T> content, long totalElements, PageRequest request) {
        int totalPages = request.size() == 0 ? 0
                : (int) Math.ceil((double) totalElements / request.size());
        return new PageResponse<>(content, totalElements, totalPages, request.page(), request.size());
    }
}
