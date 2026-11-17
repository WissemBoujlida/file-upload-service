package com.service.file.upload.s3.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record GeneratePresignedUploadUrlResponse(
        String uploadUrl,

        Long fileId,

        Instant expiresAt
) {
}
