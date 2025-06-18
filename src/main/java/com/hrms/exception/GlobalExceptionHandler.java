package com.hrms.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Map<String, Object> createErrorBody(HttpStatus status, String errorType, String message, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", errorType);
        body.put("message", message);
        if (request instanceof ServletWebRequest) {
            body.put("path", ((ServletWebRequest) request).getRequest().getRequestURI());
        }
        return body;
    }

    private Map<String, Object> createValidationErrorBody(HttpStatus status, String errorType, List<String> errors, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", errorType);
        body.put("errors", errors); // List of validation error messages
        if (request instanceof ServletWebRequest) {
            body.put("path", ((ServletWebRequest) request).getRequest().getRequestURI());
        }
        return body;
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        logger.error("Resource not found: {} (Path: {})", ex.getMessage(),
            request instanceof ServletWebRequest ? ((ServletWebRequest)request).getRequest().getRequestURI() : "N/A");
        Map<String, Object> body = createErrorBody(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(BadRequestException ex, WebRequest request) {
        logger.error("Bad request: {} (Path: {})", ex.getMessage(),
            request instanceof ServletWebRequest ? ((ServletWebRequest)request).getRequest().getRequestURI() : "N/A");
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        logger.warn("Access denied: {} (Path: {})", ex.getMessage(),
            request instanceof ServletWebRequest ? ((ServletWebRequest)request).getRequest().getRequestURI() : "N/A");
        Map<String, Object> body = createErrorBody(HttpStatus.FORBIDDEN, "Forbidden",
            "Access Denied: You do not have permission to perform this action.", request);
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
        logger.error("Data integrity violation: {} (Path: {})", ex.getMessage(),
            request instanceof ServletWebRequest ? ((ServletWebRequest)request).getRequest().getRequestURI() : "N/A", ex);
        String userMessage = "Data integrity error. This could be due to a duplicate entry or other constraint violation. Please check your input.";
        // More specific message parsing if needed, e.g., for unique constraint names
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String causeMessage = ex.getCause().getMessage().toLowerCase();
            if (causeMessage.contains("unique constraint") || causeMessage.contains("duplicate key")) {
                userMessage = "A record with the provided identifier (e.g., name, email, username) already exists. Please use a unique value.";
            }
        }
        Map<String, Object> body = createErrorBody(HttpStatus.CONFLICT, "Conflict", userMessage, request);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());
        logger.warn("Validation error: {} (Path: {})", errors,
            request instanceof ServletWebRequest ? ((ServletWebRequest)request).getRequest().getRequestURI() : "N/A");
        Map<String, Object> body = createValidationErrorBody(HttpStatus.BAD_REQUEST, "Validation Failed", errors, request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        logger.error("Illegal state: {} (Path: {})", ex.getMessage(),
            request instanceof ServletWebRequest ? ((ServletWebRequest)request).getRequest().getRequestURI() : "N/A");
        // IllegalStateException can often map to 409 Conflict if it's about operational state,
        // or 400 Bad Request if it's about pre-condition for an operation not met due to bad state.
        // Using 409 Conflict here as specified.
        Map<String, Object> body = createErrorBody(HttpStatus.CONFLICT, "Conflict/Invalid State", ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class) // Often used for invalid enum conversions or bad params
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.error("Illegal argument: {} (Path: {})", ex.getMessage(),
            request instanceof ServletWebRequest ? ((ServletWebRequest)request).getRequest().getRequestURI() : "N/A");
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, "Bad Request/Invalid Argument", ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }


    // Optional: Generic fallback handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllUncaughtException(Exception ex, WebRequest request) {
        logger.error("An unexpected error occurred: {} (Path: {})", ex.getMessage(),
            request instanceof ServletWebRequest ? ((ServletWebRequest)request).getRequest().getRequestURI() : "N/A", ex);
        Map<String, Object> body = createErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
            "An unexpected error occurred. Please try again later or contact support if the issue persists.", request);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
