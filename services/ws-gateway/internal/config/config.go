package config

import (
	"github.com/kelseyhightower/envconfig"
)

type Config struct {
	ServerPort     int    `envconfig:"SERVER_PORT" default:"8080"`
	NatsURL        string `envconfig:"NATS_URL" default:"nats://localhost:4222"`
	JwksURL        string `envconfig:"JWKS_URL" required:"true"`
	JwtIssuer      string `envconfig:"JWT_ISSUER" required:"true"`
	JwksCacheTTL   int    `envconfig:"JWKS_CACHE_TTL" default:"3600"`
	LogLevel       string `envconfig:"LOG_LEVEL" default:"info"`
	ChatServiceURL string `envconfig:"CHAT_SERVICE_URL" default:"http://chat-service:8080"`
}

func Load() (*Config, error) {
	var cfg Config
	if err := envconfig.Process("", &cfg); err != nil {
		return nil, err
	}
	return &cfg, nil
}
