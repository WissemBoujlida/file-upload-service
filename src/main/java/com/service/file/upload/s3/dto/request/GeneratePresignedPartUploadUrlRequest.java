package com.service.file.upload.s3.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GeneratePresignedPartUploadUrlRequest(
        @NotNull Long fileId,

        @NotNull @Positive Integer partNumber,

        @NotNull @Positive Long partSize
) {
}
