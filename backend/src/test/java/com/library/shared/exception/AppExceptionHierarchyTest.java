package com.library.shared.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link AppException} hierarchy.
 * Verifies that each subclass carries the correct HTTP status code and message.
 */
class AppExceptionHierarchyTest {

    @Test
    void notFoundException_hasStatus404() {
        NotFoundException ex = new NotFoundException("Author not found");
        assertEquals(404, ex.getStatus());
        assertEquals("Author not found", ex.getMessage());
    }

    @Test
    void notFoundException_of_formatsMessage() {
        UUID id = UUID.randomUUID();
        NotFoundException ex = NotFoundException.of("Book", id);
        assertEquals(404, ex.getStatus());
        assertTrue(ex.getMessage().contains("Book"));
        assertTrue(ex.getMessage().contains(id.toString()));
    }

    @Test
    void conflictException_hasStatus409() {
        ConflictException ex = new ConflictException("Duplicate ISBN");
        assertEquals(409, ex.getStatus());
        assertEquals("Duplicate ISBN", ex.getMessage());
    }

    @Test
    void validationException_hasStatus400() {
        ValidationException ex = new ValidationException("Name must not be blank");
        assertEquals(400, ex.getStatus());
        assertEquals("Name must not be blank", ex.getMessage());
    }

    @Test
    void forbiddenException_hasStatus403() {
        ForbiddenException ex = new ForbiddenException("Access denied");
        assertEquals(403, ex.getStatus());
    }

    @Test
    void forbiddenException_accessDenied_factory() {
        ForbiddenException ex = ForbiddenException.accessDenied();
        assertEquals(403, ex.getStatus());
        assertEquals("Access denied", ex.getMessage());
    }

    @Test
    void unprocessableEntityException_hasStatus422() {
        UnprocessableEntityException ex = new UnprocessableEntityException("Author not found: " + UUID.randomUUID());
        assertEquals(422, ex.getStatus());
    }

    @Test
    void unauthorizedException_hasStatus401() {
        UnauthorizedException ex = new UnauthorizedException("Invalid credentials");
        assertEquals(401, ex.getStatus());
    }

    @Test
    void unauthorizedException_invalidCredentials_factory() {
        UnauthorizedException ex = UnauthorizedException.invalidCredentials();
        assertEquals(401, ex.getStatus());
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void allExceptions_extendAppException() {
        assertTrue(new NotFoundException("x") instanceof AppException);
        assertTrue(new ConflictException("x") instanceof AppException);
        assertTrue(new ValidationException("x") instanceof AppException);
        assertTrue(new ForbiddenException("x") instanceof AppException);
        assertTrue(new UnprocessableEntityException("x") instanceof AppException);
        assertTrue(new UnauthorizedException("x") instanceof AppException);
    }

    @Test
    void allExceptions_extendRuntimeException() {
        assertTrue(new NotFoundException("x") instanceof RuntimeException);
        assertTrue(new ConflictException("x") instanceof RuntimeException);
        assertTrue(new ValidationException("x") instanceof RuntimeException);
    }
}
