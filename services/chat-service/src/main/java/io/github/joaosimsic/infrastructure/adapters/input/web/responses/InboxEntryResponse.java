package io.github.joaosimsic.infrastructure.adapters.input.web.responses;

import io.github.joaosimsic.core.domain.InboxEntry;
import java.time.Instant;
import java.util.UUID;

public record InboxEntryResponse(
    UUID conversationId,
    Instant lastMessageAt,
    String lastMessagePreview,
    UUID lastMessageSenderId,
    int unreadCount
) {
    public static InboxEntryResponse from(InboxEntry entry) {
        return new InboxEntryResponse(
            entry.conversationId(),
            entry.lastMessageAt(),
            entry.lastMessagePreview(),
            entry.lastMessageSenderId(),
            entry.unreadCount()
        );
    }
}
