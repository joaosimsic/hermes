#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ENV=${1:-dev}
ALLOWED_ENVS=("dev" "staging" "prod")

if [[ ! " ${ALLOWED_ENVS[@]} " =~ " ${ENV} " ]]; then
    echo -e "${RED}Error: Invalid environment '$ENV'. Use dev, staging, or prod.${NC}"
    exit 1
fi

echo "=========================================="
echo -e "Hermes K8s Setup: ${BLUE}${ENV^^}${NC}"
echo "=========================================="

ENV_FILE="../../.env"

[ -f "../../.env.${ENV}" ] && ENV_FILE="../../.env.${ENV}"

if [ -f "$ENV_FILE" ]; then
    echo -e "${BLUE}Loading variables from $ENV_FILE...${NC}"
    set -a
    source "$ENV_FILE"
    set +a
else
    echo -e "${RED}Error: Environment file $ENV_FILE not found.${NC}"
    exit 1
fi

generate_files() {
    local target_dir=$1
    shift
    local vars=("$@")
    
    mkdir -p "$target_dir" || { echo -e "${RED}Error: Cannot create directory $target_dir${NC}"; exit 1; }
    
    > "$target_dir/secrets.env"
    > "$target_dir/params.env"
    
    echo -ne "${YELLOW}Processing $(basename "$(dirname "$(dirname "$target_dir")")")... ${NC}"

    for var in "${vars[@]}"; do
        if [[ -z "${!var:-}" ]]; then
            echo -e "\n${RED}FAILED: Variable '$var' is missing or empty in $ENV_FILE${NC}"
            exit 1
        fi

        if [[ "$var" =~ (^USERNAME|_USER|PASSWORD|PASS|SECRET|KEY|TOKEN) ]]; then
            echo "$var=${!var}" >> "$target_dir/secrets.env"
        else
            echo "$var=${!var}" >> "$target_dir/params.env"
        fi
    done

    if [[ -f "$target_dir/secrets.env" && -f "$target_dir/params.env" ]]; then
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${RED}FAILED: Files were not generated in $target_dir${NC}"
        exit 1
    fi
}

echo -e "Validating and generating manifests..."

GATEWAY_VARS=(
    "GATEWAY_PROFILE" "GATEWAY_PORT" "GATEWAY_SECRET" "GATEWAY_CACHE_HOST" 
    "GATEWAY_CACHE_PORT" "RATE_LIMIT_AUTHENTICATED" "RATE_LIMIT_AUTHENTICATED_BURST"
    "RATE_LIMIT_UNAUTHENTICATED" "RATE_LIMIT_UNAUTHENTICATED_BURST" "CACHE_TTL_SECONDS"
    "RABBITMQ_USERNAME" "RABBITMQ_PASSWORD"
)
generate_files "services/gateway/overlays/$ENV" "${GATEWAY_VARS[@]}"

AUTH_VARS=(
    "AUTH_PROFILE" "AUTH_SERVICE_HOST" "AUTH_SERVICE_PORT" "AUTH_COOKIE_DOMAIN"
    "AUTH_COOKIE_SECURE" "GITHUB_REDIRECT_URI" "AUTH_DB_HOST" "AUTH_DB_PORT"
    "AUTH_CACHE_HOST" "AUTH_CACHE_PORT" "AUTH_ACCESS_TOKEN_MAX_AGE" 
    "AUTH_REFRESH_TOKEN_MAX_AGE" "APP_PASSWORD" "FLYWAY_PASSWORD"
    "RABBITMQ_USERNAME" "RABBITMQ_PASSWORD" "GITHUB_CLIENT_ID" "GITHUB_CLIENT_SECRET"
)
generate_files "services/auth-service/overlays/$ENV" "${AUTH_VARS[@]}"

USER_VARS=(
    "USER_PROFILE" "USER_SERVICE_HOST" "USER_SERVICE_PORT" "USER_DB_HOST"
    "USER_DB_PORT" "USER_CACHE_HOST" "USER_CACHE_PORT" "APP_USER" "APP_PASSWORD"
    "FLYWAY_USER" "FLYWAY_PASSWORD" "AWS_ACCESS_KEY_ID" "AWS_SECRET_ACCESS_KEY"
)
generate_files "services/user-service/overlays/$ENV" "${USER_VARS[@]}"

DB_VARS=("POSTGRES_DB" "POSTGRES_USER" "POSTGRES_PASSWORD" "POSTGRES_PORT")
generate_files "shared/postgres/auth-db/overlays/$ENV" "${DB_VARS[@]}"
generate_files "shared/postgres/user-db/overlays/$ENV" "${DB_VARS[@]}"

MQ_VARS=("RABBITMQ_HOST" "RABBITMQ_PORT" "RABBITMQ_USERNAME" "RABBITMQ_PASSWORD")
generate_files "shared/rabbitmq/overlays/$ENV" "${MQ_VARS[@]}"

KC_VARS=(
    "KEYCLOAK_ADMIN_USERNAME" "KEYCLOAK_ADMIN_PASSWORD" 
    "GITHUB_CLIENT_ID" "GITHUB_CLIENT_SECRET" "POSTGRES_DB"
)
generate_files "shared/keycloak/overlays/$ENV" "${KC_VARS[@]}"

echo -e "\n${GREEN}All files synchronized with .env and validated.${NC}"
