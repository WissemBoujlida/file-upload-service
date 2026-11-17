package com.service.file.upload.file;

public enum FileStatus {
    UPLOADING,

    READY,

    FAILED,

    QUARANTINED,

    PENDING_DELETE,

    PENDING_CACHE_INVALIDATION,

    DELETED
}
