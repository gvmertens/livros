package com.library.shared.exception;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ErrorResponse}.
 */
class ErrorResponseTest {

    @Test
    void of_setsStatusAndMessage() {
        ErrorResponse resp = ErrorResponse.of(404, "Not found");
        assertEquals(404, resp.status());
        assertEquals("Not found", resp.message());
        assertNotNull(resp.timestamp());
    }

    @Test
    void of_timestampIsRecent() {
        Instant before = Instant.now();
        ErrorResponse resp = ErrorResponse.of(500, "Error");
        Instant after = Instant.now();
        assertFalse(resp.timestamp().isBefore(before));
        assertFalse(resp.timestamp().isAfter(after));
    }

    @Test
    void constructor_setsAllFields() {
        Instant ts = Instant.parse("2024-01-01T00:00:00Z");
        ErrorResponse resp = new ErrorResponse(400, "Bad request", ts);
        assertEquals(400, resp.status());
        assertEquals("Bad request", resp.message());
        assertEquals(ts, resp.timestamp());
    }
}
