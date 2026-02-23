package io.github.joaosimsic.core.domain;

import java.time.Instant;
import java.util.UUID;

public record Message(
    UUID conversationId,
    UUID messageId,
    UUID senderId,
    String content,
    UUID mediaId,
    MessageType type,
    Instant createdAt
) {
    public enum MessageType {
        TEXT,
        IMAGE,
        FILE,
        AUDIO,
        VIDEO,
        SYSTEM
    }

    public static Message createText(UUID conversationId, UUID senderId, String content) {
        return new Message(
            conversationId,
            UUID.randomUUID(),
            senderId,
            content,
            null,
            MessageType.TEXT,
            Instant.now()
        );
    }

    public static Message createWithMedia(UUID conversationId, UUID senderId, String content, UUID mediaId, MessageType type) {
        return new Message(
            conversationId,
            UUID.randomUUID(),
            senderId,
            content,
            mediaId,
            type,
            Instant.now()
        );
    }
}
