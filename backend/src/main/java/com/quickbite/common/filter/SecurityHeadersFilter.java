package com.quickbite.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Adds OWASP-recommended security response headers.
 * Phase 4 — Security: NFR-4 acceptance criteria.
 *
 * Headers:
 * - X-Content-Type-Options: nosniff
 * - X-Frame-Options: DENY
 * - X-XSS-Protection: 0 (modern browsers rely on CSP, old header can cause issues)
 * - Strict-Transport-Security (HSTS): max-age 1 year, includeSubDomains
 * - Referrer-Policy: strict-origin-when-cross-origin
 * - Permissions-Policy: geolocation=(), camera=(), microphone=()
 * - Cache-Control: no-store for API responses
 * - Content-Security-Policy: default-src 'self'
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse httpRes) {
            httpRes.setHeader("X-Content-Type-Options", "nosniff");
            httpRes.setHeader("X-Frame-Options", "DENY");
            httpRes.setHeader("X-XSS-Protection", "0");
            httpRes.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            httpRes.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            httpRes.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=()");
            httpRes.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'");

            // Prevent caching of API responses
            String uri = ((jakarta.servlet.http.HttpServletRequest) request).getRequestURI();
            if (uri.startsWith("/api")) {
                httpRes.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                httpRes.setHeader("Pragma", "no-cache");
            }
        }

        chain.doFilter(request, response);
    }
}
