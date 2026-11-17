package com.service.file.upload.cloudflare;

import com.service.file.upload.cloudflare.dto.CloudflareResponse;
import com.service.file.upload.cloudflare.dto.PurgeCacheRequest;
import com.service.file.upload.cloudflare.dto.PurgeCacheResponse;
import com.service.file.upload.exception.CacheInvalidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudflareService {
    private final RestClient cloudflareClient;
    private final ObjectMapper mapper;

    public CloudflareResponse<PurgeCacheResponse> purgeCache(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }

        return cloudflareClient.post()
                .uri("/purge_cache")
                .body(PurgeCacheRequest.builder().files(urls).build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, ((request, response) -> {
                    String errorMsg = "HTTP " + response.getStatusCode();
                    try {
                        CloudflareResponse<?> res = mapper.readValue(response.getBody(), CloudflareResponse.class);

                        if (res != null && res.errors() != null) {
                            errorMsg = res.errors().stream()
                                    .map(error -> String.format("code: %s :: msg: %s", error.code(), error.message()))
                                    .collect(Collectors.joining("\n"));
                        }
                    } catch (IOException e) {
                        // Fallback to HTTP status if body can't be parsed
                    }

                    throw new CacheInvalidationException("An error occurred while purging cache: \n" + errorMsg);
                }
                ))
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
