package com.service.file.upload.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        String message,
        List<ValidationError> validationErrors
) {
}
