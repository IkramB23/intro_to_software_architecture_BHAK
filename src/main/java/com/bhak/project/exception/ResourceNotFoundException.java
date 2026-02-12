package com.bhak.project.exception;

// exception quand une ressource n'existe pas en base (404)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " introuvable avec l'identifiant : " + id);
    }
}
