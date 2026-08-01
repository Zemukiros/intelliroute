package com.intelliroute.api.web.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain and validation errors to consistent JSON problem responses.
 *
 * <p>Error contract:
 * <pre>{@code
 * {
 *   "timestamp": "2026-08-01T12:00:00Z",
 *   "status": 404,
 *   "error": "UNKNOWN_NODE",
 *   "message": "Unknown origin node: 'Z'",
 *   "details": { ... optional context ... }
 * }
 * }</pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnknownNodeException.class)
    public ResponseEntity<Map<String, Object>> handleUnknownNode(UnknownNodeException ex) {
        return problem(HttpStatus.NOT_FOUND, "UNKNOWN_NODE", ex.getMessage(),
                Map.of("nodeId", ex.nodeId(), "role", ex.role()));
    }

    @ExceptionHandler(RouteUnreachableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreachable(RouteUnreachableException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "ROUTE_UNREACHABLE", ex.getMessage(),
                Map.of("origin", ex.origin(),
                        "destination", ex.destination(),
                        "visitedNodes", ex.visitedNodes()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body is missing or not valid JSON", Map.of());
    }

    private static ResponseEntity<Map<String, Object>> problem(
            HttpStatus status, String code, String message, Map<String, Object> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", code);
        body.put("message", message);
        if (!details.isEmpty()) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
