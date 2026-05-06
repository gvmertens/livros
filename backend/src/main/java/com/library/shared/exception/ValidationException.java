package com.library.shared.exception;

/** Thrown when a business rule validation fails. Maps to HTTP 400. */
public class ValidationException extends AppException {

    public ValidationException(String message) {
        super(400, message);
    }
}
