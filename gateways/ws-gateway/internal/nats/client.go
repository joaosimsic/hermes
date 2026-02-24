package nats

import (
	"encoding/json"
	"fmt"
	"sync"

	"github.com/joaosimsic/hermes/ws-gateway/internal/protocol"
	"github.com/nats-io/nats.go"
	"go.uber.org/zap"
)

const (
	SubjectChatUser          = "chat.user.%s"
	SubjectChatConversation  = "chat.conversation.%s"
	SubjectPresence          = "presence.%s"
	SubjectPresenceBroadcast = "presence.broadcast"
	SubjectUserOnline        = "user.online"
	SubjectSendMessage       = "chat.send"
	SubjectMarkRead          = "chat.read"
)

type MessageHandler interface {
	OnMessageReceived(targetUserID string, msg *protocol.MessagePayload)
	OnTypingReceived(targetUserID string, payload *protocol.TypingPayload)
	OnReadReceiptReceived(targetUserID string, payload *protocol.ReadReceiptPayload)
	OnPresenceReceived(payload *protocol.PresencePayload)
	OnAckReceived(targetUserID string, payload *protocol.AckPayload)
}

type Client struct {
	conn    *nats.Conn
	logger  *zap.Logger
	mu      sync.RWMutex
	handler MessageHandler
	subs    []*nats.Subscription
}

func NewClient(url string, logger *zap.Logger) (*Client, error) {
	opts := []nats.Option{
		nats.RetryOnFailedConnect(true),
		nats.MaxReconnects(-1),
		nats.ReconnectWait(nats.DefaultReconnectWait),
		nats.DisconnectErrHandler(func(_ *nats.Conn, err error) {
			if err != nil {
				logger.Warn("NATS disconnected", zap.Error(err))
			}
		}),
		nats.ReconnectHandler(func(_ *nats.Conn) {
			logger.Info("NATS reconnected")
		}),
	}

	nc, err := nats.Connect(url, opts...)
	if err != nil {
		return nil, fmt.Errorf("nats: failed to connect to %s: %w", url, err)
	}

	return &Client{
		conn:   nc,
		logger: logger.Named("nats_client"),
	}, nil
}

func (c *Client) SetHandler(handler MessageHandler) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.handler = handler
}

func (c *Client) SubscribeToUserMessages(userID string) error {
	subject := fmt.Sprintf(SubjectChatUser, userID)
	sub, err := c.conn.Subscribe(subject, func(msg *nats.Msg) {
		c.handleUserMessage(userID, msg)
	})
	if err != nil {
		return fmt.Errorf("nats: failed to subscribe to user %s: %w", userID, err)
	}

	c.trackSubscription(sub)
	return nil
}

func (c *Client) SubscribeToPresenceBroadcast() error {
	sub, err := c.conn.Subscribe(SubjectPresenceBroadcast, func(msg *nats.Msg) {
		var payload protocol.PresencePayload
		if err := json.Unmarshal(msg.Data, &payload); err != nil {
			c.logger.Error("failed to unmarshal presence broadcast", zap.Error(err))
			return
		}

		c.mu.RLock()
		h := c.handler
		c.mu.RUnlock()

		if h != nil {
			h.OnPresenceReceived(&payload)
		}
	})
	if err != nil {
		return fmt.Errorf("nats: failed to subscribe to presence broadcast: %w", err)
	}

	c.trackSubscription(sub)
	return nil
}

func (c *Client) handleUserMessage(userID string, msg *nats.Msg) {
	var envelope protocol.Envelope
	if err := json.Unmarshal(msg.Data, &envelope); err != nil {
		c.logger.Warn("failed to unmarshal NATS envelope", zap.Error(err), zap.String("userID", userID))
		return
	}

	c.mu.RLock()
	h := c.handler
	c.mu.RUnlock()

	if h == nil {
		return
	}

	switch envelope.Type {
	case protocol.TypeMessage:
		unmarshalAndHandle(c, envelope.Payload, userID, h.OnMessageReceived)
	case protocol.TypeTyping:
		unmarshalAndHandle(c, envelope.Payload, userID, h.OnTypingReceived)
	case protocol.TypeReadReceipt:
		unmarshalAndHandle(c, envelope.Payload, userID, h.OnReadReceiptReceived)
	case protocol.TypeAck:
		unmarshalAndHandle(c, envelope.Payload, userID, h.OnAckReceived)
	default:
		c.logger.Debug("received unknown envelope type", zap.String("type", string(envelope.Type)))
	}
}

func (c *Client) PublishMessage(senderID string, msg *protocol.SendMessagePayload) error {
	payload := map[string]any{
		"senderId":       senderID,
		"conversationId": msg.ConversationID,
		"content":        msg.Content,
		"mediaId":        msg.MediaID,
		"clientMsgId":    msg.ClientMsgID,
	}
	return c.publish(SubjectSendMessage, payload)
}

func (c *Client) PublishTyping(userID, conversationID string) error {
	subject := fmt.Sprintf(SubjectChatConversation, conversationID)
	env, err := protocol.NewEnvelope(protocol.TypeTyping, protocol.TypingPayload{
		ConversationID: conversationID,
		UserID:         userID,
	})
	if err != nil {
		return err
	}

	data, err := env.Bytes()
	if err != nil {
		return err
	}
	return c.conn.Publish(subject, data)
}

func (c *Client) PublishMarkRead(userID, conversationID, messageID string) error {
	payload := map[string]string{
		"userId":         userID,
		"conversationId": conversationID,
		"messageId":      messageID,
	}
	return c.publish(SubjectMarkRead, payload)
}

func (c *Client) PublishPresence(userID, status string) error {
	payload := protocol.PresencePayload{
		UserID: userID,
		Status: status,
	}
	return c.publish(SubjectPresenceBroadcast, payload)
}

func (c *Client) PublishUserOnline(userID string) error {
	payload := map[string]string{"userId": userID}
	return c.publish(SubjectUserOnline, payload)
}

func (c *Client) Close() {
	c.mu.Lock()
	defer c.mu.Unlock()

	for _, sub := range c.subs {
		if err := sub.Unsubscribe(); err != nil {
			c.logger.Error("failed to unsubscribe", zap.String("subject", sub.Subject), zap.Error(err))
		}
	}
	c.subs = nil
	c.conn.Close()
}

func (c *Client) trackSubscription(sub *nats.Subscription) {
	c.mu.Lock()
	defer c.mu.Unlock()
	c.subs = append(c.subs, sub)
}

func (c *Client) publish(subject string, v any) error {
	data, err := json.Marshal(v)
	if err != nil {
		return fmt.Errorf("nats: marshal error for subject %s: %w", subject, err)
	}
	return c.conn.Publish(subject, data)
}

func unmarshalAndHandle[T any](c *Client, data []byte, userID string, next func(string, *T)) {
	var p T
	if err := json.Unmarshal(data, &p); err != nil {
		c.logger.Error("failed to unmarshal payload",
			zap.Error(err),
			zap.String("target_type", fmt.Sprintf("%T", p)),
		)
		return
	}
	next(userID, &p)
}
