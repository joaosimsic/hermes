package protocol

import (
	"encoding/json"
	"time"
)

type MessageType string

const (
	TypeSendMessage MessageType = "send_message"
	TypeMessage     MessageType = "message"
	TypeTyping      MessageType = "typing"
	TypeMarkRead    MessageType = "mark_read"
	TypeReadReceipt MessageType = "read_receipt"
	TypePresence    MessageType = "presence"
	TypeAck         MessageType = "ack"
	TypeError       MessageType = "error"
	TypePing        MessageType = "ping"
	TypePong        MessageType = "pong"
)

type Envelope struct {
	Type    MessageType     `json:"type"`
	Payload json.RawMessage `json:"payload,omitempty"`
}

type SendMessagePayload struct {
	ConversationID string `json:"conversationId"`
	Content        string `json:"content"`
	MediaID        string `json:"mediaId,omitempty"`
	ClientMsgID    string `json:"clientMsgId"`
}

type MessagePayload struct {
	ConversationID string    `json:"conversationId"`
	MessageID      string    `json:"messageId"`
	SenderID       string    `json:"senderId"`
	Content        string    `json:"content"`
	MediaID        string    `json:"mediaId,omitempty"`
	CreatedAt      time.Time `json:"createdAt"`
}

type TypingPayload struct {
	ConversationID string `json:"conversationId"`
	UserID         string `json:"userId,omitempty"`
}

type MarkReadPayload struct {
	ConversationID string `json:"conversationId"`
	MessageID      string `json:"messageId"`
}

type ReadReceiptPayload struct {
	ConversationID string `json:"conversationId"`
	UserID         string `json:"userId"`
	MessageID      string `json:"messageId"`
}

type PresencePayload struct {
	UserID string `json:"userId"`
	Status string `json:"status"`
}

type AckPayload struct {
	ClientMsgID string `json:"clientMsgId"`
	MessageID   string `json:"messageId"`
}

type ErrorPayload struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

func NewEnvelope(msgType MessageType, payload interface{}) (*Envelope, error) {
	data, err := json.Marshal(payload)
	if err != nil {
		return nil, err
	}
	return &Envelope{
		Type:    msgType,
		Payload: data,
	}, nil
}

func (e *Envelope) Bytes() ([]byte, error) {
	return json.Marshal(e)
}
