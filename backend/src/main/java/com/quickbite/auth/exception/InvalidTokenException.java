package com.quickbite.auth.exception;

/**
 * Exception thrown when JWT token is invalid or expired.
 */
public class InvalidTokenException extends AuthException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
