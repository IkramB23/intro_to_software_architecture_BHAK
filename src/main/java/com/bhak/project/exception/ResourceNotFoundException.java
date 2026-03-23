package com.bhak.project.exception;

/**
 * Exception thrown when a requested resource does not exist in the database.
 *
 * <h2>Purpose</h2>
 * Raised in services ({@code UserService}, {@code VerificationService})
 * when a provided identifier matches no record.
 * Caught by {@link GlobalExceptionHandler} to return an
 * HTTP <b>404 Not Found</b> response with a descriptive message.
 *
 * <h2>Constructors</h2>
 * <ul>
 *   <li>{@code ResourceNotFoundException(String message)} – free-form message.</li>
 *   <li>{@code ResourceNotFoundException(String resourceName, Long id)} –
 *       automatically builds a message like
 *       {@code "Account not found with identifier: 42"}.</li>
 * </ul>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " introuvable avec l'identifiant : " + id);
    }
}
