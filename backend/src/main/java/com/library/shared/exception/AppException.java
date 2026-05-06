package com.library.shared.exception;

/**
 * Base exception for all application-level errors.
 * Subclasses map to specific HTTP status codes via {@link GlobalExceptionMapper}.
 */
public abstract class AppException extends RuntimeException {

    private final int status;

    protected AppException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
