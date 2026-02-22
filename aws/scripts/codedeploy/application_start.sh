#!/bin/bash
set -euo pipefail

bash "$(dirname "$0")/sanity_check.sh" "application_start"

INFRA_ENV_FILE=/etc/games/infra.env

if [[ ! -f "$INFRA_ENV_FILE" ]]; then
  echo "Missing infra env file: $INFRA_ENV_FILE" >&2
  exit 1
fi

set -a
source "$INFRA_ENV_FILE"
set +a

: "${GAMES_SERVER_NAME:?GAMES_SERVER_NAME must be defined in /etc/games/infra.env}"
: "${GAMES_ENVIRONMENT:?GAMES_ENVIRONMENT must be defined in /etc/games/infra.env}"
: "${GAMES_PORT:?GAMES_PORT must be defined in /etc/games/infra.env}"

# Deploy the unit file to the correct systemd location for locally-managed services
cp /opt/games/games.service /etc/systemd/system/games.service
chown root:root /etc/systemd/system/games.service
chmod 644 /etc/systemd/system/games.service

systemctl daemon-reload
systemctl enable games
systemctl restart games

systemctl enable nginx

NGINX_TMPL_DIR="/opt/games/nginx"
NGINX_SITE_FILE="/etc/nginx/conf.d/games-${GAMES_ENVIRONMENT}.conf"
mkdir -p "$(dirname "$NGINX_SITE_FILE")"

CERT_FULLCHAIN="/etc/letsencrypt/live/${GAMES_SERVER_NAME}/fullchain.pem"
CERT_PRIVKEY="/etc/letsencrypt/live/${GAMES_SERVER_NAME}/privkey.pem"

if [[ ! -f "$CERT_FULLCHAIN" || ! -f "$CERT_PRIVKEY" ]]; then
  CERTBOT_EMAIL="${LETSENCRYPT_EMAIL:-${GAMES_OPS_EMAIL:-}}"
  if [[ -z "$CERTBOT_EMAIL" && -n "${GAMES_HOSTED_ZONE_NAME:-}" ]]; then
    CERTBOT_EMAIL="ops@${GAMES_HOSTED_ZONE_NAME}"
  fi
  if [[ -z "$CERTBOT_EMAIL" ]]; then
    echo "Missing certbot email. Define LETSENCRYPT_EMAIL or GAMES_OPS_EMAIL (or GAMES_HOSTED_ZONE_NAME)." >&2
    exit 1
  fi

  # Render the HTTP-only config so nginx can start and serve the ACME webroot
  # challenge without needing cert files that don't exist yet.
  envsubst '${GAMES_SERVER_NAME}' \
    < "${NGINX_TMPL_DIR}/games-http.conf.tmpl" \
    > "$NGINX_SITE_FILE"
  /usr/sbin/nginx -t
  systemctl restart nginx

  certbot certonly \
    --webroot \
    -w /var/www/certbot \
    -d "$GAMES_SERVER_NAME" \
    --non-interactive \
    --agree-tos \
    -m "$CERTBOT_EMAIL"

  # Certs now exist — render the full HTTPS config and reload.
  envsubst '${GAMES_SERVER_NAME} ${GAMES_PORT}' \
    < "${NGINX_TMPL_DIR}/games-https.conf.tmpl" \
    > "$NGINX_SITE_FILE"
  /usr/sbin/nginx -t
  systemctl reload nginx
else
  envsubst '${GAMES_SERVER_NAME} ${GAMES_PORT}' \
    < "${NGINX_TMPL_DIR}/games-https.conf.tmpl" \
    > "$NGINX_SITE_FILE"
  /usr/sbin/nginx -t
  systemctl restart nginx
  certbot renew --quiet --deploy-hook "systemctl reload nginx"
fi