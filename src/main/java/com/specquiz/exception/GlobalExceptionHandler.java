package com.specquiz.exception;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.specquiz.exception.CertificationValidationException.Violation;

/**
 * Maps exceptions to the standard error body (DES-011):
 * {@code { timestamp, status, error, message, path, violations[] }}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Standard structured error response. */
    public record ErrorResponse(
            String timestamp,
            int status,
            String error,
            String message,
            String path,
            List<Violation> violations) {
    }

    @ExceptionHandler(CertificationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(CertificationNotFoundException ex,
            HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CertificationValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(CertificationValidationException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex.getViolations());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<Violation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new Violation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "request validation failed", request, violations);
    }

    /** Query-param / path-var constraint violations (F-004). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        List<Violation> violations = ex.getConstraintViolations().stream()
                .map(cv -> new Violation(cv.getPropertyPath().toString(), cv.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "request validation failed", request, violations);
    }

    /** Malformed / unreadable request body (F-004, design §10). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "malformed request body", request, List.of());
    }

    /** Defense-in-depth for the unique-title constraint if a pre-check is bypassed (F-003). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "data integrity violation (e.g. duplicate title)",
                request, List.of(new Violation("title", "a certificate with this title already exists")));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
            HttpServletRequest request, List<Violation> violations) {
        ErrorResponse body = new ErrorResponse(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                violations);
        return ResponseEntity.status(status).body(body);
    }
}
