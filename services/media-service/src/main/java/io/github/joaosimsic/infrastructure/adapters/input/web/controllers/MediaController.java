package io.github.joaosimsic.infrastructure.adapters.input.web.controllers;

import io.github.joaosimsic.core.domain.MediaFile;
import io.github.joaosimsic.core.domain.PresignedUrl;
import io.github.joaosimsic.core.ports.input.MediaUseCase;
import io.github.joaosimsic.infrastructure.adapters.input.web.filters.GatewayPrincipal;
import io.github.joaosimsic.infrastructure.adapters.input.web.requests.UploadRequest;
import io.github.joaosimsic.infrastructure.adapters.input.web.responses.MediaFileResponse;
import io.github.joaosimsic.infrastructure.adapters.input.web.responses.PresignedUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaUseCase mediaUseCase;

    @PostMapping("/upload")
    public ResponseEntity<PresignedUrlResponse> requestUpload(
            @AuthenticationPrincipal GatewayPrincipal principal,
            @Valid @RequestBody UploadRequest request) {

        UUID userId = UUID.fromString(principal.userId());
        PresignedUrl presignedUrl = mediaUseCase.requestUpload(
            userId,
            request.fileName(),
            request.contentType(),
            request.size()
        );

        return ResponseEntity.ok(PresignedUrlResponse.from(presignedUrl));
    }

    @PostMapping("/{mediaId}/confirm")
    public ResponseEntity<Void> confirmUpload(
            @AuthenticationPrincipal GatewayPrincipal principal,
            @PathVariable UUID mediaId) {

        UUID userId = UUID.fromString(principal.userId());
        mediaUseCase.confirmUpload(mediaId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{mediaId}/download")
    public ResponseEntity<PresignedUrlResponse> getDownloadUrl(
            @AuthenticationPrincipal GatewayPrincipal principal,
            @PathVariable UUID mediaId) {

        UUID userId = UUID.fromString(principal.userId());
        PresignedUrl presignedUrl = mediaUseCase.getDownloadUrl(mediaId, userId);
        return ResponseEntity.ok(PresignedUrlResponse.from(presignedUrl));
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaFileResponse> getMediaFile(@PathVariable UUID mediaId) {
        return mediaUseCase.getMediaFile(mediaId)
            .map(MediaFileResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> deleteMedia(
            @AuthenticationPrincipal GatewayPrincipal principal,
            @PathVariable UUID mediaId) {

        UUID userId = UUID.fromString(principal.userId());
        mediaUseCase.deleteMedia(mediaId, userId);
        return ResponseEntity.noContent().build();
    }
}
