package com.service.file.upload.s3;

import com.service.file.upload.exception.FileNotFoundException;
import com.service.file.upload.exception.FileNotReadyException;
import com.service.file.upload.exception.FileUploadException;
import com.service.file.upload.file.File;
import com.service.file.upload.file.FileRepository;
import com.service.file.upload.file.FileStatus;
import com.service.file.upload.s3.dto.request.*;
import com.service.file.upload.s3.dto.response.GeneratePresignedDownloadUrlResponse;
import com.service.file.upload.s3.dto.response.GeneratePresignedPartUploadUrlResponse;
import com.service.file.upload.s3.dto.response.GeneratePresignedUploadUrlResponse;
import com.service.file.upload.s3.dto.response.StartMultipartUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.time.Duration;
import java.util.*;

import static com.service.file.upload.file.FileStatus.UPLOADING;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final FileRepository fileRepository;

    @Value("${awss3.public-bucket}")
    private String publicBucket;

    @Value("${awss3.presign-signature-duration}")
    private Long signatureDuration;

    @Value("${awss3.max-file-size}")
    private Long maxFileSize;

    @Value("${awss3.max-part-size}")
    private Long maxPartSize;

    public GeneratePresignedUploadUrlResponse generatePresignedUploadUrl(
            GeneratePresignedUploadUrlRequest request
    ) {
        String s3Key = generateS3Key(request.fileName());

        if (request.fileSize() > maxFileSize) {
            throw new FileUploadException("File size is too large");
        }

        File file = fileRepository.save(File.builder()
                .name(request.fileName())
                .size(request.fileSize())
                .contentType(request.contentType())
                .s3Key(s3Key)
                .s3Bucket(publicBucket)
                .build());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(publicBucket)
                .key(s3Key)
                .contentType(request.contentType())
                .contentLength(request.fileSize())
                .build();

        PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(signatureDuration))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(putObjectPresignRequest);

        return GeneratePresignedUploadUrlResponse.builder()
                .uploadUrl(presignedPutObjectRequest.url().toExternalForm())
                .fileId(file.getId())
                .expiresAt(presignedPutObjectRequest.expiration())
                .build();
    }

    public GeneratePresignedDownloadUrlResponse generatePresignedDownloadUrl(
            Long fileId
    ) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with id: " + fileId));

        if (file.getStatus() != FileStatus.READY) {
            throw new FileNotReadyException("File not ready: " + fileId);
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(file.getS3Bucket())
                .key(file.getS3Key())
                // force it to download, use original filename
                .responseContentDisposition(String.format("attachment; fileName=\"%s\"", file.getName()))
                .build();

        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(signatureDuration))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(getObjectPresignRequest);

        return GeneratePresignedDownloadUrlResponse.builder()
                .downloadUrl(presignedGetObjectRequest.url().toExternalForm())
                .expiresAt(presignedGetObjectRequest.expiration())
                .build();
    }

    public StartMultipartUploadResponse startMultipartUpload(
            StartMultipartUploadRequest request
    ) {
        String s3Key = generateS3Key(request.fileName());

        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                .bucket(publicBucket)
                .key(s3Key)
                .contentType(request.contentType())
                .build());

        File file = fileRepository.save(File.builder()
                .name(request.fileName())
                .size(request.fileSize())
                .contentType(request.contentType())
                .s3Key(s3Key)
                .s3Bucket(publicBucket)
                .uploadId(response.uploadId())
                .build());

        return StartMultipartUploadResponse.builder()
                .uploadId(response.uploadId())
                .fileId(file.getId())
                .build();
    }

    public GeneratePresignedPartUploadUrlResponse generatePresignedPartUploadUrl(
            String uploadId,
            GeneratePresignedPartUploadUrlRequest request
    ) {
        File file = fileRepository.findById(request.fileId())
                .orElseThrow(() -> new FileNotFoundException("File not found with id: " + request.fileId()));

        validateMultipartUpload(file, uploadId);

        if (request.partSize() > maxPartSize) {
            throw new FileUploadException("Part size is too large");
        }

        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(file.getS3Bucket())
                .key(file.getS3Key())
                .uploadId(uploadId)
                .partNumber(request.partNumber())
                .contentLength(request.partSize())
                .build();

        UploadPartPresignRequest uploadPartPresignRequest = UploadPartPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(signatureDuration))
                .uploadPartRequest(uploadPartRequest)
                .build();

        PresignedUploadPartRequest presignedUploadPartRequest = s3Presigner.presignUploadPart(uploadPartPresignRequest);

        return GeneratePresignedPartUploadUrlResponse.builder()
                .partUploadUrl(presignedUploadPartRequest.url().toExternalForm())
                .expiresAt(presignedUploadPartRequest.expiration())
                .build();
    }

    public void finishMultipartUpload(
            String uploadId,
            FinishMultipartUploadRequest request
    ) {
        File file = fileRepository.findById(request.fileId())
                .orElseThrow(() -> new FileNotFoundException("File not found with id: " + request.fileId()));

        validateMultipartUpload(file, uploadId);

        List<CompletedPart> completedParts = request.parts()
                .stream()
                .map((part) -> CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(part.eTag())
                        .build())
                .sorted(Comparator.comparingInt(CompletedPart::partNumber))
                .toList();

        s3Client.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                        .uploadId(uploadId)
                        .bucket(file.getS3Bucket())
                        .key(file.getS3Key())
                        .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                        .build());
    }

    public void cancelMultipartUpload(
            String uploadId,
            CancelMultipartUploadRequest request
    ) {
        File file = fileRepository.findById(request.fileId())
                .orElseThrow(() -> new FileNotFoundException("File not found with id: " + request.fileId()));

        validateMultipartUpload(file, uploadId);

        s3Client.abortMultipartUpload(
                AbortMultipartUploadRequest.builder()
                        .uploadId(uploadId)
                        .bucket(file.getS3Bucket())
                        .key(file.getS3Key())
                        .build()
        );

        file.setStatus(FileStatus.FAILED);

        fileRepository.save(file);
    }

    public void deleteFile(Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with id: " + fileId));

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(file.getS3Bucket())
                .key(file.getS3Key())
                .build());
    }

    // S3 max 1000 delete objects per request
    public List<String> deleteObjects(String s3Bucket, List<String> s3Keys) {
        if (s3Keys.isEmpty()) {
            return List.of();
        }

        List<String> keys = new ArrayList<>(new HashSet<>(s3Keys));

        List<String> deletedKeys = new ArrayList<>();

        // batch delete objects
        for (int i = 0; i < keys.size(); i += 1000) {
            List<String> batch = s3Keys.subList(i, Math.min(i + 1000, s3Keys.size()));

            List<ObjectIdentifier> objects = batch.stream()
                    .map(key -> ObjectIdentifier.builder()
                            .key(key)
                            .build())
                    .toList();

            DeleteObjectsResponse response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(s3Bucket)
                    .delete(Delete.builder().objects(objects).quiet(false).build())
                    .build());

            if (response.hasDeleted()) {
                response.deleted().forEach(object -> deletedKeys.add(object.key()));
            }

            if (response.hasErrors()) {
                response.errors().forEach(error ->
                        log.error("Failed to delete s3 object: bucket: {} key: {}, code: {}, msg: {}",
                                s3Bucket, error.key(), error.code(), error.message()));
            }
        }

        return deletedKeys;
    }

    private String generateS3Key(String fileName) {
        return String.format("uploads/%s-%s", UUID.randomUUID(), fileName);
    }

    private void validateMultipartUpload(File file, String uploadId) {
        if (file.getStatus() != UPLOADING) {
            throw new FileUploadException("Cannot upload file with id: " + file.getId() + ". File already " + file.getStatus());
        }

        if (!file.getUploadId().equals(uploadId)) {
            throw new FileUploadException("Invalid upload id " + uploadId);
        }
    }

}
