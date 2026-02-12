package com.bhak.project.exception;

// exception quand la requete contient des donnees invalides (400)
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
