package com.library.shared.exception;

import java.time.Instant;

/**
 * Consistent error response body returned for all 4xx and 5xx responses.
 *
 * @param status    HTTP status code
 * @param message   human-readable error description
 * @param timestamp when the error occurred
 */
public record ErrorResponse(int status, String message, Instant timestamp) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Instant.now());
    }
}
