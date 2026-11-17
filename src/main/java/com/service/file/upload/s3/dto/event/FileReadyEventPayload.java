package com.service.file.upload.s3.dto.event;

import lombok.Builder;

@Builder
public record FileReadyEventPayload(
        String key,

        String bucket,

        String contentType,

        Long size,

        String eTag
) {
}
