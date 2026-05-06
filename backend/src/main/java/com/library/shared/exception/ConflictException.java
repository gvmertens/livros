package com.library.shared.exception;

/** Thrown when a request conflicts with existing state (e.g. duplicate email, ISBN). Maps to HTTP 409. */
public class ConflictException extends AppException {

    public ConflictException(String message) {
        super(409, message);
    }
}
