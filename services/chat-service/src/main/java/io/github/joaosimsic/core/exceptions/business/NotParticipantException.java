package io.github.joaosimsic.core.exceptions.business;

import io.github.joaosimsic.core.exceptions.abstracts.BusinessException;

import java.util.UUID;

public class NotParticipantException extends BusinessException {
    
    public NotParticipantException(UUID conversationId, UUID userId) {
        super("User " + userId + " is not a participant of conversation " + conversationId);
    }
}
