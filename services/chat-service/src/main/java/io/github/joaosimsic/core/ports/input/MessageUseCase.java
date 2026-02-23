package io.github.joaosimsic.core.ports.input;

import io.github.joaosimsic.core.domain.Message;

import java.util.List;
import java.util.UUID;

public interface MessageUseCase {
    
    Message sendMessage(UUID conversationId, UUID senderId, String content, UUID mediaId, Message.MessageType type);
    
    List<Message> getMessages(UUID conversationId, UUID beforeMessageId, int limit);
    
    List<Message> getRecentMessages(UUID conversationId, int limit);
    
    void markAsRead(UUID conversationId, UUID userId, UUID messageId);
    
    void deliverMissedMessages(UUID userId);
}
