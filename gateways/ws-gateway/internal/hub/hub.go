package hub

import (
	"sync"

	"github.com/joaosimsic/hermes/ws-gateway/internal/protocol"
	"go.uber.org/zap"
)

type Publisher interface {
	PublishMessage(senderID string, msg *protocol.SendMessagePayload) error
	PublishTyping(userID, conversationID string) error
	PublishMarkRead(userID, conversationID, messageID string) error
	PublishPresence(userID, status string) error
	PublishUserOnline(userID string) error
}

type Hub struct {
	clients    map[string]*Client
	register   chan *Client
	unregister chan *Client
	stop       chan struct{}
	nats       Publisher
	logger     *zap.Logger
	mu         sync.RWMutex
}

func NewHub(natsClient Publisher, logger *zap.Logger) *Hub {
	return &Hub{
		clients:    make(map[string]*Client),
		register:   make(chan *Client),
		unregister: make(chan *Client),
		stop:       make(chan struct{}),
		nats:       natsClient,
		logger:     logger,
	}
}

func (h *Hub) Run() {
	for {
		select {
		case <-h.stop:
			return
		case client := <-h.register:
			h.mu.Lock()
			if existing, ok := h.clients[client.userID]; ok {
				existing.Close()
			}
			h.clients[client.userID] = client
			h.mu.Unlock()

			h.logger.Info("client registered", zap.String("user_id", client.userID))
			h.publishPresence(client.userID, "online")
			h.notifyUserOnline(client.userID)

		case client := <-h.unregister:
			h.mu.Lock()
			if existing, ok := h.clients[client.userID]; ok && existing == client {
				delete(h.clients, client.userID)
				h.mu.Unlock()

				h.logger.Info("client unregistered", zap.String("user_id", client.userID))
				h.publishPresence(client.userID, "offline")
			} else {
				h.mu.Unlock()
			}
		}
	}
}

func (h *Hub) Register(client *Client) {
	h.register <- client
}

func (h *Hub) Unregister(client *Client) {
	h.unregister <- client
}

func (h *Hub) Stop() {
	close(h.stop)
}

func (h *Hub) GetClient(userID string) *Client {
	h.mu.RLock()
	defer h.mu.RUnlock()
	return h.clients[userID]
}

func (h *Hub) IsOnline(userID string) bool {
	h.mu.RLock()
	defer h.mu.RUnlock()
	_, ok := h.clients[userID]
	return ok
}

func (h *Hub) SendToUser(userID string, data []byte) bool {
	h.mu.RLock()
	client, ok := h.clients[userID]
	h.mu.RUnlock()

	if !ok {
		return false
	}

	client.Send(data)
	return true
}

func (h *Hub) HandleSendMessage(client *Client, msg *protocol.SendMessagePayload) {
	_ = h.nats.PublishMessage(client.userID, msg)
}

func (h *Hub) HandleTyping(client *Client, msg *protocol.TypingPayload) {
	_ = h.nats.PublishTyping(client.userID, msg.ConversationID)
}

func (h *Hub) HandleMarkRead(client *Client, msg *protocol.MarkReadPayload) {
	_ = h.nats.PublishMarkRead(client.userID, msg.ConversationID, msg.MessageID)
}

func (h *Hub) publishPresence(userID, status string) {
	_ = h.nats.PublishPresence(userID, status)
}

func (h *Hub) notifyUserOnline(userID string) {
	_ = h.nats.PublishUserOnline(userID)
}

func (h *Hub) OnMessageReceived(targetUserID string, msg *protocol.MessagePayload) {
	env, err := protocol.NewEnvelope(protocol.TypeMessage, msg)
	if err != nil {
		h.logger.Error("failed to create message envelope", zap.Error(err))
		return
	}

	data, err := env.Bytes()
	if err != nil {
		h.logger.Error("failed to serialize message", zap.Error(err))
		return
	}

	h.SendToUser(targetUserID, data)
}

func (h *Hub) OnTypingReceived(targetUserID string, payload *protocol.TypingPayload) {
	env, err := protocol.NewEnvelope(protocol.TypeTyping, payload)
	if err != nil {
		h.logger.Error("failed to serialize typing envelope", zap.Error(err))
		return
	}

	data, err := env.Bytes()
	if err != nil {
		h.logger.Error("failed to serialize typing envelope", zap.Error(err))
		return
	}

	h.SendToUser(targetUserID, data)
}

func (h *Hub) OnReadReceiptReceived(targetUserID string, payload *protocol.ReadReceiptPayload) {
	env, err := protocol.NewEnvelope(protocol.TypeReadReceipt, payload)
	if err != nil {
		h.logger.Error("failed to serialize read receipt envelope", zap.Error(err))
		return
	}

	data, err := env.Bytes()
	if err != nil {
		h.logger.Error("failed to serialize read receipt envelope", zap.Error(err))
		return
	}

	h.SendToUser(targetUserID, data)
}

func (h *Hub) OnPresenceReceived(payload *protocol.PresencePayload) {
	env, err := protocol.NewEnvelope(protocol.TypePresence, payload)
	if err != nil {
		h.logger.Error("failed to create presence envelope", zap.Error(err))
		return
	}

	data, err := env.Bytes()
	if err != nil {
		h.logger.Error("failed to serialize presence envelope", zap.Error(err))
		return
	}

	h.mu.RLock()

	for _, client := range h.clients {
		client.Send(data)
	}

	h.mu.RUnlock()
}

func (h *Hub) OnAckReceived(targetUserID string, payload *protocol.AckPayload) {
	env, err := protocol.NewEnvelope(protocol.TypeAck, payload)
	if err != nil {
		h.logger.Error("failed to create ack envelope", zap.Error(err))
		return
	}

	data, err := env.Bytes()
	if err != nil {
		h.logger.Error("failed to serialize ack envelope", zap.Error(err))
		return
	}

	h.SendToUser(targetUserID, data)
}
