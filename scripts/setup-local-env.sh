#!/bin/bash
# Sets up a local .env file by fetching environment configuration from AWS SSM.
#
# Usage:   ./scripts/setup-local-env.sh [environment] [aws-profile]
# Example: ./scripts/setup-local-env.sh Beta my-aws-profile
#
# Arguments:
#   environment  - CDK environment name (default: Beta)
#   aws-profile  - AWS CLI profile used to read from SSM (default: default)
#
# The generated .env sets AWS_PROFILE to the local-dev role for that environment,
# which the server assumes at runtime to access DynamoDB.

set -euo pipefail

ENVIRONMENT=${1:-Beta}
SOURCE_PROFILE=${2:-default}

ENV_LOWER=$(echo "$ENVIRONMENT" | tr '[:upper:]' '[:lower:]')
APP_NAME="Games"
REGION="us-west-2"
ENV_FILE=".env"

echo "Fetching ${ENVIRONMENT} configuration from SSM (profile: ${SOURCE_PROFILE})..."

USER_POOL_ID=$(aws ssm get-parameter \
    --name "/${APP_NAME}/BE/${ENVIRONMENT}/CognitoUserPoolId" \
    --region "$REGION" \
    --profile "$SOURCE_PROFILE" \
    --query "Parameter.Value" \
    --output text)

cat > "$ENV_FILE" <<EOF
GAMES_COGNITO_REGION=${REGION}
GAMES_COGNITO_USER_POOL_ID=${USER_POOL_ID}
GAMES_USERS_TABLE_NAME=games-${ENV_LOWER}-users
AWS_PROFILE=games-${ENV_LOWER}-local-dev
EOF

echo "Created ${ENV_FILE} for the ${ENVIRONMENT} environment."
