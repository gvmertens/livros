package com.library.shared.pagination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PageRequest}.
 */
class PageRequestTest {

    @Test
    void defaults_returnsPage0Size20() {
        PageRequest req = PageRequest.defaults();
        assertEquals(0, req.page());
        assertEquals(20, req.size());
    }

    @Test
    void of_validParams_createsPageRequest() {
        PageRequest req = PageRequest.of(2, 50);
        assertEquals(2, req.page());
        assertEquals(50, req.size());
    }

    @Test
    void offset_calculatesCorrectly() {
        PageRequest req = PageRequest.of(3, 10);
        assertEquals(30, req.offset());
    }

    @Test
    void offset_page0_isZero() {
        PageRequest req = PageRequest.of(0, 20);
        assertEquals(0, req.offset());
    }

    @Test
    void negativePage_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PageRequest.of(-1, 20));
        assertTrue(ex.getMessage().contains("negative"));
    }

    @Test
    void sizeZero_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> PageRequest.of(0, 0));
    }

    @Test
    void sizeExceedsMax_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PageRequest.of(0, 101));
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    void sizeAtMax_isAllowed() {
        PageRequest req = PageRequest.of(0, 100);
        assertEquals(100, req.size());
    }

    @Test
    void sizeAtMin_isAllowed() {
        PageRequest req = PageRequest.of(0, 1);
        assertEquals(1, req.size());
    }
}
