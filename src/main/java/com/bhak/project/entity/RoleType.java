package com.bhak.project.entity;

// les differents roles possibles dans l'application
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
