package com.library.shared.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * JAX-RS filter that assigns a correlation ID to every request.
 *
 * <p>On request: reads {@code X-Correlation-ID} header; generates a UUID if absent.
 * Stores the ID in MDC so all log statements for the request include it automatically.
 *
 * <p>On response: echoes the correlation ID back in the {@code X-Correlation-ID} header.
 */
@Provider
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String correlationId = requestContext.getHeaderString(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        // Store on request context so the response filter can read it
        requestContext.setProperty(MDC_KEY, correlationId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        Object correlationId = requestContext.getProperty(MDC_KEY);
        if (correlationId != null) {
            responseContext.getHeaders().putSingle(CORRELATION_ID_HEADER, correlationId.toString());
        }
        MDC.remove(MDC_KEY);
    }
}
