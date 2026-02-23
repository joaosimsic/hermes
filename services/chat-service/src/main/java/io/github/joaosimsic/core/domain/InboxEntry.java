package io.github.joaosimsic.core.domain;

import java.time.Instant;
import java.util.UUID;

public record InboxEntry(
    UUID userId,
    UUID conversationId,
    Instant lastMessageAt,
    String lastMessagePreview,
    UUID lastMessageSenderId,
    int unreadCount
) {
    public static InboxEntry create(UUID userId, UUID conversationId, Message lastMessage) {
        String preview = truncateContent(lastMessage.content());
        return new InboxEntry(
            userId,
            conversationId,
            lastMessage.createdAt(),
            preview,
            lastMessage.senderId(),
            0
        );
    }

    public InboxEntry incrementUnread() {
        return new InboxEntry(
            userId,
            conversationId,
            lastMessageAt,
            lastMessagePreview,
            lastMessageSenderId,
            unreadCount + 1
        );
    }

    public InboxEntry markAsRead() {
        return new InboxEntry(
            userId,
            conversationId,
            lastMessageAt,
            lastMessagePreview,
            lastMessageSenderId,
            0
        );
    }

    public InboxEntry withLastMessage(Message message) {
        String preview = truncateContent(message.content());
        return new InboxEntry(
            userId,
            conversationId,
            message.createdAt(),
            preview,
            message.senderId(),
            unreadCount
        );
    }

    private static String truncateContent(String content) {
        if (content == null) return "";
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
}
