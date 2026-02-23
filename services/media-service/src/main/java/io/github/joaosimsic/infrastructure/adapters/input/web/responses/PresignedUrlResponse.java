package io.github.joaosimsic.infrastructure.adapters.input.web.responses;

import io.github.joaosimsic.core.domain.PresignedUrl;

import java.time.Instant;
import java.util.UUID;

public record PresignedUrlResponse(
    UUID mediaId,
    String url,
    String method,
    Instant expiresAt
) {
    public static PresignedUrlResponse from(PresignedUrl presignedUrl) {
        return new PresignedUrlResponse(
            presignedUrl.mediaId(),
            presignedUrl.url(),
            presignedUrl.method(),
            presignedUrl.expiresAt()
        );
    }
}
