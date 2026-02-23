package io.github.joaosimsic.core.ports.input;

import io.github.joaosimsic.core.domain.InboxEntry;

import java.util.List;
import java.util.UUID;

public interface InboxUseCase {
    
    List<InboxEntry> getInbox(UUID userId, int limit);
    
    int getTotalUnreadCount(UUID userId);
}
