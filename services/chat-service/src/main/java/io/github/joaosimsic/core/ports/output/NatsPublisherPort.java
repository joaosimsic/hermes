package io.github.joaosimsic.core.ports.output;

import io.github.joaosimsic.core.domain.Message;

import java.util.UUID;

public interface NatsPublisherPort {
    
    void publishMessageToUser(UUID targetUserId, Message message);
    
    void publishTypingToConversation(UUID conversationId, UUID userId);
    
    void publishReadReceipt(UUID conversationId, UUID userId, UUID messageId);
    
    void publishAck(UUID targetUserId, String clientMsgId, UUID messageId);
    
    void publishPresence(UUID userId, String status);
}
