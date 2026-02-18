#!/bin/bash

# Hermes Kubernetes Secrets Setup Script
# This script helps you set up all required secrets.env files

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "Hermes K8s Secrets Setup"
echo "=========================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Define all secrets.env file locations
SECRETS_FILES=(
    # Services
    "services/gateway/overlays/dev/secrets.env"
    "services/gateway/overlays/staging/secrets.env"
    "services/gateway/overlays/prod/secrets.env"
    "services/auth-service/overlays/dev/secrets.env"
    "services/auth-service/overlays/staging/secrets.env"
    "services/auth-service/overlays/prod/secrets.env"
    "services/user-service/overlays/dev/secrets.env"
    "services/user-service/overlays/staging/secrets.env"
    "services/user-service/overlays/prod/secrets.env"
    # Shared infrastructure
    "shared/postgres/auth-db/overlays/dev/secrets.env"
    "shared/postgres/auth-db/overlays/staging/secrets.env"
    "shared/postgres/auth-db/overlays/prod/secrets.env"
    "shared/postgres/user-db/overlays/dev/secrets.env"
    "shared/postgres/user-db/overlays/staging/secrets.env"
    "shared/postgres/user-db/overlays/prod/secrets.env"
    "shared/rabbitmq/overlays/dev/secrets.env"
    "shared/rabbitmq/overlays/staging/secrets.env"
    "shared/rabbitmq/overlays/prod/secrets.env"
    "shared/keycloak/overlays/dev/secrets.env"
    "shared/keycloak/overlays/staging/secrets.env"
    "shared/keycloak/overlays/prod/secrets.env"
)

TOTAL=${#SECRETS_FILES[@]}
EXISTING=0
MISSING=0

echo "Checking for secrets.env files..."
echo ""

# Check which files exist
for secret_file in "${SECRETS_FILES[@]}"; do
    if [ -f "$secret_file" ]; then
        echo -e "${GREEN}✓${NC} $secret_file (exists)"
        EXISTING=$((EXISTING + 1))
    else
        echo -e "${YELLOW}✗${NC} $secret_file (missing)"
        MISSING=$((MISSING + 1))
    fi
done

echo ""
echo "Summary:"
echo "  Total: $TOTAL"
echo "  Existing: $EXISTING"
echo "  Missing: $MISSING"
echo ""

if [ $MISSING -eq 0 ]; then
    echo -e "${GREEN}All secrets.env files already exist!${NC}"
    echo ""
    echo "To update secrets, edit the files directly or delete them and run this script again."
    exit 0
fi

echo -e "${BLUE}This script will create placeholder secrets.env files.${NC}"
echo -e "${BLUE}Refer to secrets.env.example for the actual values you need to fill in.${NC}"
echo ""
read -p "Create missing files with placeholders? (y/N): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Setup cancelled. No files were created."
    exit 0
fi

echo ""
echo "Creating missing secrets.env files..."
echo ""

CREATED=0

# Create secrets files with appropriate placeholders
create_gateway_secrets() {
    local file=$1
    cat > "$file" << 'EOF'
# Gateway Secrets
# See infrastructure/k8s/secrets.env.example for detailed documentation

GATEWAY_SECRET=change-me-gateway-secret-MUST-BE-LONG-32-CHARS-MIN
EOF
}

create_auth_service_secrets() {
    local file=$1
    cat > "$file" << 'EOF'
# Auth Service Secrets
# See infrastructure/k8s/secrets.env.example for detailed documentation

SPRING_DATASOURCE_PASSWORD=change-me-app-password
FLYWAY_PASSWORD=change-me-flyway-password
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=change-me-keycloak-admin-password
SPRING_RABBITMQ_USERNAME=admin
SPRING_RABBITMQ_PASSWORD=change-me-rabbitmq-password
EOF
}

create_user_service_secrets() {
    local file=$1
    cat > "$file" << 'EOF'
# User Service Secrets
# See infrastructure/k8s/secrets.env.example for detailed documentation

SPRING_DATASOURCE_PASSWORD=change-me-app-password
FLYWAY_PASSWORD=change-me-flyway-password
SPRING_RABBITMQ_USERNAME=admin
SPRING_RABBITMQ_PASSWORD=change-me-rabbitmq-password
SPRING_CLOUD_AWS_CREDENTIALS_ACCESS_KEY=your-aws-access-key-id
SPRING_CLOUD_AWS_CREDENTIALS_SECRET_KEY=your-aws-secret-access-key
EOF
}

create_postgres_secrets() {
    local file=$1
    cat > "$file" << 'EOF'
# PostgreSQL Secrets
# See infrastructure/k8s/secrets.env.example for detailed documentation

POSTGRES_PASSWORD=change-me-postgres-admin-password
FLYWAY_PASSWORD=change-me-flyway-password
APP_PASSWORD=change-me-app-password
EOF
}

create_rabbitmq_secrets() {
    local file=$1
    cat > "$file" << 'EOF'
# RabbitMQ Secrets
# See infrastructure/k8s/secrets.env.example for detailed documentation

RABBITMQ_DEFAULT_USER=admin
RABBITMQ_DEFAULT_PASS=change-me-rabbitmq-password
EOF
}

create_keycloak_secrets() {
    local file=$1
    cat > "$file" << 'EOF'
# Keycloak Secrets
# See infrastructure/k8s/secrets.env.example for detailed documentation

KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=change-me-keycloak-admin-password
GITHUB_CLIENT_ID=your-github-oauth-app-client-id
GITHUB_CLIENT_SECRET=your-github-oauth-app-client-secret
EOF
}

# Create missing files
for secret_file in "${SECRETS_FILES[@]}"; do
    if [ ! -f "$secret_file" ]; then
        # Ensure directory exists
        mkdir -p "$(dirname "$secret_file")"
        
        # Create appropriate file based on path
        if [[ $secret_file =~ gateway ]]; then
            create_gateway_secrets "$secret_file"
        elif [[ $secret_file =~ auth-service ]]; then
            create_auth_service_secrets "$secret_file"
        elif [[ $secret_file =~ user-service ]]; then
            create_user_service_secrets "$secret_file"
        elif [[ $secret_file =~ postgres ]]; then
            create_postgres_secrets "$secret_file"
        elif [[ $secret_file =~ rabbitmq ]]; then
            create_rabbitmq_secrets "$secret_file"
        elif [[ $secret_file =~ keycloak ]]; then
            create_keycloak_secrets "$secret_file"
        fi
        
        echo -e "${GREEN}✓${NC} Created: $secret_file"
        CREATED=$((CREATED + 1))
    fi
done

echo ""
echo -e "${GREEN}Created $CREATED secrets.env files!${NC}"
echo ""
echo "=========================================="
echo "NEXT STEPS"
echo "=========================================="
echo ""
echo "1. Review the master secrets reference:"
echo "   cat infrastructure/k8s/secrets.env.example"
echo ""
echo "2. Edit each secrets.env file with actual values"
echo ""
echo "3. IMPORTANT: Never commit secrets.env files to git!"
echo "   (They are gitignored by default)"
echo ""
echo "4. For production, use strong passwords (32+ characters)"
echo "   Generate with: openssl rand -base64 32"
echo ""
echo "5. After configuring secrets, deploy with:"
echo "   kubectl apply -k clusters/dev"
echo ""
echo "=========================================="
