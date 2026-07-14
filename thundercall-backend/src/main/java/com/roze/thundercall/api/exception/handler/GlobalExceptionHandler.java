package com.roze.thundercall.api.exception.handler;

import com.roze.thundercall.api.exception.AuthException;
import com.roze.thundercall.api.exception.ResourceExistException;
import com.roze.thundercall.api.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceExistException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateException(ResourceExistException ex) {
        ErrorResponse response = new ErrorResponse();
        response.setStatus(HttpStatus.CONFLICT.value());
        response.setMessage(ex.getMessage());
        response.setTimestamp(Instant.now());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);

    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse response = new ErrorResponse();
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setMessage(ex.getMessage());
        response.setTimestamp(Instant.now());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthException ex) {
        ErrorResponse response = new ErrorResponse();
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setMessage(ex.getMessage());
        response.setTimestamp(Instant.now());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // FIX: this was previously unhandled — application-level validation
    // errors (like "a folder with this name already exists here") fell
    // through with no clear status or message, which is exactly what made
    // a genuine, easily-fixable business rule look like a mysterious
    // session/auth failure on the client for several rounds of debugging.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse response = new ErrorResponse();
        response.setStatus(HttpStatus.CONFLICT.value());
        response.setMessage(ex.getMessage());
        response.setTimestamp(Instant.now());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
}