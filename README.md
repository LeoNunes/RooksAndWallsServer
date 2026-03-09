# Rooks And Walls Server
Backend project for the Rooks And Walls Project.

## Requirements
- **JVM 17 or later** (required for Kotlin 2.3, Ktor 3.4, and Gradle 9.x)

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `GAMES_PORT` | No | Port the server listens on (default: `5000`) |
| `GAMES_COGNITO_REGION` | Yes | AWS region of the Cognito user pool (e.g. `us-east-1`) |
| `GAMES_COGNITO_USER_POOL_ID` | Yes | Cognito user pool ID used for JWT validation |
| `GAMES_USERS_TABLE_NAME` | Yes | DynamoDB table name for storing user data |

## IntelliJ Setup
To enable development mode, add the following line to `VM options` in the project configuration:
```-Dio.ktor.development=true```

## Local Development

Running locally requires a Cognito User Pool (for JWT validation) and a DynamoDB table (for user storage). The recommended approach is to use the real **Beta** environment's resources.

### 1. Configure an AWS profile for DynamoDB access

The CDK project creates an IAM role (`games-beta-local-dev`) that grants read/write access to the Beta DynamoDB table. Add a profile to `~/.aws/config` that assumes it:

```ini
[profile games-beta-local-dev]
role_arn = arn:aws:iam::<account-id>:role/games-beta-local-dev
source_profile = default
region = us-west-2
```

`source_profile` should point to whichever profile holds your base IAM credentials. The SDK will assume the role automatically and handle credential refresh.

### 2. Generate the `.env` file

Run the setup script to fetch the required values from AWS SSM and write a `.env` file:

```bash
./scripts/setup-local-env.sh
# or, to target a specific environment or use a non-default AWS profile:
./scripts/setup-local-env.sh Beta my-aws-profile
```

This creates a `.env` file (gitignored) with:

```
GAMES_COGNITO_REGION=us-west-2
GAMES_COGNITO_USER_POOL_ID=<fetched from SSM>
GAMES_USERS_TABLE_NAME=games-beta-users
AWS_PROFILE=games-beta-local-dev
```

### 3. Load the `.env` in IntelliJ

In your run configuration, add the environment variables from `.env`. The simplest way is to install the [EnvFile plugin](https://plugins.jetbrains.com/plugin/7861-envfile), enable it in the run configuration, and point it at `.env`. Alternatively, paste the values directly into the **Environment variables** field.