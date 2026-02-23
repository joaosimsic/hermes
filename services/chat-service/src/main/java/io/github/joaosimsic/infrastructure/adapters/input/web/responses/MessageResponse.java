package io.github.joaosimsic.infrastructure.adapters.input.web.responses;

import io.github.joaosimsic.core.domain.Message;
import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
    UUID conversationId,
    UUID messageId,
    UUID senderId,
    String content,
    UUID mediaId,
    String type,
    Instant createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
            message.conversationId(),
            message.messageId(),
            message.senderId(),
            message.content(),
            message.mediaId(),
            message.type().name(),
            message.createdAt()
        );
    }
}
