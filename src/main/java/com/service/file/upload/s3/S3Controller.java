package com.service.file.upload.s3;

import com.service.file.upload.s3.dto.request.*;
import com.service.file.upload.s3.dto.response.GeneratePresignedDownloadUrlResponse;
import com.service.file.upload.s3.dto.response.GeneratePresignedPartUploadUrlResponse;
import com.service.file.upload.s3.dto.response.GeneratePresignedUploadUrlResponse;
import com.service.file.upload.s3.dto.response.StartMultipartUploadResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class S3Controller {
    private final S3Service s3Service;

    @GetMapping("/files/{fileId}/presigned-download-urls")
    public ResponseEntity<GeneratePresignedDownloadUrlResponse> getPresignedDownloadUrl(
            @PathVariable Long fileId
    ) {
        return ResponseEntity.ok(s3Service.generatePresignedDownloadUrl(fileId));
    }

    @PostMapping("/files/presigned-upload-urls")
    public ResponseEntity<GeneratePresignedUploadUrlResponse> getPresignedUploadUrl(
            @Valid @RequestBody GeneratePresignedUploadUrlRequest request
    ) {
        return ResponseEntity.ok(s3Service.generatePresignedUploadUrl(request));
    }

    @PostMapping("/multipart-uploads")
    public ResponseEntity<StartMultipartUploadResponse> startMultipartUpload(
            @Valid @RequestBody StartMultipartUploadRequest request
    ) {
        return ResponseEntity.ok(s3Service.startMultipartUpload(request));
    }

    @PostMapping("/multipart-uploads/{uploadId}/presigned-upload-urls")
    public ResponseEntity<GeneratePresignedPartUploadUrlResponse> getPresignedPartUploadUrl(
            @PathVariable String uploadId,
            @Valid @RequestBody GeneratePresignedPartUploadUrlRequest request
    ) {
        return ResponseEntity.ok(s3Service.generatePresignedPartUploadUrl(uploadId, request));
    }

    @PostMapping("/multipart-uploads/{uploadId}/complete")
    public ResponseEntity<Void> completeMultipartUpload(
            @PathVariable String uploadId,
            @Valid @RequestBody FinishMultipartUploadRequest request
    ) {
        s3Service.finishMultipartUpload(uploadId, request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/multipart-uploads/{uploadId}/cancel")
    public ResponseEntity<Void> cancelMultipartUpload(
            @PathVariable String uploadId,
            @Valid @RequestBody CancelMultipartUploadRequest request
    ) {
        s3Service.cancelMultipartUpload(uploadId, request);

        return ResponseEntity.ok().build();
    }
}
