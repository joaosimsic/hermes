package io.github.joaosimsic.core.domain;

import java.time.Instant;
import java.util.UUID;

public record Conversation(
    UUID id,
    ConversationType type,
    String name,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt
) {
    public enum ConversationType {
        DIRECT,
        GROUP
    }

    public static Conversation createDirect(UUID createdBy) {
        var now = Instant.now();
        return new Conversation(
            UUID.randomUUID(),
            ConversationType.DIRECT,
            null,
            createdBy,
            now,
            now
        );
    }

    public static Conversation createGroup(String name, UUID createdBy) {
        var now = Instant.now();
        return new Conversation(
            UUID.randomUUID(),
            ConversationType.GROUP,
            name,
            createdBy,
            now,
            now
        );
    }
}
