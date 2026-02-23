package io.github.joaosimsic.core.ports.output;

import io.github.joaosimsic.core.domain.InboxEntry;
import io.github.joaosimsic.core.domain.Message;

import java.util.List;
import java.util.UUID;

public interface InboxPort {
    
    void save(InboxEntry entry);
    
    List<InboxEntry> findByUserId(UUID userId, int limit);
    
    void updateWithNewMessage(UUID userId, UUID conversationId, Message message, boolean incrementUnread);
    
    void markAsRead(UUID userId, UUID conversationId);
    
    int countUnread(UUID userId);
}
