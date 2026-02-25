package config

import (
	"os"
	"testing"
)

func TestLoad(t *testing.T) {
	clearEnv := func() {
		envs := []string{
			"WS_GATEWAY_PORT", "NATS_URL", "JWKS_CACHE_TTL",
			"LOG_LEVEL", "CHAT_SERVICE_URL", "GATEWAY_PROFILE",
			"KC_JWKS_URL", "KC_JWT_ISSUER", "COGNITO_JWKS_URL",
			"COGNITO_JWT_ISSUER", "CORS_ALLOWED_ORIGINS",
		}
		for _, e := range envs {
			_ = os.Unsetenv(e) 
		}
	}

	t.Run("Successful Load Dev Profile", func(t *testing.T) {
		defer clearEnv()

		setEnv(t, "WS_GATEWAY_PORT", "8080")
		setEnv(t, "NATS_URL", "nats://localhost:4222")
		setEnv(t, "JWKS_CACHE_TTL", "3600")
		setEnv(t, "LOG_LEVEL", "debug")
		setEnv(t, "CHAT_SERVICE_URL", "http://chat:8081")
		setEnv(t, "GATEWAY_PROFILE", "dev")
		setEnv(t, "KC_JWKS_URL", "http://keycloak/jwks")
		setEnv(t, "KC_JWT_ISSUER", "my-issuer")

		cfg, err := Load()
		if err != nil {
			t.Fatalf("Expected no error, got %v", err)
		}

		if cfg.ServerPort != 8080 {
			t.Errorf("Expected port 8080, got %d", cfg.ServerPort)
		}
		if cfg.GetJwksURL() != "http://keycloak/jwks" {
			t.Errorf("Expected Keycloak URL, got %s", cfg.GetJwksURL())
		}
	})

	t.Run("Validation Error Prod Profile Missing Cognito", func(t *testing.T) {
		defer clearEnv()
		setEnv(t, "WS_GATEWAY_PORT", "8080")
		setEnv(t, "NATS_URL", "nats://localhost:4222")
		setEnv(t, "JWKS_CACHE_TTL", "3600")
		setEnv(t, "LOG_LEVEL", "info")
		setEnv(t, "CHAT_SERVICE_URL", "http://chat:8081")
		setEnv(t, "GATEWAY_PROFILE", "prod")

		_, err := Load()
		if err == nil {
			t.Fatal("Expected validation error for prod profile, got nil")
		}
	})

	t.Run("Validation Error Dev Profile Missing Keycloak", func(t *testing.T) {
		defer clearEnv()
		setEnv(t, "WS_GATEWAY_PORT", "8080")
		setEnv(t, "NATS_URL", "nats://localhost:4222")
		setEnv(t, "JWKS_CACHE_TTL", "3600")
		setEnv(t, "LOG_LEVEL", "info")
		setEnv(t, "CHAT_SERVICE_URL", "http://chat:8081")
		setEnv(t, "GATEWAY_PROFILE", "dev")

		_, err := Load()
		if err == nil {
			t.Fatal("Expected validation error for dev profile, got nil")
		}
	})
}

func setEnv(t *testing.T, key, value string) {
	err := os.Setenv(key, value)
	if err != nil {
		t.Fatalf("Failed to set env %s: %v", key, err)
	}
}

func TestConfigGetters(t *testing.T) {
	t.Run("Getter Logic for Dev", func(t *testing.T) {
		cfg := &Config{
			Profile:     "dev",
			KcJwksURL:   "http://dev-jwks",
			KcJwtIssuer: "dev-issuer",
			FrontendURL: "http://localhost:3000",
		}

		if cfg.GetJwksURL() != cfg.KcJwksURL {
			t.Error("GetJwksURL should return KC URL in dev")
		}
		if cfg.GetAllowedOrigin() != cfg.FrontendURL {
			t.Error("GetAllowedOrigin should return FrontendURL in dev")
		}
	})

	t.Run("Getter Logic for Prod", func(t *testing.T) {
		cfg := &Config{
			Profile:            "prod",
			CognitoJwksURL:     "http://prod-jwks",
			CorsAllowedOrigins: "https://example.com",
		}

		if cfg.GetJwksURL() != cfg.CognitoJwksURL {
			t.Error("GetJwksURL should return Cognito URL in prod")
		}
		if cfg.GetAllowedOrigin() != cfg.CorsAllowedOrigins {
			t.Error("GetAllowedOrigin should return CorsAllowedOrigins in prod")
		}
	})
}
