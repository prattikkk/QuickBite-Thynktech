package com.quickbite.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed per-user / per-IP rate limiting filter.
 * Phase 4 — Performance: NFR-1 API rate limit: 100 req/min/user.
 *
 * Uses a sliding-window counter: key = "rl:{identity}", value = request count.
 * Window resets every 60 seconds via Redis TTL.
 */
@Slf4j
@Component
public class RateLimitFilter implements Filter {

    private final StringRedisTemplate redisTemplate;
    private final long maxRequestsPerMinute;
    private final long authMaxRequestsPerMinute;

    public RateLimitFilter(
            @org.springframework.lang.Nullable StringRedisTemplate redisTemplate,
            @Value("${rate-limit.requests-per-minute:100}") long maxRequestsPerMinute,
            @Value("${rate-limit.auth-requests-per-minute:20}") long authMaxRequestsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.authMaxRequestsPerMinute = authMaxRequestsPerMinute;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        // Skip non-API and health/actuator requests
        String path = httpReq.getRequestURI();
        if (!path.startsWith("/api") && !path.startsWith("/ws")) {
            chain.doFilter(request, response);
            return;
        }

        String identity = resolveIdentity(httpReq);
        long limit = path.startsWith("/api/auth") ? authMaxRequestsPerMinute : maxRequestsPerMinute;
        String key = "rl:" + identity;

        if (redisTemplate == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(60));
            }

            // Set rate-limit response headers
            httpRes.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            long remaining = Math.max(0, limit - (count != null ? count : 0));
            httpRes.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            httpRes.setHeader("X-RateLimit-Reset", String.valueOf(ttl != null ? ttl : 60));

            if (count != null && count > limit) {
                log.warn("Rate limit exceeded for identity={}, count={}, limit={}", identity, count, limit);
                httpRes.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpRes.setContentType("application/json");
                httpRes.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded. Try again in " + (ttl != null ? ttl : 60) + " seconds.\"}");
                return;
            }
        } catch (Exception e) {
            // If Redis is down, allow the request through (graceful degradation)
            log.warn("Rate limiter unavailable, allowing request: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private String resolveIdentity(HttpServletRequest request) {
        // Prefer authenticated user ID from JWT (set by JwtAuthenticationFilter)
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            return "user:" + auth.getName();
        }
        // Fallback to IP
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return "ip:" + ip.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
