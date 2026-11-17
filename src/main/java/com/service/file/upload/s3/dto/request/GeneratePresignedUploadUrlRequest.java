package com.service.file.upload.s3.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GeneratePresignedUploadUrlRequest(
        @NotBlank String fileName,

        @NotBlank String contentType,

        @Positive @NotNull Long fileSize
) {
}
