package com.specquiz.exception;

import java.util.List;

import lombok.Getter;

/**
 * Carries the full set of aggregated structural violations (AC-021), so callers can
 * report every problem at once rather than failing on the first.
 */
@Getter
public class CertificationValidationException extends RuntimeException {

    private final transient List<Violation> violations;

    public CertificationValidationException(List<Violation> violations) {
        super("certificate validation failed with " + violations.size() + " violation(s)");
        this.violations = violations;
    }

    /** A single validation problem: which field/rule and a human-readable message. */
    public record Violation(String field, String message) {
    }
}
