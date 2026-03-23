package com.bhak.project.dto;

import lombok.Data;

/**
 * DTO (Data Transfer Object) representing a login request.
 *
 * <h2>Purpose</h2>
 * Carries the credentials entered by the user from the HTTP request body
 * {@code POST /api/auth/login} to the controller
 * {@link com.bhak.project.controller.AuthController}.
 *
 * <h2>Fields</h2>
 * <ul>
 *   <li>{@code username} – account username.</li>
 *   <li>{@code password} – cleartext password (will be verified against BCrypt hash).</li>
 * </ul>
 *
 * <h2>Annotations</h2>
 * Lombok {@code @Data} automatically generates getters, setters, {@code equals()},
 * {@code hashCode()}, and {@code toString()}.
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}
