package io.github.joaosimsic.infrastructure.adapters.input.web.requests;

import io.github.joaosimsic.core.domain.Message;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record SendMessageRequest(
    @NotBlank String content,
    UUID mediaId,
    Message.MessageType type
) {}
