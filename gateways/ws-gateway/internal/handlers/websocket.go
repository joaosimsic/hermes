package handlers

import (
	"net/http"
	"time"

	"github.com/gorilla/websocket"
	"github.com/joaosimsic/hermes/ws-gateway/internal/config"
	"github.com/joaosimsic/hermes/ws-gateway/internal/hub"
	"github.com/joaosimsic/hermes/ws-gateway/pkg/jwt"
	"go.uber.org/zap"
)

type WebSocketHandler struct {
	cfg       *config.Config
	hub       *hub.Hub
	validator *jwt.Validator
	logger    *zap.Logger
	upgrader  websocket.Upgrader
}

func NewWebSocketHandler(cfg *config.Config, h *hub.Hub, v *jwt.Validator, l *zap.Logger) *WebSocketHandler {
	return &WebSocketHandler{
		cfg:       cfg,
		hub:       h,
		validator: v,
		logger:    l,
		upgrader: websocket.Upgrader{
			ReadBufferSize:  1024,
			WriteBufferSize: 1024,
			CheckOrigin: func(r *http.Request) bool {
				if cfg.Profile == "dev" {
					return true
				}

				origin := r.Header.Get("Origin")

				return origin == cfg.AllowedOrigin
			},
		},
	}
}

func (h *WebSocketHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	token := h.extractToken(r)

	if token == "" {
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	claims, err := h.validator.Validate(r.Context(), token)
	if err != nil {
		h.logger.Warn("JWT validation failed", zap.Error(err))

		http.Error(w, "Unauthorized", http.StatusUnauthorized)

		return
	}

	conn, err := h.upgrader.Upgrade(w, r, nil)
	if err != nil {
		h.logger.Error("WebSocket upgrade failed", zap.Error(err))

		return
	}

	client := hub.NewClient(h.hub, conn, claims.Subject, claims.Email, h.logger)
	h.hub.Register(client)

	go client.WritePump()
	go client.ReadPump()
}

func (h *WebSocketHandler) extractToken(r *http.Request) string {
	if cookie, err := r.Cookie("access_token"); err == nil {
		return cookie.Value
	}

	return ""
}

type HealthHandler struct{}

func (h *HealthHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte(`{"status":"healthy","timestamp":"` + time.Now().Format(time.RFC3339) + `"}`))
}
