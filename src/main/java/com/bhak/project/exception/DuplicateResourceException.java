package com.bhak.project.exception;

/**
 * Exception thrown when a field with a uniqueness constraint is already in use.
 *
 * <h2>Purpose</h2>
 * Raised in services when a username, email, or phone number
 * already exists in the database. Caught by {@link GlobalExceptionHandler} to
 * return an HTTP <b>409 Conflict</b> response.
 *
 * <h2>Constructor</h2>
 * {@code DuplicateResourceException(String field, String value)} –
 * produces a message like {@code "The value 'bob' is already assigned to field: username"}.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String field, String value) {
        super("La valeur '" + value + "' est déjà attribuée au champ : " + field);
    }
}
