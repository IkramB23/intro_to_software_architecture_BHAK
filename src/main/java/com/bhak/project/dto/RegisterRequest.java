package com.bhak.project.dto;

import com.bhak.project.entity.RoleType;
import lombok.Data;

/**
 * DTO representing a registration or account creation request.
 *
 * <h2>Purpose</h2>
 * Carries the registration data entered by the user
 * ({@code POST /api/auth/register}) or by an admin ({@code POST /api/admin/users})
 * to the corresponding controllers.
 *
 * <h2>Fields</h2>
 * <ul>
 *   <li>{@code username} – unique username.</li>
 *   <li>{@code email} – unique email address.</li>
 *   <li>{@code phoneNumber} – phone number (optional, but unique if provided).</li>
 *   <li>{@code password} – cleartext password (will be hashed with BCrypt).</li>
 *   <li>{@code roleType} – requested role ({@code ADMIN}, {@code MODERATOR}, {@code USER}).
 *       If not specified, the {@code USER} level is applied by default.</li>
 * </ul>
 *
 * <h2>Annotations</h2>
 * Lombok {@code @Data}.
 */
@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String phoneNumber;
    private String password;
    private RoleType roleType;
}
