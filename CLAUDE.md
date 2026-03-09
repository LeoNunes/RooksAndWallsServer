# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Requirements:** JVM 17+, Kotlin 2.3, Ktor 3.4, Gradle 9.x

```bash
./gradlew run                                                # Port 5000 by default; override with GAMES_PORT
./gradlew test                                               # Run all tests
./gradlew test --tests "me.leonunes.model.GameTest"          # Run a specific test class
./gradlew buildFatJar                                        # Build → build/libs/service.jar
```

For IntelliJ dev mode, add `-Dio.ktor.development=true` to VM options (enables CORS for `localhost:5173`).

## Architecture

Top-level packages: `plugins/` (Ktor routing), `dto/` (JSON serialization, decoupled from model), `rooksandwalls/model/` (game logic), `common/` (reusable board/movement abstractions shared across potential future games).

**WebSocket protocol:** Clients join via `WS /rw/game/{gameId}`. An optional `token` query parameter carries a Cognito JWT; omitting it connects as a guest (random UUID identity). Invalid tokens cause an immediate close with code `CANNOT_ACCEPT`. On connect, the server calls `GameManager.joinGame(user)` — returning or reconnecting the player — then sends the full game state from that player's perspective. A new full state push is sent on every change. Actions are sent as a JSON object with exactly one non-null field (`addPiece` or `move`). Errors during action processing are returned as plain-text messages over the socket. On disconnect, the player's `connectionStatus` is set to `Disconnected` (reconnecting restores it to `Connected`).

**Game lifecycle:** `GameManagerFactory` (singleton via `AppDependencies`) maps `GameId → GameManager`. `POST /rw/game/` creates a new game (optionally with a custom `GameConfig`) and returns its `gameId`. `GameManager` wraps a `Game` and accumulates players; once all seats are filled it calls `game.start(players)`. Player identity (`User`) comes from `UserService`: authenticated users are resolved via `CognitoJwtValidator`, guests get a transient UUID.
