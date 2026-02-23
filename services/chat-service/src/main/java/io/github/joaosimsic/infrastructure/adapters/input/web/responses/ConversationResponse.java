package io.github.joaosimsic.infrastructure.adapters.input.web.responses;

import io.github.joaosimsic.core.domain.Conversation;
import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
    UUID id,
    String type,
    String name,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt
) {
    public static ConversationResponse from(Conversation conversation) {
        return new ConversationResponse(
            conversation.id(),
            conversation.type().name(),
            conversation.name(),
            conversation.createdBy(),
            conversation.createdAt(),
            conversation.updatedAt()
        );
    }
}
