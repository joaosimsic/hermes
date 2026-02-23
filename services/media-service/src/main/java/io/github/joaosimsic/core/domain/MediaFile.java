package io.github.joaosimsic.core.domain;

import java.time.Instant;
import java.util.UUID;

public record MediaFile(
    UUID id,
    UUID uploadedBy,
    String fileName,
    String contentType,
    long size,
    String bucket,
    String objectKey,
    MediaStatus status,
    Instant createdAt,
    Instant expiresAt
) {
    public enum MediaStatus {
        PENDING,
        UPLOADED,
        FAILED,
        DELETED
    }

    public static MediaFile createPending(UUID uploadedBy, String fileName, String contentType, long size, String bucket, String objectKey) {
        return new MediaFile(
            UUID.randomUUID(),
            uploadedBy,
            fileName,
            contentType,
            size,
            bucket,
            objectKey,
            MediaStatus.PENDING,
            Instant.now(),
            null
        );
    }

    public MediaFile markUploaded() {
        return new MediaFile(
            id, uploadedBy, fileName, contentType, size, bucket, objectKey,
            MediaStatus.UPLOADED, createdAt, null
        );
    }

    public MediaFile markFailed() {
        return new MediaFile(
            id, uploadedBy, fileName, contentType, size, bucket, objectKey,
            MediaStatus.FAILED, createdAt, null
        );
    }

    public MediaFile markDeleted() {
        return new MediaFile(
            id, uploadedBy, fileName, contentType, size, bucket, objectKey,
            MediaStatus.DELETED, createdAt, null
        );
    }
}
