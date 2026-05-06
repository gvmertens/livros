package com.library.shared.exception;

/** Thrown when a requested resource does not exist. Maps to HTTP 404. */
public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(404, message);
    }

    public static NotFoundException of(String resourceName, Object id) {
        return new NotFoundException(resourceName + " not found: " + id);
    }
}
