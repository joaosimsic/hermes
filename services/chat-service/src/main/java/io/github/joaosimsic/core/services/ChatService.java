package io.github.joaosimsic.core.services;

import io.github.joaosimsic.core.domain.*;
import io.github.joaosimsic.core.exceptions.business.ConversationNotFoundException;
import io.github.joaosimsic.core.exceptions.business.NotParticipantException;
import io.github.joaosimsic.core.ports.input.ConversationUseCase;
import io.github.joaosimsic.core.ports.input.InboxUseCase;
import io.github.joaosimsic.core.ports.input.MessageUseCase;
import io.github.joaosimsic.core.ports.output.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService implements ConversationUseCase, MessageUseCase, InboxUseCase {

    private final ConversationPort conversationPort;
    private final MessagePort messagePort;
    private final InboxPort inboxPort;
    private final NatsPublisherPort natsPublisher;

    @Override
    public Conversation createDirectConversation(UUID creatorId, UUID participantId) {
        var existing = conversationPort.findDirectConversationId(creatorId, participantId);
        if (existing.isPresent()) {
            return conversationPort.findById(existing.get()).orElseThrow();
        }

        var conversation = Conversation.createDirect(creatorId);
        conversationPort.save(conversation);

        var participants = List.of(
            Participant.createOwner(conversation.id(), creatorId),
            Participant.createMember(conversation.id(), participantId)
        );
        conversationPort.saveParticipants(participants);

        return conversation;
    }

    @Override
    public Conversation createGroupConversation(String name, UUID creatorId, List<UUID> participantIds) {
        var conversation = Conversation.createGroup(name, creatorId);
        conversationPort.save(conversation);

        var participants = new ArrayList<Participant>();
        participants.add(Participant.createOwner(conversation.id(), creatorId));
        for (UUID userId : participantIds) {
            if (!userId.equals(creatorId)) {
                participants.add(Participant.createMember(conversation.id(), userId));
            }
        }
        conversationPort.saveParticipants(participants);

        return conversation;
    }

    @Override
    public Optional<Conversation> findById(UUID conversationId) {
        return conversationPort.findById(conversationId);
    }

    @Override
    public Optional<Conversation> findDirectConversation(UUID userId1, UUID userId2) {
        return conversationPort.findDirectConversationId(userId1, userId2)
            .flatMap(conversationPort::findById);
    }

    @Override
    public List<Participant> getParticipants(UUID conversationId) {
        return conversationPort.findParticipants(conversationId);
    }

    @Override
    public void addParticipant(UUID conversationId, UUID userId, UUID addedBy) {
        validateParticipant(conversationId, addedBy);
        var participant = Participant.createMember(conversationId, userId);
        conversationPort.saveParticipant(participant);
    }

    @Override
    public void removeParticipant(UUID conversationId, UUID userId, UUID removedBy) {
        validateParticipant(conversationId, removedBy);
        conversationPort.removeParticipant(conversationId, userId);
    }

    @Override
    public boolean isParticipant(UUID conversationId, UUID userId) {
        return conversationPort.findParticipant(conversationId, userId).isPresent();
    }

    @Override
    public Message sendMessage(UUID conversationId, UUID senderId, String content, UUID mediaId, Message.MessageType type) {
        validateParticipant(conversationId, senderId);

        Message message;
        if (mediaId != null) {
            message = Message.createWithMedia(conversationId, senderId, content, mediaId, type);
        } else {
            message = Message.createText(conversationId, senderId, content);
        }

        messagePort.save(message);

        var participants = conversationPort.findParticipants(conversationId);
        for (Participant participant : participants) {
            boolean isOwnMessage = participant.userId().equals(senderId);
            inboxPort.updateWithNewMessage(participant.userId(), conversationId, message, !isOwnMessage);

            if (!isOwnMessage) {
                natsPublisher.publishMessageToUser(participant.userId(), message);
            }
        }

        log.info("Message sent: conversationId={}, senderId={}, messageId={}",
            conversationId, senderId, message.messageId());

        return message;
    }

    @Override
    public List<Message> getMessages(UUID conversationId, UUID beforeMessageId, int limit) {
        return messagePort.findByConversationIdBefore(conversationId, beforeMessageId, limit);
    }

    @Override
    public List<Message> getRecentMessages(UUID conversationId, int limit) {
        return messagePort.findByConversationId(conversationId, limit);
    }

    @Override
    public void markAsRead(UUID conversationId, UUID userId, UUID messageId) {
        validateParticipant(conversationId, userId);

        conversationPort.updateLastReadMessage(conversationId, userId, messageId);
        inboxPort.markAsRead(userId, conversationId);

        var participants = conversationPort.findParticipants(conversationId);
        for (Participant participant : participants) {
            if (!participant.userId().equals(userId)) {
                natsPublisher.publishReadReceipt(conversationId, userId, messageId);
            }
        }
    }

    @Override
    public void deliverMissedMessages(UUID userId) {
        log.info("Delivering missed messages to user: {}", userId);

        var conversationIds = conversationPort.findConversationIdsByUserId(userId);
        for (UUID conversationId : conversationIds) {
            var participant = conversationPort.findParticipant(conversationId, userId);
            if (participant.isEmpty()) continue;

            var lastReadMessageId = participant.get().lastReadMessageId();
            if (lastReadMessageId == null) {
                var recentMessages = messagePort.findByConversationId(conversationId, 50);
                for (Message message : recentMessages) {
                    if (!message.senderId().equals(userId)) {
                        natsPublisher.publishMessageToUser(userId, message);
                    }
                }
            } else {
                var missedMessages = messagePort.findMessagesAfter(conversationId, lastReadMessageId);
                for (Message message : missedMessages) {
                    if (!message.senderId().equals(userId)) {
                        natsPublisher.publishMessageToUser(userId, message);
                    }
                }
            }
        }
    }

    @Override
    public List<InboxEntry> getInbox(UUID userId, int limit) {
        return inboxPort.findByUserId(userId, limit);
    }

    @Override
    public int getTotalUnreadCount(UUID userId) {
        return inboxPort.countUnread(userId);
    }

    private void validateParticipant(UUID conversationId, UUID userId) {
        var conversation = conversationPort.findById(conversationId)
            .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        if (!isParticipant(conversationId, userId)) {
            throw new NotParticipantException(conversationId, userId);
        }
    }
}
