package com.smith.web.error;

import com.smith.domain.exception.LlmProviderException;
import com.smith.domain.exception.UnknownModelException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ApiError(
                HttpStatus.BAD_REQUEST.value(), "Validation failed", message));
    }

    @ExceptionHandler(UnknownModelException.class)
    public ResponseEntity<ApiError> handleUnknownModel(UnknownModelException ex) {
        return ResponseEntity.badRequest().body(new ApiError(
                HttpStatus.BAD_REQUEST.value(), "Unknown model", ex.getMessage()));
    }

    @ExceptionHandler(LlmProviderException.class)
    public ResponseEntity<ApiError> handleProvider(LlmProviderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ApiError(
                HttpStatus.BAD_GATEWAY.value(), "LLM provider error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal error", ex.getMessage()));
    }
}
