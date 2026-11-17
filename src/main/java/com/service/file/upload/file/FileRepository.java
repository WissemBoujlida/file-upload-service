package com.service.file.upload.file;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    Optional<File> findByS3BucketAndS3Key(String s3Bucket, String s3Key);

    List<File> findByStatus(FileStatus status, Limit limit);
}
