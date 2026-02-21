package com.quickbite.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that establishes a correlation/request-id for every
 * incoming HTTP request. The id is:
 * <ol>
 *   <li>Read from the {@code X-Request-Id} header if the caller sent one.</li>
 *   <li>Otherwise, generated as a new UUID.</li>
 * </ol>
 * It is pushed into SLF4J MDC as {@code requestId} so every log line
 * automatically includes it. The id is also returned in the response
 * header {@code X-Request-Id}.
 * <p>
 * After the security chain runs, the filter also pushes {@code userId}
 * into MDC if an authenticated principal is available.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);

            // After the chain, try to capture userId from security context
            pushUserIdToMdc();
        } finally {
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    private void pushUserIdToMdc() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null
                    && !"anonymousUser".equals(auth.getPrincipal().toString())) {
                MDC.put(MDC_USER_ID, auth.getName());
            }
        } catch (Exception ignored) {
            // Best-effort — never break the request
        }
    }
}
