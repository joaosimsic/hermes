package io.github.joaosimsic.infrastructure.adapters.output.storage;

import io.github.joaosimsic.core.domain.PresignedUrl;
import io.github.joaosimsic.core.ports.output.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3StorageAdapter implements StoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Override
    public PresignedUrl generateUploadUrl(UUID mediaId, String objectKey, String contentType, Duration expiration) {
        var request = PutObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .putObjectRequest(builder -> builder
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
            )
            .build();

        var presignedRequest = s3Presigner.presignPutObject(request);
        Instant expiresAt = Instant.now().plus(expiration);

        log.debug("Generated upload URL for mediaId={}, objectKey={}", mediaId, objectKey);

        return PresignedUrl.forUpload(mediaId, presignedRequest.url().toString(), expiresAt);
    }

    @Override
    public PresignedUrl generateDownloadUrl(UUID mediaId, String objectKey, Duration expiration) {
        var request = GetObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .getObjectRequest(builder -> builder
                .bucket(bucketName)
                .key(objectKey)
            )
            .build();

        var presignedRequest = s3Presigner.presignGetObject(request);
        Instant expiresAt = Instant.now().plus(expiration);

        log.debug("Generated download URL for mediaId={}, objectKey={}", mediaId, objectKey);

        return PresignedUrl.forDownload(mediaId, presignedRequest.url().toString(), expiresAt);
    }

    @Override
    public boolean objectExists(String objectKey) {
        try {
            var request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking object existence: objectKey={}", objectKey, e);
            return false;
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        try {
            var request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

            s3Client.deleteObject(request);
            log.info("Deleted object: objectKey={}", objectKey);
        } catch (Exception e) {
            log.error("Error deleting object: objectKey={}", objectKey, e);
        }
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }
}
