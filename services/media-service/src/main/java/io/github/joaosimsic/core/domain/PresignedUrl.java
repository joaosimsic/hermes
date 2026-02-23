package io.github.joaosimsic.core.domain;

import java.time.Instant;
import java.util.UUID;

public record PresignedUrl(
    UUID mediaId,
    String url,
    String method,
    Instant expiresAt
) {
    public static PresignedUrl forUpload(UUID mediaId, String url, Instant expiresAt) {
        return new PresignedUrl(mediaId, url, "PUT", expiresAt);
    }

    public static PresignedUrl forDownload(UUID mediaId, String url, Instant expiresAt) {
        return new PresignedUrl(mediaId, url, "GET", expiresAt);
    }
}
