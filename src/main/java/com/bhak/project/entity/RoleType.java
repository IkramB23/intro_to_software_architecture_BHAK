package com.bhak.project.entity;

/**
 * Enumeration of application roles used for access control (RBAC).
 *
 * <h2>Purpose</h2>
 * Defines three authorisation levels: {@code ADMIN}, {@code MODERATOR}, and {@code USER}.
 * Each value carries a human-readable label ({@code getLabel()}) used during
 * database initialisation by {@link com.bhak.project.configuration.DataInitializer}.
 *
 * <h2>How it works</h2>
 * The enum is persisted in the database as a string ({@code @Enumerated(EnumType.STRING)})
 * in the {@link Role} entity. Spring Security builds the authority by prefixing
 * {@code "ROLE_"} to the enum name (e.g. {@code ROLE_ADMIN}), which allows the use of
 * {@code @PreAuthorize("hasRole('ADMIN')")} or {@code .hasRole("ADMIN")} in
 * the security configuration.
 */
public enum RoleType {

    ADMIN("Administrateur"),
    MODERATOR("Moderateur"),
    USER("Utilisateur");

    private final String label;

    RoleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
