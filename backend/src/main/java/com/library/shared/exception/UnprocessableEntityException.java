package com.library.shared.exception;

/** Thrown when a request references a resource that does not exist (e.g. unknown authorId). Maps to HTTP 422. */
public class UnprocessableEntityException extends AppException {

    public UnprocessableEntityException(String message) {
        super(422, message);
    }
}
