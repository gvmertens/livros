package com.library.shared.pagination;

/**
 * Pagination parameters for list queries.
 *
 * @param page zero-based page index (default 0)
 * @param size number of items per page (default 20, max 100)
 */
public record PageRequest(int page, int size) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_SIZE);
        }
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
    }

    public static PageRequest defaults() {
        return new PageRequest(DEFAULT_PAGE, DEFAULT_SIZE);
    }

    /** Zero-based offset for SQL OFFSET clause. */
    public int offset() {
        return page * size;
    }
}
