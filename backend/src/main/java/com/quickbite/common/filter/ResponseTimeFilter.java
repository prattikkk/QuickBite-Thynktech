package com.quickbite.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Adds X-Response-Time header to every HTTP response.
 * Phase 4 — Performance: NFR-1 acceptance criteria.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ResponseTimeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            if (response instanceof HttpServletResponse httpRes) {
                httpRes.setHeader("X-Response-Time", durationMs + "ms");
            }
        }
    }
}
