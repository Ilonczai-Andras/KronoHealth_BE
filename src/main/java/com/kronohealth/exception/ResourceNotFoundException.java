package com.kronohealth.exception;

/**
 * Dobható, ha egy kért erőforrás nem található (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

