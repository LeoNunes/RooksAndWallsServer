# AI Strategies Design — Rooks and Walls

## Overview

Replace the single `RandomAiStrategy` with a multi-tier AI system using Monte Carlo Tree Search (MCTS). Four difficulty tiers (Easy, Medium, Hard, Maximum) share the same algorithm but differ in simulation count, time budget, and playout policy. MCTS is chosen over minimax because it naturally handles 2–4 player games without approximations, and scales to superhuman strength with more compute time.

## Architecture

Three new files added under `ai/`:

```
ai/
  AiStrategy.kt        (existing — RandomAiStrategy stays, used for piece placement)
  SimGame.kt           (new — lightweight cloneable game state for simulations)
  PlayoutPolicy.kt     (new — random and heuristic playout policies)
  MctsAiStrategy.kt    (new — MCTS engine + difficulty presets)
```

## Piece Placement Stage

`MctsAiStrategy` delegates the `PiecePlacement` stage entirely to `RandomAiStrategy` (random empty square). MCTS only runs during the `Moves` stage. This is a conscious simplification — placement strategy can be revisited later.

## SimGame

A pure, cloneable game state used for fast synchronous simulation — decoupled from the async `Game` class (no coroutines, mutex, or channels). Only models the `Moves` stage.

**State:**
- `pieces: MutableList<SimPiece>` — owner index + position
- `walls: MutableSet<EdgeCoordinate>`
- `currentPlayerIndex: Int`
- `playerCount: Int`
- `eliminatedPlayers: MutableSet<Int>`

**Key operations:**
- `getLegalMoves(): List<SimMove>` — full enumeration of legal `(pieceMovement?, wallPlacement)` pairs for the current player. Used by MCTS expansion to generate child nodes. Rook destinations are computed internally using a sliding-ray algorithm that respects walls and pieces (reimplementing the logic of `SteppedMovement` without depending on the live `Board`/`Piece` infrastructure).
- `sampleMove(policy: PlayoutPolicy): SimMove` — samples a single move according to the policy. Used during playout to avoid enumerating the full move space on every step.
- `applyMove(move: SimMove)` — mutates state: moves piece, places wall, runs region-check elimination (regions ≤ 8 squares), advances turn skipping eliminated players.
- `isTerminal(): Boolean` — true when only one player remains or all pieces are eliminated.
- `winner(): Int?` — only meaningful when `isTerminal()` is true. Returns the winning player index if exactly one player remains; null on draw (all pieces eliminated simultaneously). Must not be called on non-terminal states — backpropagation only calls it on the terminal state reached at the end of a playout.
- `clone(): SimGame` — deep copy for tree nodes.

The region-size threshold (≤ 8) is hardcoded to match the live game, which also hardcodes it. If `GameConfig` ever exposes this as a parameter, `SimGame` should be updated to read it.

Constructed from a `Game` snapshot at decision time by copying pieces, walls, player count, and current player index.

## MCTS Algorithm

Standard 4-phase UCT loop adapted for multi-player.

**Tree node fields:**
- `state: SimGame`
- `move: SimMove?` — move that produced this node
- `children: MutableList<MctsNode>`
- `visits: Int`
- `wins: IntArray(playerCount)` — per-player win counts

**Phases:**

1. **Selection** — descend from root using UCB1. At each node, score each child from the **parent's** `currentPlayerIndex` perspective (the player who chose the move leading to that child):
   `score = wins[parentPlayerIndex] / visits + C * sqrt(ln(parentVisits) / visits)`
   where `C = sqrt(2)` (standard UCT value; win rates are normalised to [0, 1] by dividing by simulation count, so this scale is appropriate).
   Stop at an unexpanded node or terminal state.

2. **Expansion** — call `getLegalMoves()`, add one new child node per legal move (or a random subset if the move count is very large).

3. **Simulation (playout)** — clone the child's state, call `sampleMove(policy)` repeatedly until `isTerminal()`. No tree nodes are created during playout. The playout is entirely single-threaded.

4. **Backpropagation** — walk from child to root, incrementing `visits` at every node. If `winner()` returns a non-null player index, increment `wins[winner]`. On a draw (`winner()` returns null), no `wins` entry is incremented — a draw is treated as a loss for all.

**Final selection:** child with highest visit count (more robust than raw win rate).

`chooseAction()` translates the winning `SimMove` back to a `GameAction` compatible with the existing `Game` interface. Since MCTS is entirely synchronous and single-threaded by design, no synchronization is needed on `MctsNode`.

## Playout Policies

**`RandomPlayoutPolicy`** — picks a random legal move at each step via `sampleMove`. Fast; used by Easy and Medium.

**`HeuristicPlayoutPolicy`** — biased sampling:
- Avoids walls that create a region ≤ 8 containing your own piece
- Prefers walls that create a region ≤ 8 containing only opponent pieces
- Falls back to random when no heuristic-preferred move exists

Used by Hard and Maximum.

## Difficulty Tiers

| Tier    | Simulations  | Timeout | Playout Policy |
|---------|--------------|---------|----------------|
| Easy    | 50           | —       | Random         |
| Medium  | 500          | —       | Random         |
| Hard    | 3000         | —       | Heuristic      |
| Maximum | Int.MAX_VALUE | 5s     | Heuristic      |

Expressed as `MctsConfig(simulationCount: Int, timeoutMs: Long?, playoutPolicy: PlayoutPolicy)`. Maximum sets `simulationCount = Int.MAX_VALUE` and uses `timeoutMs = 5000` as the termination condition; others set `timeoutMs = null` and stop at simulation count.

## Integration

**`GameManager.kt`** — wraps `chooseAction()` in `withContext(Dispatchers.Default)` so MCTS runs on the CPU thread pool without blocking the WebSocket coroutine. The `CoroutineScope` already available via `AppDependencies.coroutineScope` is used for the AI player runner loop (no change to current scope provisioning).

**AI endpoint** — `POST /rw/game/{gameId}/ai` gains an optional `difficulty: AiDifficulty` field (defaulting to `RANDOM` for backward compatibility). `AiDifficulty` is an enum: `RANDOM`, `EASY`, `MEDIUM`, `HARD`, `MAXIMUM`. The `RANDOM` value maps to the existing `RandomAiStrategy`; others map to `MctsAiStrategy` with the corresponding preset.

**No changes** to `Game.kt`, `Board.kt`, `common/`, or any other routing.

## Testing

- Unit tests for `SimGame`: verify legal move generation, wall elimination logic (region ≤ 8), turn advancement with skipped eliminated players, winner detection, and draw detection.
- Unit tests for each `PlayoutPolicy`: verify heuristic preferences trigger correctly when a wall would enclose opponent-only vs. self-containing regions.
- Integration test: run `MctsAiStrategy` on a near-terminal board state and verify it selects the winning move over the losing alternative.
