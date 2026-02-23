package io.github.joaosimsic.infrastructure.adapters.input.web.responses;

import io.github.joaosimsic.core.domain.MediaFile;

import java.time.Instant;
import java.util.UUID;

public record MediaFileResponse(
    UUID id,
    UUID uploadedBy,
    String fileName,
    String contentType,
    long size,
    String status,
    Instant createdAt
) {
    public static MediaFileResponse from(MediaFile mediaFile) {
        return new MediaFileResponse(
            mediaFile.id(),
            mediaFile.uploadedBy(),
            mediaFile.fileName(),
            mediaFile.contentType(),
            mediaFile.size(),
            mediaFile.status().name(),
            mediaFile.createdAt()
        );
    }
}
