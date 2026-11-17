package com.service.file.upload.exception;

public class CacheInvalidationException extends RuntimeException {
    public CacheInvalidationException(String message) {
        super(message);
    }
}
