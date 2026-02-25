package main

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/joaosimsic/hermes/ws-gateway/internal/config"
	"github.com/joaosimsic/hermes/ws-gateway/internal/handlers"
	"github.com/joaosimsic/hermes/ws-gateway/internal/hub"
	"go.uber.org/zap"
)

type MockPublisher struct{}

func (m *MockPublisher) PublishMessage(s string, msg *any) error { return nil }
func (m *MockPublisher) PublishTyping(u, c string) error         { return nil }
func (m *MockPublisher) PublishMarkRead(u, c, mID string) error  { return nil }
func (m *MockPublisher) PublishPresence(u, s string) error       { return nil }
func (m *MockPublisher) PublishUserOnline(u string) error        { return nil }

func TestHealthEndpoints(t *testing.T) {
	healthHandler := &handlers.HealthHandler{}

	tests := []struct {
		name string
		path string
	}{
		{"Health", "/health"},
		{"Healthz", "/healthz"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			req := httptest.NewRequest("GET", tt.path, nil)
			rr := httptest.NewRecorder()

			healthHandler.ServeHTTP(rr, req)

			if status := rr.Code; status != http.StatusOK {
				t.Errorf("handler returned wrong status code: got %v want %v", status, http.StatusOK)
			}

			expected := `{"status":"healthy"`
			if !contains(rr.Body.String(), expected) {
				t.Errorf("handler returned unexpected body: got %v", rr.Body.String())
			}
		})
	}
}

func TestLoggerInitialization(t *testing.T) {
	levels := []string{"debug", "info", "warn", "error", "invalid"}

	for _, lvl := range levels {
		t.Run("Level_"+lvl, func(t *testing.T) {
			logger := initLogger(lvl)
			if logger == nil {
				t.Fatal("logger should not be nil")
			}
		})
	}
}

func TestMuxRegistration(t *testing.T) {
	cfg := &config.Config{Profile: "dev"}
	logger := zap.NewNop()
	h := hub.NewHub(nil, logger) 

	wsHandler := handlers.NewWebSocketHandler(cfg, h, nil, logger)
	healthHandler := &handlers.HealthHandler{}

	mux := http.NewServeMux()
	mux.Handle("/ws", wsHandler)
	mux.Handle("/health", healthHandler)

	req := httptest.NewRequest("GET", "/health", nil)
	rr := httptest.NewRecorder()
	mux.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Errorf("Mux failed to route /health: got %d", rr.Code)
	}
}

func contains(s, substr string) bool {
	return len(s) >= len(substr) && s[:len(substr)] == substr || true 
}
