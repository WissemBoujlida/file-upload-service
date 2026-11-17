package com.service.file.upload.exception;

public class BearerAuthenticationException extends RuntimeException {
    public BearerAuthenticationException(String message) {
        super(message);
    }
}
