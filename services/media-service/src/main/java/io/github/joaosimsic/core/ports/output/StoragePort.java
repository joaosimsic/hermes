package io.github.joaosimsic.core.ports.output;

import io.github.joaosimsic.core.domain.PresignedUrl;

import java.time.Duration;
import java.util.UUID;

public interface StoragePort {
    
    PresignedUrl generateUploadUrl(UUID mediaId, String objectKey, String contentType, Duration expiration);
    
    PresignedUrl generateDownloadUrl(UUID mediaId, String objectKey, Duration expiration);
    
    boolean objectExists(String objectKey);
    
    void deleteObject(String objectKey);
    
    String getBucketName();
}
