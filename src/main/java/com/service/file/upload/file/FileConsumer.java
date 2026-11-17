package com.service.file.upload.file;

import com.service.file.upload.exception.FileIntegrityException;
import com.service.file.upload.exception.FileNotFoundException;
import com.service.file.upload.exception.FileUploadException;
import com.service.file.upload.s3.dto.event.FileReadyEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static com.service.file.upload.file.FileStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileConsumer {
    private final FileRepository fileRepository;

    @KafkaListener(topics = "file", groupId = "file-metadata")
    public void markFileAsReady(FileReadyEventPayload payload) {
        String bucket = payload.bucket();
        String key = payload.key();

        File file = fileRepository.findByS3BucketAndS3Key(bucket, key)
                .orElseThrow(() -> new FileNotFoundException("file not found with key: " + key + " in bucket: " + bucket));

        // idempotency check: if file is already READY, QUARANTINED, or FAILED, drop duplicated event
        if (file.getStatus() != UPLOADING) {
            throw new FileUploadException("file already " + file.getStatus() + " with key: " + key + " in bucket: " + bucket);
        }

        // integrity check
        if (!payload.size().equals(file.getSize())) {

            file.setStatus(QUARANTINED);
            fileRepository.save(file);

            throw new FileIntegrityException("file integrity failure with key: " + key + " in bucket: " + bucket);
        }

        file.setStatus(READY);
        file.setETag(payload.eTag());

        fileRepository.save(file);
    }
}
