package io.github.joaosimsic.infrastructure.adapters.input.web.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record UploadRequest(
    @NotBlank String fileName,
    @NotBlank String contentType,
    @Positive long size
) {}
