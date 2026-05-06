package com.library.shared.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.util.stream.Collectors;

/**
 * Global JAX-RS exception mapper that converts all exceptions into a consistent
 * {@link ErrorResponse} JSON body.
 *
 * <p>Mapping rules:
 * <ul>
 *   <li>{@link AppException} subclasses → their declared HTTP status</li>
 *   <li>{@link ConstraintViolationException} → 400 with field-level messages</li>
 *   <li>Everything else → 500 (stack trace logged, not exposed to client)</li>
 * </ul>
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable ex) {
        if (ex instanceof AppException appEx) {
            return errorResponse(appEx.getStatus(), appEx.getMessage());
        }

        if (ex instanceof ConstraintViolationException cve) {
            String message = cve.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            return errorResponse(400, message);
        }

        LOG.errorf(ex, "Unhandled exception [correlationId=%s]", MDC.get("correlationId"));
        return errorResponse(500, "Internal server error");
    }

    private Response errorResponse(int status, String message) {
        return Response.status(status)
                .entity(ErrorResponse.of(status, message))
                .build();
    }
}
