package com.bhak.project.exception;

// exception quand un champ unique est deja utilise (409)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String field, String value) {
        super("La valeur '" + value + "' est déjà attribuée au champ : " + field);
    }
}
