package io.github.joaosimsic.infrastructure.adapters.input.web.controllers;

import io.github.joaosimsic.core.domain.Conversation;
import io.github.joaosimsic.core.domain.InboxEntry;
import io.github.joaosimsic.core.domain.Message;
import io.github.joaosimsic.core.domain.Participant;
import io.github.joaosimsic.core.ports.input.ConversationUseCase;
import io.github.joaosimsic.core.ports.input.InboxUseCase;
import io.github.joaosimsic.core.ports.input.MessageUseCase;
import io.github.joaosimsic.infrastructure.adapters.input.web.filters.GatewayPrincipal;
import io.github.joaosimsic.infrastructure.adapters.input.web.requests.*;
import io.github.joaosimsic.infrastructure.adapters.input.web.responses.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationUseCase conversationUseCase;
    private final MessageUseCase messageUseCase;
    private final InboxUseCase inboxUseCase;

    @PostMapping("/conversations/direct")
    public ResponseEntity<ConversationResponse> createDirectConversation(
            @AuthenticationPrincipal GatewayPrincipal principal,
            @Valid @RequestBody CreateDirectConversationRequest request) {

        UUID userId = UUID.fromString(principal.userId());
        Conversation conversation = conversationUseCase.createDirectConversation(userId, request.participantId());
        return ResponseEntity.ok(ConversationResponse.from(conversation));
    }

    @PostMapping("/conversations/group")
    public ResponseEntity<ConversationResponse> createGroupConversation(
            @AuthenticationPrincipal GatewayPrincipal principal,
            @Valid @RequestBody CreateGroupConversationRequest request) {

        UUID userId = UUID.fromString(principal.userId());
        Conversation conversation = conversationUseCase.createGroupConversation(
            request.name(), userId, request.participantIds());
        return ResponseEntity.ok(ConversationResponse.from(conversation));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable UUID conversationId) {

        return conversationUseCase.findById(conversationId)
            .map(ConversationResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/conversations/{conversationId}/participants")
    public ResponseEntity<List<ParticipantResponse>> getParticipants(
            @PathVariable UUID conversationId) {

        List<Participant> participants = conversationUseCase.getParticipants(conversationId);
        List<ParticipantResponse> response = participants.stream()
            .map(ParticipantResponse::from)
            .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal GatewayPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {

        UUID userId = UUID.fromString(principal.userId());
        Message message = messageUseCase.sendMessage(
            conversationId,
            userId,
            request.content(),
            request.mediaId(),
            request.type() != null ? request.type() : Message.MessageType.TEXT
        );
        return ResponseEntity.ok(MessageResponse.from(message));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) UUID before,
            @RequestParam(defaultValue = "50") int limit) {

        List<Message> messages;
        if (before != null) {
            messages = messageUseCase.getMessages(conversationId, before, limit);
        } else {
            messages = messageUseCase.getRecentMessages(conversationId, limit);
        }

        List<MessageResponse> response = messages.stream()
            .map(MessageResponse::from)
            .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal GatewayPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody MarkReadRequest request) {

        UUID userId = UUID.fromString(principal.userId());
        messageUseCase.markAsRead(conversationId, userId, request.messageId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/inbox")
    public ResponseEntity<InboxResponse> getInbox(
            @AuthenticationPrincipal GatewayPrincipal principal,
            @RequestParam(defaultValue = "50") int limit) {

        UUID userId = UUID.fromString(principal.userId());
        List<InboxEntry> entries = inboxUseCase.getInbox(userId, limit);
        int totalUnread = inboxUseCase.getTotalUnreadCount(userId);

        List<InboxEntryResponse> entryResponses = entries.stream()
            .map(InboxEntryResponse::from)
            .toList();

        return ResponseEntity.ok(new InboxResponse(entryResponses, totalUnread));
    }
}
