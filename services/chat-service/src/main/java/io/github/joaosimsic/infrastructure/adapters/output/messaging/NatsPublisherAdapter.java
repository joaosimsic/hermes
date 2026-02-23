package io.github.joaosimsic.infrastructure.adapters.output.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.joaosimsic.core.domain.Message;
import io.github.joaosimsic.core.ports.output.NatsPublisherPort;
import io.nats.client.Connection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NatsPublisherAdapter implements NatsPublisherPort {

    private static final String SUBJECT_CHAT_USER = "chat.user.%s";
    private static final String SUBJECT_PRESENCE_BROADCAST = "presence.broadcast";

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    @Override
    public void publishMessageToUser(UUID targetUserId, Message message) {
        try {
            var envelope = Map.of(
                "type", "message",
                "payload", Map.of(
                    "conversationId", message.conversationId().toString(),
                    "messageId", message.messageId().toString(),
                    "senderId", message.senderId().toString(),
                    "content", message.content() != null ? message.content() : "",
                    "mediaId", message.mediaId() != null ? message.mediaId().toString() : "",
                    "createdAt", message.createdAt().toString()
                )
            );

            String subject = String.format(SUBJECT_CHAT_USER, targetUserId);
            byte[] data = objectMapper.writeValueAsBytes(envelope);
            natsConnection.publish(subject, data);

            log.debug("Published message to user {}: messageId={}", targetUserId, message.messageId());
        } catch (Exception e) {
            log.error("Failed to publish message to user {}", targetUserId, e);
        }
    }

    @Override
    public void publishTypingToConversation(UUID conversationId, UUID userId) {
        try {
            var envelope = Map.of(
                "type", "typing",
                "payload", Map.of(
                    "conversationId", conversationId.toString(),
                    "userId", userId.toString()
                )
            );

            String subject = "chat.conversation." + conversationId;
            byte[] data = objectMapper.writeValueAsBytes(envelope);
            natsConnection.publish(subject, data);
        } catch (Exception e) {
            log.error("Failed to publish typing indicator", e);
        }
    }

    @Override
    public void publishReadReceipt(UUID conversationId, UUID userId, UUID messageId) {
        try {
            var envelope = Map.of(
                "type", "read_receipt",
                "payload", Map.of(
                    "conversationId", conversationId.toString(),
                    "userId", userId.toString(),
                    "messageId", messageId.toString()
                )
            );

            String subject = "chat.conversation." + conversationId;
            byte[] data = objectMapper.writeValueAsBytes(envelope);
            natsConnection.publish(subject, data);
        } catch (Exception e) {
            log.error("Failed to publish read receipt", e);
        }
    }

    @Override
    public void publishAck(UUID targetUserId, String clientMsgId, UUID messageId) {
        try {
            var envelope = Map.of(
                "type", "ack",
                "payload", Map.of(
                    "clientMsgId", clientMsgId,
                    "messageId", messageId.toString()
                )
            );

            String subject = String.format(SUBJECT_CHAT_USER, targetUserId);
            byte[] data = objectMapper.writeValueAsBytes(envelope);
            natsConnection.publish(subject, data);
        } catch (Exception e) {
            log.error("Failed to publish ack", e);
        }
    }

    @Override
    public void publishPresence(UUID userId, String status) {
        try {
            var payload = Map.of(
                "userId", userId.toString(),
                "status", status
            );

            byte[] data = objectMapper.writeValueAsBytes(payload);
            natsConnection.publish(SUBJECT_PRESENCE_BROADCAST, data);
        } catch (Exception e) {
            log.error("Failed to publish presence", e);
        }
    }
}
