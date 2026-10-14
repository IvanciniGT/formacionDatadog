package com.course.shop.web;

import com.course.shop.service.PaymentGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PaymentGateway.PaymentException.class)
    public ResponseEntity<Map<String, String>> payment(PaymentGateway.PaymentException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> notFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        // /api/error throws IllegalStateException too: it must stay a 500 so the demo shows errors
        HttpStatus status = e.getMessage().startsWith("Boom") ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }
}
