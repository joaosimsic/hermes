package io.github.joaosimsic.core.ports.input;

import io.github.joaosimsic.core.domain.Conversation;
import io.github.joaosimsic.core.domain.Participant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationUseCase {
    
    Conversation createDirectConversation(UUID creatorId, UUID participantId);
    
    Conversation createGroupConversation(String name, UUID creatorId, List<UUID> participantIds);
    
    Optional<Conversation> findById(UUID conversationId);
    
    Optional<Conversation> findDirectConversation(UUID userId1, UUID userId2);
    
    List<Participant> getParticipants(UUID conversationId);
    
    void addParticipant(UUID conversationId, UUID userId, UUID addedBy);
    
    void removeParticipant(UUID conversationId, UUID userId, UUID removedBy);
    
    boolean isParticipant(UUID conversationId, UUID userId);
}
