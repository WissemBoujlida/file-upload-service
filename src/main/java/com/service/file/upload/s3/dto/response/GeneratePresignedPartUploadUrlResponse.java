package com.service.file.upload.s3.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record GeneratePresignedPartUploadUrlResponse(
        String partUploadUrl,

        Instant expiresAt
) {
}
