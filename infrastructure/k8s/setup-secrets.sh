#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "=========================================="
echo "Hermes K8s Secrets Setup: Force Refresh"
echo "=========================================="

if [ -f "../../.env" ]; then
    echo -e "${BLUE}Loading variables from root .env file...${NC}"
    set -a
    source "../../.env"
    set +a
fi

POSTGRES_PASS=${POSTGRES_PASSWORD}
FLYWAY_PASS=${FLYWAY_PASSWORD}
APP_PASS=${APP_PASSWORD}
RABBIT_PASS=${RABBITMQ_PASSWORD}
RABBIT_USER=${RABBITMQ_USERNAME}
KC_ADMIN_PASS=${KEYCLOAK_ADMIN_PASSWORD}
KC_ADMIN_USER=${KEYCLOAK_ADMIN_USERNAME}

SECRETS_FILES=(
    "services/gateway/overlays/dev/secrets.env"
    "services/auth-service/overlays/dev/secrets.env"
    "services/user-service/overlays/dev/secrets.env"
    "shared/postgres/auth-db/overlays/dev/secrets.env"
    "shared/postgres/user-db/overlays/dev/secrets.env"
    "shared/rabbitmq/overlays/dev/secrets.env"
    "shared/keycloak/overlays/dev/secrets.env"
)

create_gateway_secrets() {
    cat > "$1" << EOF
GATEWAY_SECRET=${GATEWAY_SECRET:-"$(openssl rand -base64 32)"}
RABBITMQ_USERNAME=$RABBIT_USER
RABBITMQ_PASSWORD=$RABBIT_PASS
EOF
}

create_auth_service_secrets() {
    cat > "$1" << EOF
SPRING_DATASOURCE_PASSWORD=$APP_PASS
SPRING_FLYWAY_PASSWORD=$FLYWAY_PASS
KEYCLOAK_ADMIN_USERNAME=$KC_ADMIN_USER
KEYCLOAK_ADMIN_PASSWORD=$KC_ADMIN_PASS
SPRING_RABBITMQ_USERNAME=$RABBIT_USER
SPRING_RABBITMQ_PASSWORD=$RABBIT_PASS
EOF
}

create_user_service_secrets() {
    cat > "$1" << EOF
SPRING_DATASOURCE_PASSWORD=$APP_PASS
SPRING_FLYWAY_PASSWORD=$FLYWAY_PASS
SPRING_RABBITMQ_USERNAME=$RABBIT_USER
SPRING_RABBITMQ_PASSWORD=$RABBIT_PASS
SPRING_CLOUD_AWS_CREDENTIALS_ACCESS_KEY=${AWS_ACCESS_KEY_ID:-"your-access-key-id"}
SPRING_CLOUD_AWS_CREDENTIALS_SECRET_KEY=${AWS_SECRET_ACCESS_KEY:-"your-secret-access-key"}
EOF
}

create_postgres_secrets() {
    cat > "$1" << EOF
POSTGRES_PASSWORD=$POSTGRES_PASS
FLYWAY_PASSWORD=$FLYWAY_PASS
APP_PASSWORD=$APP_PASS
EOF
}

create_rabbitmq_secrets() {
    cat > "$1" << EOF
RABBITMQ_USERNAME=$RABBIT_USER
RABBITMQ_PASSWORD=$RABBIT_PASS
EOF
}

create_keycloak_secrets() {
    cat > "$1" << EOF
KEYCLOAK_ADMIN=$KC_ADMIN_USER
KEYCLOAK_ADMIN_PASSWORD=$KC_ADMIN_PASS
GITHUB_CLIENT_ID=${GITHUB_CLIENT_ID:-"some-id"}
GITHUB_CLIENT_SECRET=${GITHUB_CLIENT_SECRET:-"some-secret"}
EOF
}

for secret_file in "${SECRETS_FILES[@]}"; do
    mkdir -p "$(dirname "$secret_file")"
    
    rm -f "$secret_file" 
    
    if [[ $secret_file =~ gateway ]]; then create_gateway_secrets "$secret_file"
    elif [[ $secret_file =~ auth-service ]]; then create_auth_service_secrets "$secret_file"
    elif [[ $secret_file =~ user-service ]]; then create_user_service_secrets "$secret_file"
    elif [[ $secret_file =~ postgres ]]; then create_postgres_secrets "$secret_file"
    elif [[ $secret_file =~ rabbitmq ]]; then create_rabbitmq_secrets "$secret_file"
    elif [[ $secret_file =~ keycloak ]]; then create_keycloak_secrets "$secret_file"
    fi
    
    echo -e "${GREEN}✓ Generated:${NC} $secret_file"
done

echo -e "\n${BLUE}Secrets synchronized with .env variables.${NC}"
