package com.service.file.upload.s3.notification;

import com.service.file.upload.s3.dto.event.S3Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/s3")
@RequiredArgsConstructor
@Slf4j
public class S3NotificationController {

    private final S3NotificationService s3NotificationService;

    @PostMapping
    public ResponseEntity<Void> consumeObjectCreatedPutEvent(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody S3Event event
    ) {

        log.info("event : {}", event);

        s3NotificationService.consumeObjectCreatedPutEvent(authorization, event);

        return ResponseEntity.accepted().build();
    }
}
