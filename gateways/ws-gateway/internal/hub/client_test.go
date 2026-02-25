package hub

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/gorilla/websocket"
	"github.com/joaosimsic/hermes/ws-gateway/internal/protocol"
	"go.uber.org/zap"
)

type MockPublisher struct{}

func (m *MockPublisher) PublishMessage(s string, msg *protocol.SendMessagePayload) error { return nil }
func (m *MockPublisher) PublishTyping(u, c string) error                                 { return nil }
func (m *MockPublisher) PublishMarkRead(u, c, mID string) error                          { return nil }
func (m *MockPublisher) PublishPresence(u, s string) error                               { return nil }
func (m *MockPublisher) PublishUserOnline(u string) error                                { return nil }

func TestClient_ReadPump(t *testing.T) {
	logger := zap.NewNop()
	h := NewHub(&MockPublisher{}, logger)

	s := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		upgrader := websocket.Upgrader{
			CheckOrigin: func(r *http.Request) bool { return true },
		}
		conn, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			return
		}

		client := NewClient(h, conn, "user_123", "test@example.com", logger)
		h.Register(client)

		go client.ReadPump()
	}))
	defer s.Close()

	u := "ws" + strings.TrimPrefix(s.URL, "http")
	ws, _, err := websocket.DefaultDialer.Dial(u, nil)
	if err != nil {
		t.Fatalf("Failed to dial: %v", err)
	}
	defer func() { _ = ws.Close() }()

	msg := protocol.Envelope{
		Type:    protocol.TypeSendMessage,
		Payload: json.RawMessage(`{"conversationId":"conv_1","content":"hello"}`),
	}
	data, _ := json.Marshal(msg)

	if err := ws.WriteMessage(websocket.TextMessage, data); err != nil {
		t.Fatalf("Failed to send message: %v", err)
	}

	time.Sleep(50 * time.Millisecond)
}

func TestClient_WritePump(t *testing.T) {
	logger := zap.NewNop()
	h := NewHub(&MockPublisher{}, logger)

	messageReceived := make(chan string, 1)

	s := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		upgrader := websocket.Upgrader{
			CheckOrigin: func(r *http.Request) bool { return true },
		}
		conn, _ := upgrader.Upgrade(w, r, nil)

		client := NewClient(h, conn, "user_123", "test@example.com", logger)

		go client.WritePump()

		client.Send([]byte("ping-from-server"))
	}))
	defer s.Close()

	u := "ws" + strings.TrimPrefix(s.URL, "http")
	ws, _, _ := websocket.DefaultDialer.Dial(u, nil)
	defer func() { _ = ws.Close() }()

	_, p, err := ws.ReadMessage()
	if err != nil {
		t.Fatalf("Failed to read: %v", err)
	}
	messageReceived <- string(p)

	if got := <-messageReceived; got != "ping-from-server" {
		t.Errorf("Expected ping-from-server, got %s", got)
	}
}
