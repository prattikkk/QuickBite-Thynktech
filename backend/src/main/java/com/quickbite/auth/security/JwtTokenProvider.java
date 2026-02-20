package com.quickbite.auth.security;

import com.quickbite.auth.exception.InvalidTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Token Provider for generating and validating JWT tokens.
 * Uses HS256 signing algorithm with configurable secret key.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Generate access token with user claims.
     *
     * @param userId user UUID
     * @param email user email
     * @param role user role name
     * @return JWT access token
     */
    public String generateAccessToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenExpirationMs, ChronoUnit.MILLIS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("email", email);
        claims.put("role", role);
        claims.put("type", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Generate refresh token.
     *
     * @param userId user UUID
     * @param email user email
     * @return JWT refresh token
     */
    public String generateRefreshToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiration = now.plus(refreshTokenExpirationMs, ChronoUnit.MILLIS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("email", email);
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validate JWT token.
     *
     * @param token JWT token
     * @return true if valid
     * @throws InvalidTokenException if token is invalid or expired
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw new InvalidTokenException("Token has expired", e);
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            throw new InvalidTokenException("Token is unsupported", e);
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
            throw new InvalidTokenException("Token is malformed", e);
        } catch (SecurityException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            throw new InvalidTokenException("Invalid token signature", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is invalid: {}", e.getMessage());
            throw new InvalidTokenException("Token is invalid", e);
        }
    }

    /**
     * Extract user ID from token.
     *
     * @param token JWT token
     * @return user UUID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        String userIdStr = claims.get("userId", String.class);
        return UUID.fromString(userIdStr);
    }

    /**
     * Extract email from token.
     *
     * @param token JWT token
     * @return user email
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    /**
     * Extract role from token.
     *
     * @param token JWT token
     * @return user role
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Check if token is expired.
     *
     * @param token JWT token
     * @return true if expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Get token type (access or refresh).
     *
     * @param token JWT token
     * @return token type
     */
    public String getTokenType(String token) {
        Claims claims = getClaims(token);
        return claims.get("type", String.class);
    }

    /**
     * Extract all claims from token.
     *
     * @param token JWT token
     * @return claims
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get access token expiration in seconds.
     *
     * @return expiration time in seconds
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }
}
