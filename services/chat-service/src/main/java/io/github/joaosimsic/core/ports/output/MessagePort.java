package io.github.joaosimsic.core.ports.output;

import io.github.joaosimsic.core.domain.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessagePort {
    
    Message save(Message message);
    
    List<Message> findByConversationId(UUID conversationId, int limit);
    
    List<Message> findByConversationIdBefore(UUID conversationId, UUID beforeMessageId, int limit);
    
    Optional<Message> findLastMessage(UUID conversationId);
    
    List<Message> findMessagesAfter(UUID conversationId, UUID afterMessageId);
}
