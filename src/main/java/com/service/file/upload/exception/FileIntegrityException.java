package com.service.file.upload.exception;

public class FileIntegrityException extends RuntimeException {
    public FileIntegrityException(String message) {
        super(message);
    }
}
