package com.service.file.upload.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

import static org.springframework.http.HttpStatus.*;


@RestControllerAdvice
public class GlobalExceptionHandler {
    // thrown by Spring when validation on an argument annotated with @Valid fails
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        List<ValidationError> validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .status(BAD_REQUEST)
                .body(ApiError.builder()
                        .validationErrors(validationErrors)
                        .build());
    }

    // thrown by Spring when validation on controller method arguments (@PathVariable, @RequestParam) fails
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception
    ) {
        List<ValidationError> validationErrors = exception.getParameterValidationResults()
                .stream()
                .flatMap(result -> {
                    String field = result.getMethodParameter().getParameterName();
                    return result.getResolvableErrors()
                            .stream()
                            .map(error -> new ValidationError(field, error.getDefaultMessage()));
                })
                .toList();

        return ResponseEntity
                .status(BAD_REQUEST)
                .body(ApiError.builder()
                        .validationErrors(validationErrors)
                        .build());
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ApiError> handleFileUploadException(
            FileUploadException exception
    ) {
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(ApiError.builder()
                        .message(exception.getMessage())
                        .build());

    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ApiError> handleFileNotFoundException(
            FileNotFoundException exception
    ) {
        return ResponseEntity
                .status(NOT_FOUND)
                .body(ApiError.builder()
                        .message(exception.getMessage())
                        .build());
    }

    @ExceptionHandler(BearerAuthenticationException.class)
    public ResponseEntity<ApiError> handleBearerAuthenticationException(BearerAuthenticationException exception) {
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(ApiError.builder()
                        .message(exception.getMessage())
                        .build());
    }

    @ExceptionHandler
    public ResponseEntity<ApiError> handleFileIntegrityException(FileIntegrityException exception) {
        return ResponseEntity.status(CONFLICT)
                .body(ApiError.builder()
                        .message(exception.getMessage())
                        .build());
    }
}
