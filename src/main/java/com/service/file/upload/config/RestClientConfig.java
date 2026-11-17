package com.service.file.upload.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient cloudflareClient(
            @Value("${cloudflare.api-token}") String apiToken,
            @Value("${cloudflare.zone-id}") String zoneId
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        return RestClient.builder()
                .baseUrl("https://api.cloudflare.com/client/v4/zones/" + zoneId)
                .requestFactory(requestFactory)
                .defaultHeader(AUTHORIZATION, "Bearer " + apiToken)
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .build();
    }
}
