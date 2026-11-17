package com.service.file.upload.exception;

import lombok.Builder;

@Builder
public record ValidationError(
        String field,
        String message
) {
}
