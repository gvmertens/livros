package com.library.shared.exception;

/** Temporary failure while communicating with an external dependency. */
public class ExternalServiceException extends AppException {

    public ExternalServiceException(String message) {
        super(503, message);
    }
}
