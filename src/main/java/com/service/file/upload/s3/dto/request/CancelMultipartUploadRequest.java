package com.service.file.upload.s3.dto.request;

import jakarta.validation.constraints.NotNull;

public record CancelMultipartUploadRequest(
        @NotNull Long fileId
) {
}
