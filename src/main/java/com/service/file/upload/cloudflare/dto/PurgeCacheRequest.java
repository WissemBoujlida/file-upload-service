package com.service.file.upload.cloudflare.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record PurgeCacheRequest(
        List<String> files
) {
}
