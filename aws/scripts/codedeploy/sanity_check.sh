#!/bin/bash
set -euo pipefail

HOOK_NAME="${1:-unknown}"
INFRA_ENV_FILE="/etc/games/infra.env"

echo "[sanity:${HOOK_NAME}] starting deploy-time sanity output"

if [[ -f "$INFRA_ENV_FILE" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "$INFRA_ENV_FILE"
  set +a
else
  echo "[sanity:${HOOK_NAME}] infra env missing: ${INFRA_ENV_FILE}"
fi

GAMES_SERVER_NAME_VALUE="${GAMES_SERVER_NAME:-<unset>}"
GAMES_PORT_VALUE="${GAMES_PORT:-<unset>}"
GAMES_SECRET_NAME_VALUE="${GAMES_SECRET_NAME:-<unset>}"

CERT_FULLCHAIN="/etc/letsencrypt/live/${GAMES_SERVER_NAME_VALUE}/fullchain.pem"
CERT_PRIVKEY="/etc/letsencrypt/live/${GAMES_SERVER_NAME_VALUE}/privkey.pem"
CERT_PRESENT="no"

if [[ "$GAMES_SERVER_NAME_VALUE" != "<unset>" ]] && [[ -f "$CERT_FULLCHAIN" ]] && [[ -f "$CERT_PRIVKEY" ]]; then
  CERT_PRESENT="yes"
fi

echo "[sanity:${HOOK_NAME}] GAMES_SERVER_NAME=${GAMES_SERVER_NAME_VALUE}"
echo "[sanity:${HOOK_NAME}] GAMES_PORT=${GAMES_PORT_VALUE}"
echo "[sanity:${HOOK_NAME}] GAMES_SECRET_NAME=${GAMES_SECRET_NAME_VALUE}"
echo "[sanity:${HOOK_NAME}] cert_present=${CERT_PRESENT}"
