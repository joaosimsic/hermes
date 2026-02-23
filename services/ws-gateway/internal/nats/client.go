package nats

import (
	"encoding/json"
	"fmt"

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
	handler MessageHandler
	subs    []*nats.Subscription
}

func NewClient(url string, logger *zap.Logger) (*Client, error) {
	nc, err := nats.Connect(url,
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
	)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to NATS: %w", err)
	}

	return &Client{
		conn:   nc,
		logger: logger,
	}, nil
}

func (c *Client) SetHandler(handler MessageHandler) {
	c.handler = handler
}

func (c *Client) SubscribeToUserMessages(userID string) error {
	subject := fmt.Sprintf(SubjectChatUser, userID)
	sub, err := c.conn.Subscribe(subject, func(msg *nats.Msg) {
		c.handleUserMessage(userID, msg)
	})
	if err != nil {
		return err
	}
	c.subs = append(c.subs, sub)
	return nil
}

func (c *Client) SubscribeToPresenceBroadcast() error {
	sub, err := c.conn.Subscribe(SubjectPresenceBroadcast, func(msg *nats.Msg) {
		var payload protocol.PresencePayload
		if err := json.Unmarshal(msg.Data, &payload); err != nil {
			return
		}
		if c.handler != nil {
			c.handler.OnPresenceReceived(&payload)
		}
	})
	if err != nil {
		return err
	}
	c.subs = append(c.subs, sub)
	return nil
}

func (c *Client) handleUserMessage(userID string, msg *nats.Msg) {
	var envelope protocol.Envelope
	if err := json.Unmarshal(msg.Data, &envelope); err != nil {
		c.logger.Warn("failed to unmarshal NATS message", zap.Error(err))
		return
	}

	if c.handler == nil {
		return
	}

	switch envelope.Type {
	case protocol.TypeMessage:
		var payload protocol.MessagePayload
		if err := json.Unmarshal(envelope.Payload, &payload); err != nil {
			return
		}
		c.handler.OnMessageReceived(userID, &payload)

	case protocol.TypeTyping:
		var payload protocol.TypingPayload
		if err := json.Unmarshal(envelope.Payload, &payload); err != nil {
			return
		}
		c.handler.OnTypingReceived(userID, &payload)

	case protocol.TypeReadReceipt:
		var payload protocol.ReadReceiptPayload
		if err := json.Unmarshal(envelope.Payload, &payload); err != nil {
			return
		}
		c.handler.OnReadReceiptReceived(userID, &payload)

	case protocol.TypeAck:
		var payload protocol.AckPayload
		if err := json.Unmarshal(envelope.Payload, &payload); err != nil {
			return
		}
		c.handler.OnAckReceived(userID, &payload)
	}
}

func (c *Client) PublishMessage(senderID string, msg *protocol.SendMessagePayload) error {
	data, err := json.Marshal(map[string]interface{}{
		"senderId":       senderID,
		"conversationId": msg.ConversationID,
		"content":        msg.Content,
		"mediaId":        msg.MediaID,
		"clientMsgId":    msg.ClientMsgID,
	})
	if err != nil {
		return err
	}
	return c.conn.Publish(SubjectSendMessage, data)
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
	data, _ := env.Bytes()
	return c.conn.Publish(subject, data)
}

func (c *Client) PublishMarkRead(userID, conversationID, messageID string) error {
	data, err := json.Marshal(map[string]string{
		"userId":         userID,
		"conversationId": conversationID,
		"messageId":      messageID,
	})
	if err != nil {
		return err
	}
	return c.conn.Publish(SubjectMarkRead, data)
}

func (c *Client) PublishPresence(userID, status string) error {
	data, err := json.Marshal(protocol.PresencePayload{
		UserID: userID,
		Status: status,
	})
	if err != nil {
		return err
	}
	return c.conn.Publish(SubjectPresenceBroadcast, data)
}

func (c *Client) PublishUserOnline(userID string) error {
	data, err := json.Marshal(map[string]string{
		"userId": userID,
	})
	if err != nil {
		return err
	}
	return c.conn.Publish(SubjectUserOnline, data)
}

func (c *Client) Close() {
	for _, sub := range c.subs {
		sub.Unsubscribe()
	}
	c.conn.Close()
}
