package io.github.joaosimsic.core.ports.output;

import io.github.joaosimsic.core.domain.MediaFile;

import java.util.Optional;
import java.util.UUID;

public interface MediaFilePort {
    
    MediaFile save(MediaFile mediaFile);
    
    Optional<MediaFile> findById(UUID id);
    
    void delete(UUID id);
    
    void updateStatus(UUID id, MediaFile.MediaStatus status);
}
