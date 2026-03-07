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

**WebSocket protocol:** Clients join via `WS /rw/game/{gameId}`. On connect they immediately receive the full game state, then receive a new full state push on every change. Actions are sent as a JSON object with exactly one non-null field (`addPiece` or `move`). Game state is managed in memory; `GameFactory` is a singleton holding all active games.
