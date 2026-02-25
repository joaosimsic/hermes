package nats

import (
	"encoding/json"
	"testing"
	"time"

	"github.com/joaosimsic/hermes/ws-gateway/internal/protocol"
	"github.com/nats-io/nats-server/v2/server"
	natsserver "github.com/nats-io/nats-server/v2/test"
	"github.com/nats-io/nats.go"
	"go.uber.org/zap"
)

func runTestServer() *server.Server {
	opts := natsserver.DefaultTestOptions
	opts.Port = -1 // Use a random port
	return natsserver.RunServer(&opts)
}

func TestNATSClient_Integration(t *testing.T) {
	s := runTestServer()
	defer s.Shutdown()

	logger := zap.NewNop()
	client, err := NewClient(s.ClientURL(), logger)
	if err != nil {
		t.Fatalf("Failed to connect: %v", err)
	}
	defer client.Close()

	t.Run("PublishMessage", func(t *testing.T) {
		sub, err := client.conn.Subscribe(SubjectSendMessage, func(m *nats.Msg) {
			var p map[string]any
			json.Unmarshal(m.Data, &p)
			if p["content"] != "hello" {
				t.Errorf("expected content hello, got %v", p["content"])
			}
		})
		if err != nil {
			t.Fatal(err)
		}
		defer sub.Unsubscribe()

		payload := &protocol.SendMessagePayload{
			ConversationID: "conv_1",
			Content:        "hello",
			ClientMsgID:    "msg_1",
		}

		if err := client.PublishMessage("user_1", payload); err != nil {
			t.Errorf("PublishMessage failed: %v", err)
		}
	})

	t.Run("PresenceBroadcast", func(t *testing.T) {
		done := make(chan bool)
		mockHandler := &mockNatsHandler{
			onPresence: func(p *protocol.PresencePayload) {
				if p.UserID == "user_1" && p.Status == "online" {
					done <- true
				}
			},
		}

		client.SetHandler(mockHandler)
		if err := client.SubscribeToPresenceBroadcast(); err != nil {
			t.Fatal(err)
		}

		client.PublishPresence("user_1", "online")

		select {
		case <-done:
		case <-time.After(1 * time.Second):
			t.Error("Timed out waiting for presence broadcast")
		}
	})
}

type mockNatsHandler struct {
	onPresence func(*protocol.PresencePayload)
}

func (m *mockNatsHandler) OnMessageReceived(u string, p *protocol.MessagePayload)         {}
func (m *mockNatsHandler) OnTypingReceived(u string, p *protocol.TypingPayload)           {}
func (m *mockNatsHandler) OnReadReceiptReceived(u string, p *protocol.ReadReceiptPayload) {}
func (m *mockNatsHandler) OnAckReceived(u string, p *protocol.AckPayload)                 {}
func (m *mockNatsHandler) OnPresenceReceived(p *protocol.PresencePayload) {
	if m.onPresence != nil {
		m.onPresence(p)
	}
}
