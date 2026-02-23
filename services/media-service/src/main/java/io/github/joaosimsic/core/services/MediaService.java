package io.github.joaosimsic.core.services;

import io.github.joaosimsic.core.domain.MediaFile;
import io.github.joaosimsic.core.domain.PresignedUrl;
import io.github.joaosimsic.core.exceptions.business.MediaNotFoundException;
import io.github.joaosimsic.core.exceptions.business.UnauthorizedMediaAccessException;
import io.github.joaosimsic.core.ports.input.MediaUseCase;
import io.github.joaosimsic.core.ports.output.MediaFilePort;
import io.github.joaosimsic.core.ports.output.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService implements MediaUseCase {

    private static final Duration UPLOAD_URL_EXPIRATION = Duration.ofMinutes(15);
    private static final Duration DOWNLOAD_URL_EXPIRATION = Duration.ofHours(1);

    private final MediaFilePort mediaFilePort;
    private final StoragePort storagePort;

    @Override
    public PresignedUrl requestUpload(UUID userId, String fileName, String contentType, long size) {
        String objectKey = generateObjectKey(userId, fileName);
        String bucket = storagePort.getBucketName();

        MediaFile mediaFile = MediaFile.createPending(userId, fileName, contentType, size, bucket, objectKey);
        mediaFilePort.save(mediaFile);

        PresignedUrl presignedUrl = storagePort.generateUploadUrl(
            mediaFile.id(), objectKey, contentType, UPLOAD_URL_EXPIRATION
        );

        log.info("Generated upload URL for user={}, mediaId={}, fileName={}", 
            userId, mediaFile.id(), fileName);

        return presignedUrl;
    }

    @Override
    public void confirmUpload(UUID mediaId, UUID userId) {
        MediaFile mediaFile = mediaFilePort.findById(mediaId)
            .orElseThrow(() -> new MediaNotFoundException(mediaId));

        if (!mediaFile.uploadedBy().equals(userId)) {
            throw new UnauthorizedMediaAccessException(mediaId, userId);
        }

        if (storagePort.objectExists(mediaFile.objectKey())) {
            mediaFilePort.updateStatus(mediaId, MediaFile.MediaStatus.UPLOADED);
            log.info("Confirmed upload for mediaId={}", mediaId);
        } else {
            mediaFilePort.updateStatus(mediaId, MediaFile.MediaStatus.FAILED);
            log.warn("Upload confirmation failed - object not found: mediaId={}", mediaId);
        }
    }

    @Override
    public PresignedUrl getDownloadUrl(UUID mediaId, UUID userId) {
        MediaFile mediaFile = mediaFilePort.findById(mediaId)
            .orElseThrow(() -> new MediaNotFoundException(mediaId));

        if (mediaFile.status() != MediaFile.MediaStatus.UPLOADED) {
            throw new MediaNotFoundException(mediaId);
        }

        return storagePort.generateDownloadUrl(mediaId, mediaFile.objectKey(), DOWNLOAD_URL_EXPIRATION);
    }

    @Override
    public Optional<MediaFile> getMediaFile(UUID mediaId) {
        return mediaFilePort.findById(mediaId);
    }

    @Override
    public void deleteMedia(UUID mediaId, UUID userId) {
        MediaFile mediaFile = mediaFilePort.findById(mediaId)
            .orElseThrow(() -> new MediaNotFoundException(mediaId));

        if (!mediaFile.uploadedBy().equals(userId)) {
            throw new UnauthorizedMediaAccessException(mediaId, userId);
        }

        storagePort.deleteObject(mediaFile.objectKey());
        mediaFilePort.updateStatus(mediaId, MediaFile.MediaStatus.DELETED);

        log.info("Deleted media: mediaId={}, userId={}", mediaId, userId);
    }

    private String generateObjectKey(UUID userId, String fileName) {
        String sanitizedFileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("%s/%s/%s", userId, UUID.randomUUID(), sanitizedFileName);
    }
}
