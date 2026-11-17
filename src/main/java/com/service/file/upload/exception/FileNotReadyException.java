package com.service.file.upload.exception;

public class FileNotReadyException extends RuntimeException {
    public FileNotReadyException(String message) {
        super(message);
    }
}
