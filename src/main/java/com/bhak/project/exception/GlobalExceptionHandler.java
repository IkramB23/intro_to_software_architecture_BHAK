package com.bhak.project.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised exception handler for the entire REST API.
 *
 * <h2>Purpose</h2>
 * Intercepts exceptions thrown by controllers and services
 * and converts them into standardised JSON responses with the appropriate HTTP status code.
 * This avoids repeating error handling in every controller
 * (DRY principle) and ensures a uniform response format.
 *
 * <h2>How it works</h2>
 * <ul>
 *   <li>{@code ResourceNotFoundException} → HTTP 404 (resource not found).</li>
 *   <li>{@code DuplicateResourceException} → HTTP 409 (uniqueness conflict).</li>
 *   <li>{@code InvalidRequestException} → HTTP 400 (invalid data).</li>
 *   <li>Any other {@code Exception} → HTTP 500 (unexpected internal error).</li>
 * </ul>
 * Each response contains: {@code timestamp}, {@code status}, {@code error}, and {@code message}.
 *
 * <h2>Technologies</h2>
 * {@code @RestControllerAdvice} (combines {@code @ControllerAdvice} + {@code @ResponseBody}),
 * {@code @ExceptionHandler}, Lombok ({@code @Slf4j}).
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ressource introuvable -> 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Ressource introuvable : {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // doublon detecte (pseudo, email, tel) -> 409
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateResourceException ex) {
        log.warn("Conflit de données : {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // requete invalide -> 400
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(InvalidRequestException ex) {
        log.warn("Requête invalide : {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // toute autre erreur -> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Erreur interne inattendue", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue");
    }

    // construit la reponse json d'erreur
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
