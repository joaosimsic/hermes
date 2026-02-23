package io.github.joaosimsic.core.ports.output;

import io.github.joaosimsic.core.domain.Conversation;
import io.github.joaosimsic.core.domain.Participant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationPort {
    
    Conversation save(Conversation conversation);
    
    Optional<Conversation> findById(UUID conversationId);
    
    void saveParticipant(Participant participant);
    
    void saveParticipants(List<Participant> participants);
    
    void removeParticipant(UUID conversationId, UUID userId);
    
    List<Participant> findParticipants(UUID conversationId);
    
    Optional<Participant> findParticipant(UUID conversationId, UUID userId);
    
    List<UUID> findConversationIdsByUserId(UUID userId);
    
    Optional<UUID> findDirectConversationId(UUID userId1, UUID userId2);
    
    void updateLastReadMessage(UUID conversationId, UUID userId, UUID messageId);
}
