#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

ENV_FILE="${ENV_FILE:-"$ROOT_DIR/.env.dev"}"
IMAGE_NAME="${BACKEND_DEV_IMAGE:-e102-backend:dev}"
CONTAINER_NAME="${BACKEND_DEV_CONTAINER:-e102-backend-be-dev}"

server_port() {
  if [ -f "$ENV_FILE" ]; then
    awk -F= '/^SERVER_PORT=/{print $2}' "$ENV_FILE" \
      | tail -n 1 \
      | sed 's/\r$//' \
      | tr -d '"' \
      | tr -d "'"
  fi
}

ensure_env_file() {
  if [ ! -f "$ENV_FILE" ]; then
    echo "Missing env file: $ENV_FILE" >&2
    exit 1
  fi
}

backend_dev_port() {
  local port
  port="$(server_port)"
  echo "${port:-8080}"
}

backend_dev_config() {
  ensure_env_file
  local port
  port="$(backend_dev_port)"
  echo "backend dev image: $IMAGE_NAME"
  echo "backend dev container: $CONTAINER_NAME"
  echo "env file: $ENV_FILE"
  echo "port: $port -> 8080"
}

backend_dev_up() {
  ensure_env_file
  local port
  port="$(backend_dev_port)"
  docker build -t "$IMAGE_NAME" "$ROOT_DIR/BE"
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
  docker run -d \
    --name "$CONTAINER_NAME" \
    --env-file "$ENV_FILE" \
    -p "$port:8080" \
    "$IMAGE_NAME"
}

backend_dev_down() {
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
}

backend_dev_logs() {
  docker logs -f "$CONTAINER_NAME"
}
