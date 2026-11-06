package com.oreostudios.sarah.shared; // Shared error representation.

import jakarta.servlet.http.HttpServletRequest; // Access to the failing request path.
import org.springframework.http.HttpStatus; // HTTP status codes.
import org.springframework.http.ResponseEntity; // Typed HTTP response wrapper.
import org.springframework.validation.FieldError; // Per-field validation error.
import org.springframework.web.bind.MethodArgumentNotValidException; // Bean-validation failure on @Valid bodies.
import org.springframework.web.bind.annotation.ExceptionHandler; // Maps exceptions to handlers.
import org.springframework.web.bind.annotation.RestControllerAdvice; // Global controller advice for JSON APIs.

import java.util.List; // Detail list type.

/** Translates exceptions into a consistent {@link ApiError} response across all controllers. */
@RestControllerAdvice // Applies to every REST controller.
public class GlobalExceptionHandler {

    /** Maps {@link NotFoundException} to 404. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, List.of());
    }

    /** Maps {@link PolicyViolationException} to 409. */
    @ExceptionHandler(PolicyViolationException.class)
    public ResponseEntity<ApiError> handlePolicy(PolicyViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, List.of());
    }

    /** Maps {@link IllegalArgumentException} to 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, List.of());
    }

    /** Maps bean-validation failures to 400 with per-field details. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream() // Collect field errors.
                .map(GlobalExceptionHandler::formatFieldError) // Format each as "field: message".
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, details);
    }

    private static String formatFieldError(FieldError fe) {
        return fe.getField() + ": " + fe.getDefaultMessage(); // "field: message".
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req, List<String> details) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message, req.getRequestURI(), details);
        return ResponseEntity.status(status).body(body); // Status + JSON body.
    }
}
