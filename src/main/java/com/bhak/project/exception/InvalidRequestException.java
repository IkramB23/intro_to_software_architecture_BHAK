package com.bhak.project.exception;

/**
 * Exception thrown when the request contains invalid or inconsistent data.
 *
 * <h2>Purpose</h2>
 * Used to signal missing required fields, an unknown role,
 * an expired or invalid verification token, etc.
 * Caught by {@link GlobalExceptionHandler} to return
 * an HTTP <b>400 Bad Request</b> response.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
