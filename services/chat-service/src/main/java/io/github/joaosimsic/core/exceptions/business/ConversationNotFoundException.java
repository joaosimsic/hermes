package io.github.joaosimsic.core.exceptions.business;

import io.github.joaosimsic.core.exceptions.abstracts.BusinessException;

import java.util.UUID;

public class ConversationNotFoundException extends BusinessException {
    
    public ConversationNotFoundException(UUID conversationId) {
        super("Conversation not found: " + conversationId);
    }
}
