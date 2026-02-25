package com.quickbite.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Filter that validates Content-Type on requests with a body (POST, PUT, PATCH).
 * Rejects requests with unsupported or missing Content-Type to prevent content-type confusion attacks.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ContentTypeValidationFilter extends OncePerRequestFilter {

    private static final Set<String> METHODS_WITH_BODY = Set.of("POST", "PUT", "PATCH");
    
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.MULTIPART_FORM_DATA_VALUE,
        MediaType.APPLICATION_FORM_URLENCODED_VALUE
    );

    /** Paths that skip content-type validation (webhooks, file uploads, etc.) */
    private static final Set<String> EXCLUDED_PATHS = Set.of(
        "/api/payments/webhook",
        "/actuator"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        
        String method = request.getMethod().toUpperCase();
        String path = request.getRequestURI();

        // Only validate content-type for requests with a body
        if (METHODS_WITH_BODY.contains(method) && !isExcluded(path)) {
            String contentType = request.getContentType();
            
            if (contentType == null || contentType.isBlank()) {
                log.warn("Rejected {} {} — missing Content-Type header", method, path);
                sendError(response, "Content-Type header is required for " + method + " requests");
                return;
            }

            // Extract the base media type (ignore charset and other parameters)
            String baseType = contentType.split(";")[0].trim().toLowerCase();
            
            if (!ALLOWED_CONTENT_TYPES.contains(baseType)) {
                log.warn("Rejected {} {} — unsupported Content-Type: {}", method, path, contentType);
                sendError(response, "Unsupported Content-Type: " + baseType + ". Allowed: application/json, multipart/form-data, application/x-www-form-urlencoded");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExcluded(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\",\"data\":null,\"errors\":null}");
    }
}
