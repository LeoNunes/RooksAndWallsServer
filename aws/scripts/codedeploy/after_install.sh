#!/bin/bash
set -euo pipefail

ENV_FILE=/etc/games/service.env

# Read secret name from env file if it exists; fall back to well-known default
GAMES_SECRET_NAME=$(grep -E '^GAMES_SECRET_NAME=' "$ENV_FILE" | cut -d= -f2- || echo "games/beta/secrets")
: "${GAMES_SECRET_NAME:=games/beta/secrets}"

# Fetch secret JSON from Secrets Manager
SECRET_JSON=$(aws secretsmanager get-secret-value \
  --secret-id "$GAMES_SECRET_NAME" \
  --query SecretString \
  --output text)

# Replace env file content entirely with secret-derived values
echo "$SECRET_JSON" | jq -r 'to_entries[] | "\(.key)=\(.value)"' > "$ENV_FILE"

# Secure the env file: only root can read it
chown root:root "$ENV_FILE"
chmod 600 "$ENV_FILE"