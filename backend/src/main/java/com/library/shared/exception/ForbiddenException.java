package com.library.shared.exception;

/** Thrown when an authenticated user lacks permission for the requested operation. Maps to HTTP 403. */
public class ForbiddenException extends AppException {

    public ForbiddenException(String message) {
        super(403, message);
    }

    public static ForbiddenException accessDenied() {
        return new ForbiddenException("Access denied");
    }
}
