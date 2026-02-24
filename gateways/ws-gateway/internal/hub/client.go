package hub

import (
	"encoding/json"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"github.com/joaosimsic/hermes/ws-gateway/internal/protocol"
	"go.uber.org/zap"
)

const (
	writeWait      = 10 * time.Second
	pongWait       = 60 * time.Second
	pingPeriod     = (pongWait * 9) / 10
	maxMessageSize = 65536
)

type Client struct {
	hub       *Hub
	conn      *websocket.Conn
	send      chan []byte
	userID    string
	email     string
	logger    *zap.Logger
	mu        sync.Mutex
	closed    bool
	closeOnce sync.Once
}

func NewClient(hub *Hub, conn *websocket.Conn, userID, email string, logger *zap.Logger) *Client {
	return &Client{
		hub:    hub,
		conn:   conn,
		send:   make(chan []byte, 256),
		userID: userID,
		email:  email,
		logger: logger.With(zap.String("user_id", userID)),
	}
}

func (c *Client) UserID() string {
	return c.userID
}

func (c *Client) ReadPump() {
	defer c.Close()

	c.conn.SetReadLimit(maxMessageSize)

	_ = c.conn.SetReadDeadline(time.Now().Add(pongWait))

	c.conn.SetPongHandler(func(string) error {
		_ = c.conn.SetReadDeadline(time.Now().Add(pongWait))
		return nil
	})

	for {
		_, message, err := c.conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				c.logger.Warn("websocket read error", zap.Error(err))
			}

			break
		}

		var envelope protocol.Envelope
		if err := json.Unmarshal(message, &envelope); err != nil {
			c.sendError("INVALID_MESSAGE", "Invalid message format")
			continue
		}

		c.handleMessage(&envelope)
	}
}

func (c *Client) WritePump() {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		c.Close()
	}()

	for {
		select {
		case message, ok := <-c.send:
			_ = c.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if !ok {
				_ = c.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}

			w, err := c.conn.NextWriter(websocket.TextMessage)
			if err != nil {
				return
			}

			_, _ = w.Write(message)

			n := len(c.send)
			for range n {
				_, _ = w.Write([]byte{'\n'})
				_, _ = w.Write(<-c.send)
			}

			if err := w.Close(); err != nil {
				return
			}

		case <-ticker.C:
			_ = c.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if err := c.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

func (c *Client) Send(data []byte) {
	c.mu.Lock()

	defer c.mu.Unlock()

	if c.closed {
		return
	}

	select {
	case c.send <- data:
	default:
		c.logger.Warn("send buffer full, dropping message")
	}
}

func (c *Client) Close() {
	c.closeOnce.Do(func() {
		c.mu.Lock()

		if c.closed {
			c.mu.Unlock()
			return
		}

		c.closed = true
		c.mu.Unlock()

		_ = c.conn.Close()
		close(c.send)
		c.hub.Unregister(c)
	})
}

func (c *Client) handleMessage(envelope *protocol.Envelope) {
	switch envelope.Type {
	case protocol.TypeSendMessage:
		c.handleSendMessage(envelope.Payload)
	case protocol.TypeTyping:
		c.handleTyping(envelope.Payload)
	case protocol.TypeMarkRead:
		c.handleMarkRead(envelope.Payload)
	case protocol.TypePong:
		c.sendPong()
	default:
		c.sendError("UNKNOWN_TYPE", "Unknown message type")
	}
}

func (c *Client) handleSendMessage(payload json.RawMessage) {
	var msg protocol.SendMessagePayload

	if err := json.Unmarshal(payload, &msg); err != nil {
		c.sendError("INVALID_PAYLOAD", "Invalid send_message payload")
		return
	}

	c.hub.HandleSendMessage(c, &msg)
}

func (c *Client) handleTyping(payload json.RawMessage) {
	var msg protocol.TypingPayload

	if err := json.Unmarshal(payload, &msg); err != nil {
		c.sendError("INVALID_PAYLOAD", "Invalid typing payload")
		return
	}

	c.hub.HandleTyping(c, &msg)
}

func (c *Client) handleMarkRead(payload json.RawMessage) {
	var msg protocol.MarkReadPayload

	if err := json.Unmarshal(payload, &msg); err != nil {
		c.sendError("INVALID_PAYLOAD", "Invalid mark_read payload")
		return
	}

	c.hub.HandleMarkRead(c, &msg)
}

func (c *Client) sendPong() {
	env, err := protocol.NewEnvelope(protocol.TypePong, nil)
	if err != nil {
		c.logger.Error("failed to create pong envelope", zap.Error(err))
		return
	}

	data, err := env.Bytes()
	if err != nil {
		c.logger.Error("failed serialize pong envelope", zap.Error(err))
		return
	}

	c.Send(data)
}

func (c *Client) sendError(code, message string) {
	env, err := protocol.NewEnvelope(protocol.TypeError, protocol.ErrorPayload{
		Code:    code,
		Message: message,
	})
	if err != nil {
		c.logger.Error("failed to create error envelope", zap.Error(err))
		return
	}

	data, err := env.Bytes()
	if err != nil {
		c.logger.Error("failed to serialize error envelope", zap.Error(err))
		return
	}

	c.Send(data)
}
