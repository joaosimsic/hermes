package config

import (
	"github.com/kelseyhightower/envconfig"
)

type Config struct {
	ServerPort     int    `envconfig:"GATEWAY_PORT" required:"true"`
	NatsURL        string `envconfig:"NATS_URL" required:"true"`
	JwksURL        string `envconfig:"JWKS_URL" required:"true"`
	JwtIssuer      string `envconfig:"JWT_ISSUER" required:"true"`
	JwksCacheTTL   int    `envconfig:"JWKS_CACHE_TTL" required:"true"`
	LogLevel       string `envconfig:"LOG_LEVEL" required:"true"`
	ChatServiceURL string `envconfig:"CHAT_SERVICE_URL" required:"true"`
	AllowedOrigin  string `envconfig:"FRONTEND_URL" required:"true"`
	Profile        string `envconfig:"GATEWAY_PROFILE" required:"true"`
}

func Load() (*Config, error) {
	var cfg Config

	if err := envconfig.Process("", &cfg); err != nil {
		return nil, err
	}

	return &cfg, nil
}
