package com.service.file.upload.cloudflare.dto;

import lombok.Builder;

@Builder
public record PurgeCacheResponse(
        String id
) {
}
