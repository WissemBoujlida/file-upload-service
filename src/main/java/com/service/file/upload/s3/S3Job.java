package com.service.file.upload.s3;

import com.service.file.upload.cloudflare.CloudflareService;
import com.service.file.upload.cloudflare.dto.CloudflareResponse;
import com.service.file.upload.cloudflare.dto.PurgeCacheResponse;
import com.service.file.upload.file.File;
import com.service.file.upload.file.FileService;
import com.service.file.upload.file.FileStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.service.file.upload.file.FileStatus.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3Job {
    private final S3Service s3Service;
    private final FileService fileService;
    private final CloudflareService cloudflareService;

    @Value("${awss3.public-bucket}")
    private String publicBucket;

    @Value("${cloudflare.cdn-base-url}")
    private String cdnBaseUrl;

    @Scheduled(fixedDelay = 30000)
    public void removePendingDeleteFiles() {
        List<File> pendingDeleteFiles = fileService.findByStatus(PENDING_DELETE, Limit.of(1000));

        if (pendingDeleteFiles.isEmpty()) {
            return;
        }

        Map<String, List<File>> pendingDeleteFilesByBucket = pendingDeleteFiles.stream()
                .collect(Collectors.groupingBy(File::getS3Bucket));

        List<File> files = new ArrayList<>();

        for (Map.Entry<String, List<File>> entry : pendingDeleteFilesByBucket.entrySet()) {
            String bucket = entry.getKey();
            List<File> bucketFiles = entry.getValue();

            List<String> keys = bucketFiles.stream()
                    .map(File::getS3Key)
                    .distinct()
                    .toList();

            List<String> deletedKeys = s3Service.deleteObjects(bucket, keys);

            bucketFiles.stream()
                    .filter(file -> deletedKeys.contains(file.getS3Key()))
                    .forEach(file -> {
                        FileStatus status = bucket.equals(publicBucket) ? PENDING_CACHE_INVALIDATION : DELETED;
                        file.setStatus(status);

                        files.add(file);
                    });
        }

        fileService.saveAll(files);
    }

    @Scheduled(fixedDelay = 60000)
    public void purgePendingCacheInvalidationFiles() {
        // cloudflare rate limits: https://developers.cloudflare.com/cache/how-to/purge-cache
        // single-file purge limits: 100 URLs per request | throughput: 800 URLs per second (up to 8 requests per second)
        List<File> pendingCacheInvalidationFiles = fileService.findByStatus(
                PENDING_CACHE_INVALIDATION,
                Limit.of(100)
        );

        if (pendingCacheInvalidationFiles.isEmpty()) {
            return;
        }

        cdnBaseUrl = cdnBaseUrl.endsWith("/") ? cdnBaseUrl.substring(0, cdnBaseUrl.length() - 1) : cdnBaseUrl;

        List<String> urls = pendingCacheInvalidationFiles.stream()
                .map(this::getFileUrl)
                .toList();

        CloudflareResponse<PurgeCacheResponse> response = cloudflareService.purgeCache(urls);

        if (response != null && response.success()) {
            pendingCacheInvalidationFiles.forEach(file -> file.setStatus(DELETED));

            fileService.saveAll(pendingCacheInvalidationFiles);
        }
    }

    private String getFileUrl(File file) {
        String s3Key = file.getS3Key().startsWith("/") ? file.getS3Key().substring(1) : file.getS3Key();

        return String.format("%s/%s/%s", cdnBaseUrl, file.getS3Bucket(), s3Key);
    }
}
