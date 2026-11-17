package com.service.file.upload.cloudflare.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record ResponseInfo(
        @Min(1000) Integer code,

        String message,

        @JsonProperty("documentation_url")
        String documentationUrl,

        ResponseSource source
) {
    record ResponseSource(
            String pointer
    ) {
    }
}
