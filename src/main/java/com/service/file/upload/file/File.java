package com.service.file.upload.file;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

import static com.service.file.upload.file.FileStatus.UPLOADING;
import static jakarta.persistence.EnumType.STRING;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "file", uniqueConstraints = {@UniqueConstraint(columnNames = {"s3_key", "s3_bucket"})})
public class File {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String contentType;

    @Positive(message = "File size must be > 0")
    @Column(nullable = false)
    private Long size;

    @Enumerated(STRING)
    @Column(nullable = false)
    @Builder.Default
    private FileStatus status = UPLOADING;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "s3_bucket", nullable = false)
    private String s3Bucket;

    @Column(name = "e_tag", length = 256)
    private String eTag;

    @Column(name = "upload_id", length = 256)
    private String uploadId;
}