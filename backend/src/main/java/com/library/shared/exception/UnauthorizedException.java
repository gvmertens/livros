package com.library.shared.exception;

/** Thrown when credentials are missing or invalid. Maps to HTTP 401. */
public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        super(401, message);
    }

    public static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("Invalid email or password");
    }
}
