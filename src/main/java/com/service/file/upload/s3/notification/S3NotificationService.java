package com.service.file.upload.s3.notification;

import com.service.file.upload.exception.BearerAuthenticationException;
import com.service.file.upload.file.FileProducer;
import com.service.file.upload.s3.dto.event.FileReadyEventPayload;
import com.service.file.upload.s3.dto.event.S3Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3NotificationService {
    private final FileProducer fileProducer;

    @Value("${awss3.webhook-auth-token}")
    private String webhookAuthToken;

    public void consumeObjectCreatedPutEvent(String authorization, S3Event event) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BearerAuthenticationException("Missing or invalid Bearer Token");
        }

        String token = authorization.substring(7);

        // "O(1) equals" to prevent timing attacks
        if (!MessageDigest.isEqual(
                webhookAuthToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))
        ) {
            throw new BearerAuthenticationException("Missing or invalid Bearer Token");
        }

        for (S3Event.S3EventRecord record : event.records()) {
            String bucket = record.s3().bucket().name();
            String key = URLDecoder.decode(record.s3().object().key(), StandardCharsets.UTF_8);

            fileProducer.publishFileReadyEvent(
                    FileReadyEventPayload.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(record.s3().object().contentType())
                            .size(record.s3().object().size())
                            .eTag(record.s3().object().eTag())
                            .build()
            );
        }
    }
}
