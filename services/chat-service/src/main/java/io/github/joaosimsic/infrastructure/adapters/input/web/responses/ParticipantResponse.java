package io.github.joaosimsic.infrastructure.adapters.input.web.responses;

import io.github.joaosimsic.core.domain.Participant;
import java.time.Instant;
import java.util.UUID;

public record ParticipantResponse(
    UUID conversationId,
    UUID userId,
    Instant joinedAt,
    String role
) {
    public static ParticipantResponse from(Participant participant) {
        return new ParticipantResponse(
            participant.conversationId(),
            participant.userId(),
            participant.joinedAt(),
            participant.role().name()
        );
    }
}
