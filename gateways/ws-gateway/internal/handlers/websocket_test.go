package handlers

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	jwtlib "github.com/golang-jwt/jwt/v5"
	"github.com/gorilla/websocket"
	"github.com/joaosimsic/hermes/ws-gateway/internal/config"
	"github.com/joaosimsic/hermes/ws-gateway/internal/hub"
	"github.com/joaosimsic/hermes/ws-gateway/internal/protocol"
	"github.com/joaosimsic/hermes/ws-gateway/pkg/jwt"
	"go.uber.org/zap"
)

type MockValidator struct {
	shouldSucceed bool
}

func (m *MockValidator) Validate(ctx context.Context, token string) (*jwt.Claims, error) {
	if !m.shouldSucceed || token == "" {
		return nil, errors.New("invalid token")
	}
	return &jwt.Claims{
		RegisteredClaims: jwtlib.RegisteredClaims{
			Subject: "user_123",
		},
		Email: "test@example.com",
	}, nil
}

func TestExtractToken(t *testing.T) {
	h := &WebSocketHandler{}

	tests := []struct {
		name     string
		setup    func(*http.Request)
		expected string
	}{
		{
			name: "Bearer Subprotocol",
			setup: func(r *http.Request) {
				r.Header.Set("Sec-WebSocket-Protocol", "bearer.token_abc")
			},
			expected: "token_abc",
		},
		{
			name: "Cookie Auth",
			setup: func(r *http.Request) {
				r.AddCookie(&http.Cookie{Name: "access_token", Value: "token_123"})
			},
			expected: "token_123",
		},
		{
			name: "Query Param",
			setup: func(r *http.Request) {
				r.URL.RawQuery = "token=token_xyz"
			},
			expected: "token_xyz",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			req := httptest.NewRequest("GET", "/", nil)
			tt.setup(req)
			if got := h.extractToken(req); got != tt.expected {
				t.Errorf("extractToken() = %v, want %v", got, tt.expected)
			}
		})
	}
}

type MockNats struct{}

func (m *MockNats) PublishMessage(s string, msg *protocol.SendMessagePayload) error { return nil }
func (m *MockNats) PublishTyping(u, c string) error                                 { return nil }
func (m *MockNats) PublishMarkRead(u, c, mID string) error                          { return nil }
func (m *MockNats) PublishPresence(u, s string) error                               { return nil }
func (m *MockNats) PublishUserOnline(u string) error                                { return nil }

func TestWebSocketHandler_ServeHTTP(t *testing.T) {
	logger := zap.NewNop()
	cfg := &config.Config{Profile: "dev"}

	myHub := hub.NewHub(&MockNats{}, logger)
	go myHub.Run()

	t.Run("Full Success Flow", func(t *testing.T) {
		mockV := &MockValidator{shouldSucceed: true}
		handler := NewWebSocketHandler(cfg, myHub, mockV, logger)

		server := httptest.NewServer(http.HandlerFunc(handler.ServeHTTP))
		defer server.Close()

		u := "ws" + strings.TrimPrefix(server.URL, "http")

		header := make(http.Header)
		header.Add("Sec-WebSocket-Protocol", "bearer.valid_token")

		conn, _, err := websocket.DefaultDialer.Dial(u, header)
		if err != nil {
			t.Fatalf("Failed to connect: %v", err)
		}
		defer func() { _ = conn.Close() }()

		success := false
		for range 10 {
			if myHub.IsOnline("user_123") {
				success = true
				break
			}
			time.Sleep(10 * time.Millisecond)
		}

		if !success {
			t.Error("User should be registered in Hub after successful connection")
		}
	})

	t.Run("Rejects Invalid Token", func(t *testing.T) {
		mockV := &MockValidator{shouldSucceed: false}
		handler := NewWebSocketHandler(cfg, myHub, mockV, logger)

		server := httptest.NewServer(http.HandlerFunc(handler.ServeHTTP))
		defer server.Close()

		u := "ws" + strings.TrimPrefix(server.URL, "http")
		_, resp, err := websocket.DefaultDialer.Dial(u, nil)

		if err == nil {
			t.Fatal("Expected dial to fail")
		}
		if resp.StatusCode != http.StatusUnauthorized {
			t.Errorf("Expected 401 Unauthorized, got %d", resp.StatusCode)
		}
	})
}
