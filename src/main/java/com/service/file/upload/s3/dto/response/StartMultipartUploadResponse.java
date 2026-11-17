package com.service.file.upload.s3.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record StartMultipartUploadResponse(
        @NotNull Long fileId,

            @NotBlank String uploadId
) {
}
