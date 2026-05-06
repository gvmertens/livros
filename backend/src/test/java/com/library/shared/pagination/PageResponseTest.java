package com.library.shared.pagination;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PageResponse}.
 */
class PageResponseTest {

    @Test
    void of_exactDivision_computesTotalPagesCorrectly() {
        PageRequest req = PageRequest.of(0, 10);
        PageResponse<String> resp = PageResponse.of(List.of("a", "b"), 20L, req);
        assertEquals(2, resp.totalPages());
        assertEquals(20L, resp.totalElements());
        assertEquals(0, resp.page());
        assertEquals(10, resp.size());
    }

    @Test
    void of_remainder_roundsUpTotalPages() {
        PageRequest req = PageRequest.of(0, 10);
        PageResponse<String> resp = PageResponse.of(List.of(), 21L, req);
        assertEquals(3, resp.totalPages());
    }

    @Test
    void of_zeroElements_returnsZeroPages() {
        PageRequest req = PageRequest.of(0, 20);
        PageResponse<String> resp = PageResponse.of(List.of(), 0L, req);
        assertEquals(0, resp.totalPages());
        assertEquals(0L, resp.totalElements());
    }

    @Test
    void of_preservesContent() {
        PageRequest req = PageRequest.of(1, 5);
        List<String> content = List.of("x", "y", "z");
        PageResponse<String> resp = PageResponse.of(content, 13L, req);
        assertEquals(content, resp.content());
        assertEquals(1, resp.page());
        assertEquals(5, resp.size());
    }

    @Test
    void of_singleElement_onePage() {
        PageRequest req = PageRequest.of(0, 20);
        PageResponse<Integer> resp = PageResponse.of(List.of(42), 1L, req);
        assertEquals(1, resp.totalPages());
    }
}
