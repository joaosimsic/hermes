package io.github.joaosimsic.core.domain;

import java.time.Instant;
import java.util.UUID;

public record Participant(
    UUID conversationId,
    UUID userId,
    Instant joinedAt,
    ParticipantRole role,
    UUID lastReadMessageId
) {
    public enum ParticipantRole {
        OWNER,
        ADMIN,
        MEMBER
    }

    public static Participant createOwner(UUID conversationId, UUID userId) {
        return new Participant(
            conversationId,
            userId,
            Instant.now(),
            ParticipantRole.OWNER,
            null
        );
    }

    public static Participant createMember(UUID conversationId, UUID userId) {
        return new Participant(
            conversationId,
            userId,
            Instant.now(),
            ParticipantRole.MEMBER,
            null
        );
    }

    public Participant withLastRead(UUID messageId) {
        return new Participant(
            conversationId,
            userId,
            joinedAt,
            role,
            messageId
        );
    }
}
