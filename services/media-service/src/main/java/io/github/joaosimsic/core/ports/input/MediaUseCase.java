package io.github.joaosimsic.core.ports.input;

import io.github.joaosimsic.core.domain.MediaFile;
import io.github.joaosimsic.core.domain.PresignedUrl;

import java.util.Optional;
import java.util.UUID;

public interface MediaUseCase {
    
    PresignedUrl requestUpload(UUID userId, String fileName, String contentType, long size);
    
    void confirmUpload(UUID mediaId, UUID userId);
    
    PresignedUrl getDownloadUrl(UUID mediaId, UUID userId);
    
    Optional<MediaFile> getMediaFile(UUID mediaId);
    
    void deleteMedia(UUID mediaId, UUID userId);
}
