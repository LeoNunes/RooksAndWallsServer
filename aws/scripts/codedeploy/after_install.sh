#!/bin/bash
set -euo pipefail

bash "$(dirname "$0")/sanity_check.sh" "after_install"

INFRA_ENV_FILE=/etc/games/infra.env
SERVICE_ENV_FILE=/etc/games/service.env

if [[ ! -f "$INFRA_ENV_FILE" ]]; then
  echo "Missing infra env file: $INFRA_ENV_FILE" >&2
  exit 1
fi

set -a
source "$INFRA_ENV_FILE"
set +a

: "${GAMES_SECRET_NAME:?GAMES_SECRET_NAME must be defined in /etc/games/infra.env}"
: "${GAMES_SERVER_NAME:?GAMES_SERVER_NAME must be defined in /etc/games/infra.env}"
: "${GAMES_ENVIRONMENT:?GAMES_ENVIRONMENT must be defined in /etc/games/infra.env}"
: "${GAMES_PORT:?GAMES_PORT must be defined in /etc/games/infra.env}"

# Detect the AWS region from EC2 instance metadata (IMDSv2).
IMDS_TOKEN=$(curl -sf --max-time 2 \
  -X PUT "http://169.254.169.254/latest/api/token" \
  -H "X-aws-ec2-metadata-token-ttl-seconds: 21600") \
  || { echo "Failed to obtain IMDSv2 token" >&2; exit 1; }
AWS_DEFAULT_REGION=$(curl -sf --max-time 2 \
  -H "X-aws-ec2-metadata-token: $IMDS_TOKEN" \
  "http://169.254.169.254/latest/meta-data/placement/region") \
  || { echo "Failed to detect AWS region from instance metadata" >&2; exit 1; }
export AWS_DEFAULT_REGION

# Fetch secret JSON from Secrets Manager
SECRET_JSON=$(aws secretsmanager get-secret-value \
  --secret-id "$GAMES_SECRET_NAME" \
  --query SecretString \
  --output text)

# Validate secret shape and render shell-escaped KEY=VALUE pairs.
echo "$SECRET_JSON" | jq -e 'type == "object"' >/dev/null
echo "$SECRET_JSON" | jq -r '
  to_entries[]
  | select(.value != null)
  | "\(.key)=\(.value | tostring | @sh)"
' > "$SERVICE_ENV_FILE"

# Secure the env file: only root can read it
chown root:root "$SERVICE_ENV_FILE"
chmod 600 "$SERVICE_ENV_FILE"
