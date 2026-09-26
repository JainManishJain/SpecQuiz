package com.specquiz.exception;

/**
 * Thrown when a certificate, section, or question cannot be found by id (AC-014, AC-016).
 */
public class CertificationNotFoundException extends RuntimeException {

    public CertificationNotFoundException(String message) {
        super(message);
    }

    public static CertificationNotFoundException of(String entity, Long id) {
        return new CertificationNotFoundException(entity + " not found: " + id);
    }
}
