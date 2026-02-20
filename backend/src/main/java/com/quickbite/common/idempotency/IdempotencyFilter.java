package com.quickbite.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Filter that intercepts POST requests with an {@code Idempotency-Key} header
 * on protected endpoints, returning cached responses for duplicate keys.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    /**
     * Endpoints that support idempotency. Only POST to these paths is intercepted.
     */
    private static final Set<String> PROTECTED_ENDPOINTS = Set.of(
            "/api/orders",
            "/api/payments/intent"
    );

    private static final String HEADER_NAME = "Idempotency-Key";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only filter POST requests to protected endpoints
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        return PROTECTED_ENDPOINTS.stream().noneMatch(path::equals);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String idempotencyKey = request.getHeader(HEADER_NAME);

        // No header → pass through normally
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Resolve user ID from security context
        UUID userId = resolveUserId();
        if (userId == null) {
            // Unauthenticated — pass through, let security handle it
            filterChain.doFilter(request, response);
            return;
        }

        String endpoint = request.getRequestURI();

        // Check for existing idempotency record
        Optional<IdempotencyKey> existing = idempotencyService.findExisting(idempotencyKey, userId, endpoint);
        if (existing.isPresent() && Boolean.TRUE.equals(existing.get().getUsed())) {
            IdempotencyKey record = existing.get();
            log.info("Idempotency hit: key={} user={} endpoint={}", idempotencyKey, userId, endpoint);

            // Return cached response
            response.setStatus(record.getResponseStatus());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.addHeader("X-Idempotency-Replayed", "true");
            if (record.getResponseBody() != null) {
                response.getWriter().write(record.getResponseBody());
            }
            return;
        }

        // Wrap response to capture output
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        // Execute request normally
        filterChain.doFilter(request, wrappedResponse);

        // Store the response for future duplicates (only for successful requests)
        int status = wrappedResponse.getStatus();
        if (status >= 200 && status < 300) {
            byte[] body = wrappedResponse.getContentAsByteArray();
            String responseBody = new String(body, StandardCharsets.UTF_8);
            String requestHash = hashRequest(request);

            try {
                idempotencyService.store(
                        idempotencyKey, userId, endpoint,
                        requestHash, status, responseBody
                );
                log.debug("Idempotency key stored: key={} endpoint={}", idempotencyKey, endpoint);
            } catch (Exception e) {
                log.warn("Failed to store idempotency key (duplicate?): key={} - {}", idempotencyKey, e.getMessage());
            }
        }

        // Copy body to actual response
        wrappedResponse.copyBodyToResponse();
    }

    private UUID resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String hashRequest(HttpServletRequest request) {
        try {
            String content = request.getRequestURI() + "|" + request.getContentType();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
