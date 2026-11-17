package com.service.file.upload.cloudflare.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CloudflareResponse<T>(
        List<ResponseInfo> errors,

        List<String> messages,

        T result,

        boolean success
) {
}
