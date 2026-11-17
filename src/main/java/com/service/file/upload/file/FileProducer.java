package com.service.file.upload.file;

import com.service.file.upload.s3.dto.event.FileReadyEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileProducer {
    private final KafkaTemplate<String, FileReadyEventPayload> kafkaTemplate;

    public void publishFileReadyEvent(FileReadyEventPayload fileReadyEventPayload) {
        Message<FileReadyEventPayload> message = MessageBuilder
                .withPayload(fileReadyEventPayload)
                .setHeader(KafkaHeaders.TOPIC, "file")
                .build();

        kafkaTemplate.send(message);
    }
}
