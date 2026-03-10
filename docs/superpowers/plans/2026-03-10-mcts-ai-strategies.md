# MCTS AI Strategies Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add four difficulty tiers (Easy/Medium/Hard/Maximum) of MCTS-based AI that handles 2–4 players and scales to superhuman strength at Maximum.

**Architecture:** A lightweight `SimGame` class mirrors game logic for fast synchronous simulation without coroutines. `MctsAiStrategy` runs UCT (MCTS with UCB1) on a tree of `MctsNode`s, using a swappable `PlayoutPolicy` to guide rollouts. Piece placement delegates to the existing `RandomAiStrategy`; MCTS only runs during the Moves stage.

**Tech Stack:** Kotlin, `kotlinx.coroutines` (`withContext(Dispatchers.Default)` for non-blocking execution), existing `me.leonunes.games.common` board utilities (`sliceIntoRegions`, `EdgeCoordinate`, `SquareCoordinate`), Ktor Resources for the routing update.

---

## File Structure

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `src/main/kotlin/me/leonunes/games/rooksandwalls/ai/SimGame.kt` | Cloneable, synchronous simulation state; rook movement; applyMove; region elimination |
| Create | `src/main/kotlin/me/leonunes/games/rooksandwalls/ai/PlayoutPolicy.kt` | `PlayoutPolicy` interface; `RandomPlayoutPolicy`; `HeuristicPlayoutPolicy` |
| Create | `src/main/kotlin/me/leonunes/games/rooksandwalls/ai/MctsAiStrategy.kt` | `MctsNode`; `MctsConfig` presets; `AiDifficulty` enum; `MctsAiStrategy` MCTS loop |
| Modify | `src/main/kotlin/me/leonunes/games/rooksandwalls/ai/AiPlayerRunner.kt` | Add `withContext(Dispatchers.Default)` around `chooseAction` call |
| Modify | `src/main/kotlin/me/leonunes/games/plugins/Game.kt` | Add `difficulty` field to `AddAiPlayerRequestBody`; pass strategy to `addAiPlayer` |
| Create | `src/test/kotlin/me/leonunes/games/rooksandwalls/ai/SimGameTest.kt` | Unit tests for SimGame |
| Create | `src/test/kotlin/me/leonunes/games/rooksandwalls/ai/PlayoutPolicyTest.kt` | Unit tests for playout policies |
| Create | `src/test/kotlin/me/leonunes/games/rooksandwalls/ai/MctsAiStrategyTest.kt` | Integration tests for MctsAiStrategy |

---

## Chunk 1: SimGame

### Task 1: SimPiece, SimMove, and SimGame skeleton

**Files:**
- Create: `src/main/kotlin/me/leonunes/games/rooksandwalls/ai/SimGame.kt`
- Create: `src/test/kotlin/me/leonunes/games/rooksandwalls/ai/SimGameTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// src/test/kotlin/me/leonunes/games/rooksandwalls/ai/SimGameTest.kt
package me.leonunes.games.rooksandwalls.ai

import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.coord
import me.leonunes.games.rooksandwalls.model.createGameWithPlayers
import me.leonunes.games.rooksandwalls.model.runAddPieceActions
import me.leonunes.games.rooksandwalls.model.player1
import me.leonunes.games.rooksandwalls.model.GameConfigDefaultValues
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame

class SimGameTest {

    // Uses the default 3-player, 3-piece, 8x8 config so runAddPieceActions() works correctly
    private fun defaultGame(): SimGame {
        val game = createGameWithPlayers(GameConfigDefaultValues)
        game.runAddPieceActions()  // places 9 pieces and enters the Moves stage
        return SimGame.from(game)
    }

    @Test
    fun `SimGame can be constructed from a Game snapshot`() {
        val game = createGameWithPlayers(GameConfigDefaultValues)
        game.runAddPieceActions()
        val sim = SimGame.from(game)
        assertEquals(3, sim.playerCount)
        assertEquals(8, sim.rows)
        assertEquals(8, sim.columns)
        assertEquals(9, sim.pieces.size)  // 3 players × 3 pieces
        // currentPlayerIndex should match the current turn player
        val expectedIndex = game.players.indexOfFirst { it.id == game.currentTurn?.id }
        assertEquals(expectedIndex, sim.currentPlayerIndex)
        assertFalse(sim.isTerminal())
    }

    @Test
    fun `clone produces an independent copy`() {
        val sim = defaultGame()
        val originalPosition = sim.pieces[0].position
        val clone = sim.clone()
        assertNotSame(sim, clone)
        assertNotSame(sim.pieces, clone.pieces)
        clone.pieces[0].position = coord(7, 7)
        assertEquals(originalPosition, sim.pieces[0].position)  // original unaffected
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.SimGameTest"
```
Expected: FAIL with "Unresolved reference: SimGame"

- [ ] **Step 3: Write the skeleton**

```kotlin
// src/main/kotlin/me/leonunes/games/rooksandwalls/ai/SimGame.kt
package me.leonunes.games.rooksandwalls.ai

import me.leonunes.games.common.*
import me.leonunes.games.rooksandwalls.model.Game
import me.leonunes.games.rooksandwalls.model.Wall

data class SimPiece(val ownerIndex: Int, var position: SquareCoordinate)

data class SimMove(
    val pieceIndex: Int?,                // index into SimGame.pieces; null = no piece movement
    val destination: SquareCoordinate?,  // null = no piece movement
    val wallPosition: EdgeCoordinate
)

class SimGame(
    override val rows: Int,
    override val columns: Int,
    val playerCount: Int,
    var currentPlayerIndex: Int,
    val pieces: MutableList<SimPiece>,
    internal val wallSet: MutableSet<EdgeCoordinate>,  // `internal` so tests can inspect walls directly
    val eliminatedPlayers: MutableSet<Int>
) : GridBoard, WithWalls<Wall> {

    // Satisfies WithWalls<Wall> so sliceIntoRegions() can be called on this object
    override val walls: List<Wall> get() = wallSet.map { Wall(it) }

    fun clone(): SimGame = SimGame(
        rows = rows,
        columns = columns,
        playerCount = playerCount,
        currentPlayerIndex = currentPlayerIndex,
        pieces = pieces.map { it.copy() }.toMutableList(),
        wallSet = wallSet.toMutableSet(),
        eliminatedPlayers = eliminatedPlayers.toMutableSet()
    )

    fun addWall(edge: EdgeCoordinate) { wallSet.add(edge) }

    companion object {
        fun from(game: Game): SimGame {
            val players = game.players
            val playerIndexById = players.mapIndexed { idx, p -> p.id to idx }.toMap()
            val currentPlayerIndex = playerIndexById[game.currentTurn?.id] ?: 0
            val eliminatedIds = players.map { it.id }.toSet() - game.remainingPlayers.map { it.id }.toSet()
            val eliminatedIndices = eliminatedIds.map { playerIndexById[it]!! }.toMutableSet()

            return SimGame(
                rows = game.config.boardRows,
                columns = game.config.boardColumns,
                playerCount = players.size,
                currentPlayerIndex = currentPlayerIndex,
                pieces = game.pieces.map { piece ->
                    SimPiece(playerIndexById[piece.owner.id]!!, piece.position)
                }.toMutableList(),
                wallSet = game.walls.map { it.position }.toMutableSet(),
                eliminatedPlayers = eliminatedIndices
            )
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.SimGameTest"
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/me/leonunes/games/rooksandwalls/ai/SimGame.kt \
        src/test/kotlin/me/leonunes/games/rooksandwalls/ai/SimGameTest.kt
git commit -m "feat: add SimGame skeleton with clone and from(Game)"
```

---

### Task 2: Rook movement and available walls

**Files:**
- Modify: `src/main/kotlin/me/leonunes/games/rooksandwalls/ai/SimGame.kt`
- Modify: `src/test/kotlin/me/leonunes/games/rooksandwalls/ai/SimGameTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `SimGameTest`:

```kotlin
    @Test
    fun `rookDestinations returns all reachable squares in open row and column`() {
        val sim = defaultGame()
        // find a piece with a clear path (board is mostly empty after placement)
        val piece = sim.pieces.first()
        val dests = sim.rookDestinations(piece.position)
        assert(dests.size >= 4)
    }

    @Test
    fun `rookDestinations is blocked by another piece`() {
        val sim = SimGame(
            rows = 4, columns = 4, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(
                SimPiece(0, coord(0, 0)),
                SimPiece(1, coord(0, 2))  // blocker
            ),
            wallSet = mutableSetOf(),
            eliminatedPlayers = mutableSetOf()
        )
        val dests = sim.rookDestinations(coord(0, 0))
        // Can reach (0,1) but NOT (0,2) (occupied) and NOT (0,3) (past blocker)
        assert(coord(0, 1) in dests)
        assert(coord(0, 2) !in dests)
        assert(coord(0, 3) !in dests)
    }

    @Test
    fun `rookDestinations is blocked by a wall`() {
        val sim = SimGame(
            rows = 4, columns = 4, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(0, 0))),
            wallSet = mutableSetOf(EdgeCoordinate(coord(0, 1), coord(0, 2))),  // wall between col 1 and 2
            eliminatedPlayers = mutableSetOf()
        )
        val dests = sim.rookDestinations(coord(0, 0))
        assert(coord(0, 1) in dests)
        assert(coord(0, 2) !in dests)
    }

    @Test
    fun `availableWalls excludes already placed walls`() {
        val sim = defaultGame()
        val allCount = sim.availableWalls().size
        assert(allCount > 0)
        val firstWall = sim.availableWalls().first()
        sim.addWall(firstWall)  // directly adds to wallSet for isolation
        assertEquals(allCount - 1, sim.availableWalls().size)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.SimGameTest"
```
Expected: FAIL with "Unresolved reference: rookDestinations"

- [ ] **Step 3: Implement rook movement and availableWalls**

Add to `SimGame`:

```kotlin
    internal fun rookDestinations(from: SquareCoordinate): Set<SquareCoordinate> {
        val occupiedSquares = pieces.map { it.position }.toSet()
        val result = mutableSetOf<SquareCoordinate>()
        val directions = listOf(coordStep(0, 1), coordStep(0, -1), coordStep(1, 0), coordStep(-1, 0))

        for (dir in directions) {
            var cur = from
            while (true) {
                val next = dir.takeStep(cur)
                if (!isInsideBoard(next)) break
                if (EdgeCoordinate(cur, next) in wallSet) break
                if (next in occupiedSquares) break
                result.add(next)
                cur = next
            }
        }
        return result
    }

    private fun allEdgePositions(): List<EdgeCoordinate> =
        (0 until rows).flatMap { row ->
            (0 until columns).flatMap { col ->
                buildList {
                    if (row + 1 < rows) add(EdgeCoordinate(coord(row, col), coord(row + 1, col)))
                    if (col + 1 < columns) add(EdgeCoordinate(coord(row, col), coord(row, col + 1)))
                }
            }
        }

    fun availableWalls(): List<EdgeCoordinate> = allEdgePositions().filter { it !in wallSet }
```

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.SimGameTest"
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/me/leonunes/games/rooksandwalls/ai/SimGame.kt \
        src/test/kotlin/me/leonunes/games/rooksandwalls/ai/SimGameTest.kt
git commit -m "feat: add rookDestinations and availableWalls to SimGame"
```

---

### Task 3: applyMove and region elimination

**Files:**
- Modify: `src/main/kotlin/me/leonunes/games/rooksandwalls/ai/SimGame.kt`
- Modify: `src/test/kotlin/me/leonunes/games/rooksandwalls/ai/SimGameTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `SimGameTest`:

```kotlin
    @Test
    fun `applyMove moves piece to destination`() {
        val sim = SimGame(
            rows = 4, columns = 4, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(0, 0)), SimPiece(1, coord(3, 3))),
            wallSet = mutableSetOf(), eliminatedPlayers = mutableSetOf()
        )
        val safeWall = EdgeCoordinate(coord(2, 0), coord(3, 0))
        sim.applyMove(SimMove(pieceIndex = 0, destination = coord(0, 2), wallPosition = safeWall))
        assertEquals(coord(0, 2), sim.pieces[0].position)
    }

    @Test
    fun `applyMove places wall`() {
        val sim = SimGame(
            rows = 4, columns = 4, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(0, 0)), SimPiece(1, coord(3, 3))),
            wallSet = mutableSetOf(), eliminatedPlayers = mutableSetOf()
        )
        val wall = EdgeCoordinate(coord(1, 0), coord(2, 0))
        sim.applyMove(SimMove(null, null, wall))
        assert(wall in sim.wallSet)
    }

    @Test
    fun `applyMove eliminates pieces enclosed in region of 8 or fewer`() {
        // 3x3 board. (0,0) is enclosed by: top board edge, left board edge,
        // wall below (row0→row1 on col0), and wall right (col0→col1 on row0).
        // That leaves a 1-square region at (0,0). Applying any other wall triggers elimination.
        val sim = SimGame(
            rows = 3, columns = 3, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(0, 0)), SimPiece(1, coord(2, 2))),
            wallSet = mutableSetOf(
                EdgeCoordinate(coord(0, 0), coord(1, 0)),  // below (0,0)
                EdgeCoordinate(coord(0, 0), coord(0, 1)),  // right of (0,0)
            ),
            eliminatedPlayers = mutableSetOf()
        )
        val safeWall = EdgeCoordinate(coord(1, 1), coord(2, 1))  // far from (0,0)
        sim.applyMove(SimMove(null, null, safeWall))
        // Player 0's piece at (0,0) is in a 1-square region → eliminated
        assertEquals(1, sim.pieces.size)
        assertEquals(coord(2, 2), sim.pieces[0].position)
    }

    @Test
    fun `applyMove advances turn to next non-eliminated player`() {
        val sim = SimGame(
            rows = 4, columns = 4, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(0, 0)), SimPiece(1, coord(3, 3))),
            wallSet = mutableSetOf(), eliminatedPlayers = mutableSetOf()
        )
        sim.applyMove(SimMove(null, null, EdgeCoordinate(coord(0, 0), coord(0, 1))))
        assertEquals(1, sim.currentPlayerIndex)
    }

    @Test
    fun `applyMove skips eliminated players when advancing turn`() {
        val sim = SimGame(
            rows = 4, columns = 4, playerCount = 3, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(0, 0)), SimPiece(2, coord(3, 3))),
            wallSet = mutableSetOf(), eliminatedPlayers = mutableSetOf(1)  // player 1 already out
        )
        sim.applyMove(SimMove(null, null, EdgeCoordinate(coord(0, 0), coord(0, 1))))
        assertEquals(2, sim.currentPlayerIndex)  // skips player 1
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.SimGameTest"
```
Expected: FAIL with "Unresolved reference: applyMove"

- [ ] **Step 3: Implement applyMove**

Add to `SimGame`:

```kotlin
    fun applyMove(move: SimMove) {
        // 1. Move piece if specified
        if (move.pieceIndex != null && move.destination != null) {
            pieces[move.pieceIndex].position = move.destination
        }

        // 2. Place wall
        wallSet.add(move.wallPosition)

        // 3. Eliminate pieces in regions of ≤ 8 squares
        val deadSquares = sliceIntoRegions()
            .filter { it.size <= 8 }
            .flatten()
            .toSet()
        pieces.removeAll { it.position in deadSquares }

        // 4. Update eliminated players
        val activePlayers = pieces.map { it.ownerIndex }.toSet()
        for (p in 0 until playerCount) {
            if (p !in activePlayers) eliminatedPlayers.add(p)
        }

        // 5. Advance turn, skipping eliminated players
        if (!isTerminal()) {
            do {
                currentPlayerIndex = (currentPlayerIndex + 1) % playerCount
            } while (currentPlayerIndex in eliminatedPlayers)
        }
    }
```

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.SimGameTest"
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/me/leonunes/games/rooksandwalls/ai/SimGame.kt \
        src/test/kotlin/me/leonunes/games/rooksandwalls/ai/SimGameTest.kt
git commit -m "feat: implement SimGame.applyMove with elimination logic"
```

---

### Task 4: isTerminal, winner, and getLegalMoves

**Files:**
- Modify: `src/main/kotlin/me/leonunes/games/rooksandwalls/ai/SimGame.kt`
- Modify: `src/test/kotlin/me/leonunes/games/rooksandwalls/ai/SimGameTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `SimGameTest`:

```kotlin
    @Test
    fun `isTerminal is false mid-game`() {
        assertFalse(defaultGame().isTerminal())
    }

    @Test
    fun `isTerminal is true when only one player remains`() {
        val sim = SimGame(
            rows = 4, columns = 4, playerCount = 2, currentPlayerIndex = 1,
            pieces = mutableListOf(SimPiece(0, coord(0, 0))),  // only player 0 remains
            wallSet = mutableSetOf(), eliminatedPlayers = mutableSetOf(1)
        )
        assert(sim.isTerminal())
    }

    @Test
    fun `isTerminal is true when all pieces eliminated (draw)`() {
        val sim = SimGame(
            rows = 4, columns = 4, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(),  // no pieces
            wallSet = mutableSetOf(), eliminatedPlayers = mutableSetOf(0, 1)
        )
        assert(sim.isTerminal())
    }

    @Test
    fun `winner returns the sole remaining player index`() {
        val sim = SimGame(
            rows = 4, columns = 4, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(0, 0))),
            wallSet = mutableSetOf(), eliminatedPlayers = mutableSetOf(1)
        )
        assertEquals(0, sim.winner())
    }

    @Test
    fun `winner returns null on draw`() {
        val sim = SimGame(
            rows = 4, columns = 4, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(), wallSet = mutableSetOf(),
            eliminatedPlayers = mutableSetOf(0, 1)
        )
        assertEquals(null, sim.winner())
    }

    @Test
    fun `getLegalMoves returns at least one move in a non-terminal game`() {
        val sim = defaultGame()
        assert(sim.getLegalMoves().isNotEmpty())
    }

    @Test
    fun `getLegalMoves returns only wall moves when current player has no legal piece moves`() {
        // Place a piece surrounded on all sides by walls
        val sim = SimGame(
            rows = 3, columns = 3, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(1, 1)), SimPiece(1, coord(2, 2))),
            wallSet = mutableSetOf(
                EdgeCoordinate(coord(0, 1), coord(1, 1)),
                EdgeCoordinate(coord(1, 1), coord(2, 1)),
                EdgeCoordinate(coord(1, 0), coord(1, 1)),
                EdgeCoordinate(coord(1, 1), coord(1, 2)),
            ),
            eliminatedPlayers = mutableSetOf()
        )
        val moves = sim.getLegalMoves()
        assert(moves.all { it.pieceIndex == null })
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.SimGameTest"
```
Expected: FAIL with "Unresolved reference: isTerminal"

- [ ] **Step 3: Implement isTerminal, winner, and getLegalMoves**

Add to `SimGame`:

```kotlin
    fun isTerminal(): Boolean {
        val activePlayers = pieces.map { it.ownerIndex }.toSet()
        return activePlayers.size <= 1
    }

    /**
     * Only call this when isTerminal() is true.
     * Returns the winning player index, or null if it's a draw (all pieces eliminated).
     */
    fun winner(): Int? {
        val activePlayers = pieces.map { it.ownerIndex }.toSet()
        return if (activePlayers.size == 1) activePlayers.first() else null
    }

    fun getLegalMoves(): List<SimMove> {
        val available = availableWalls()
        val myPieceIndices = pieces.indices.filter { pieces[it].ownerIndex == currentPlayerIndex }
        val pieceMoves: List<Pair<Int, SquareCoordinate>> = myPieceIndices.flatMap { idx ->
            rookDestinations(pieces[idx].position).map { dest -> idx to dest }
        }

        return if (pieceMoves.isEmpty()) {
            available.map { wall -> SimMove(null, null, wall) }
        } else {
            pieceMoves.flatMap { (pieceIdx, dest) ->
                available.map { wall -> SimMove(pieceIdx, dest, wall) }
            }
        }
    }
```

- [ ] **Step 4: Run all SimGame tests**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.SimGameTest"
```
Expected: all PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/me/leonunes/games/rooksandwalls/ai/SimGame.kt \
        src/test/kotlin/me/leonunes/games/rooksandwalls/ai/SimGameTest.kt
git commit -m "feat: add isTerminal, winner, and getLegalMoves to SimGame"
```

---

## Chunk 2: PlayoutPolicy and MCTS Engine

### Task 5: PlayoutPolicy interface and RandomPlayoutPolicy

**Files:**
- Create: `src/main/kotlin/me/leonunes/games/rooksandwalls/ai/PlayoutPolicy.kt`
- Create: `src/test/kotlin/me/leonunes/games/rooksandwalls/ai/PlayoutPolicyTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// src/test/kotlin/me/leonunes/games/rooksandwalls/ai/PlayoutPolicyTest.kt
package me.leonunes.games.rooksandwalls.ai

import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.coord
import me.leonunes.games.rooksandwalls.model.GameConfigDefaultValues
import me.leonunes.games.rooksandwalls.model.createGameWithPlayers
import me.leonunes.games.rooksandwalls.model.runAddPieceActions
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PlayoutPolicyTest {

    private fun sim(): SimGame {
        val game = createGameWithPlayers(GameConfigDefaultValues)
        game.runAddPieceActions()
        return SimGame.from(game)
    }

    @Test
    fun `RandomPlayoutPolicy produces a move with a valid wall position`() {
        val policy = RandomPlayoutPolicy()
        val simGame = sim()
        val move = policy.sampleMove(simGame)
        assertNotNull(move.wallPosition)
        assert(move.wallPosition !in simGame.wallSet)
    }

    @Test
    fun `RandomPlayoutPolicy produces no piece movement when current player has none`() {
        val locked = SimGame(
            rows = 3, columns = 3, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(1, 1)), SimPiece(1, coord(2, 2))),
            wallSet = mutableSetOf(
                EdgeCoordinate(coord(0, 1), coord(1, 1)),
                EdgeCoordinate(coord(1, 1), coord(2, 1)),
                EdgeCoordinate(coord(1, 0), coord(1, 1)),
                EdgeCoordinate(coord(1, 1), coord(1, 2)),
            ),
            eliminatedPlayers = mutableSetOf()
        )
        val move = RandomPlayoutPolicy().sampleMove(locked)
        assertNull(move.pieceIndex)
        assertNull(move.destination)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.PlayoutPolicyTest"
```
Expected: FAIL with "Unresolved reference: RandomPlayoutPolicy"

- [ ] **Step 3: Implement PlayoutPolicy and RandomPlayoutPolicy**

```kotlin
// src/main/kotlin/me/leonunes/games/rooksandwalls/ai/PlayoutPolicy.kt
package me.leonunes.games.rooksandwalls.ai

import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.SquareCoordinate

interface PlayoutPolicy {
    fun sampleMove(game: SimGame): SimMove
}

class RandomPlayoutPolicy : PlayoutPolicy {
    override fun sampleMove(game: SimGame): SimMove {
        val myPieceIndices = game.pieces.indices.filter { game.pieces[it].ownerIndex == game.currentPlayerIndex }
        val pieceMoves: List<Pair<Int, SquareCoordinate>> = myPieceIndices.flatMap { idx ->
            game.rookDestinations(game.pieces[idx].position).map { dest -> idx to dest }
        }
        val movement = pieceMoves.randomOrNull()
        val wallPosition = game.availableWalls().random()
        return SimMove(movement?.first, movement?.second, wallPosition)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.PlayoutPolicyTest"
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/me/leonunes/games/rooksandwalls/ai/PlayoutPolicy.kt \
        src/test/kotlin/me/leonunes/games/rooksandwalls/ai/PlayoutPolicyTest.kt
git commit -m "feat: add PlayoutPolicy interface and RandomPlayoutPolicy"
```

---

### Task 6: HeuristicPlayoutPolicy

**Files:**
- Modify: `src/main/kotlin/me/leonunes/games/rooksandwalls/ai/PlayoutPolicy.kt`
- Modify: `src/test/kotlin/me/leonunes/games/rooksandwalls/ai/PlayoutPolicyTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `PlayoutPolicyTest`:

```kotlin
    @Test
    fun `HeuristicPlayoutPolicy avoids wall that kills own piece when safe alternative exists`() {
        // Setup: player 0's piece at (0,0). Two walls available:
        // - killerWall: closes a region of 1 containing (0,0)
        // - safeWall: elsewhere
        // Policy should pick safeWall
        val sim = SimGame(
            rows = 3, columns = 3, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(0, 0)), SimPiece(1, coord(2, 2))),
            wallSet = mutableSetOf(
                EdgeCoordinate(coord(0, 0), coord(1, 0)),  // wall below (0,0)
                // wall to the right is NOT yet placed — that's the killerWall
            ),
            eliminatedPlayers = mutableSetOf()
        )
        // killerWall at right of (0,0) closes a 1-square region (board top-left + wall below + wall right)
        val killerWall = EdgeCoordinate(coord(0, 0), coord(0, 1))

        // Run policy many times; it should almost never pick the killerWall
        val policy = HeuristicPlayoutPolicy()
        var killerCount = 0
        repeat(20) {
            val move = policy.sampleMove(sim)
            if (move.wallPosition == killerWall) killerCount++
        }
        assert(killerCount < 20) { "HeuristicPlayoutPolicy picked the killer wall every time" }
    }

    @Test
    fun `HeuristicPlayoutPolicy prefers wall that kills only opponent`() {
        // Setup: player 1's piece at (0,0) enclosed on 3 sides; player 0 can close the box
        val sim = SimGame(
            rows = 3, columns = 3, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(2, 2)), SimPiece(1, coord(0, 0))),
            wallSet = mutableSetOf(
                EdgeCoordinate(coord(0, 0), coord(1, 0)),  // below (0,0)
                // right of (0,0) is the killingWall — closes opponent in a region of 1
            ),
            eliminatedPlayers = mutableSetOf()
        )
        val killingWall = EdgeCoordinate(coord(0, 0), coord(0, 1))

        val policy = HeuristicPlayoutPolicy()
        var killingCount = 0
        repeat(20) {
            val move = policy.sampleMove(sim)
            if (move.wallPosition == killingWall) killingCount++
        }
        assert(killingCount > 0) { "HeuristicPlayoutPolicy never picked the opponent-killing wall" }
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.PlayoutPolicyTest"
```
Expected: FAIL with "Unresolved reference: HeuristicPlayoutPolicy"

- [ ] **Step 3: Implement HeuristicPlayoutPolicy**

Add to `PlayoutPolicy.kt`:

```kotlin
class HeuristicPlayoutPolicy : PlayoutPolicy {
    override fun sampleMove(game: SimGame): SimMove {
        val myPieceIndices = game.pieces.indices.filter { game.pieces[it].ownerIndex == game.currentPlayerIndex }
        val pieceMoves: List<Pair<Int, SquareCoordinate>> = myPieceIndices.flatMap { idx ->
            game.rookDestinations(game.pieces[idx].position).map { dest -> idx to dest }
        }
        val movement = pieceMoves.randomOrNull()

        // Clone game state and apply piece movement manually (without running applyMove's full
        // elimination logic). This is a deliberate simplification: we only need to evaluate
        // wall candidates, and the piece-move-triggered elimination edge case is rare enough
        // that the heuristic quality is not materially affected.
        val testGame = game.clone()
        if (movement != null) testGame.pieces[movement.first].position = movement.second

        val myPositions = testGame.pieces
            .filter { it.ownerIndex == testGame.currentPlayerIndex }
            .map { it.position }.toSet()
        val opponentPositions = testGame.pieces
            .filter { it.ownerIndex != testGame.currentPlayerIndex }
            .map { it.position }.toSet()

        // Sample up to 10 candidate walls and classify them
        val available = testGame.availableWalls()
        val candidates = available.shuffled().take(minOf(10, available.size))

        fun wallRegions(wall: EdgeCoordinate): List<Set<SquareCoordinate>> {
            val g = testGame.clone()
            g.addWall(wall)
            return g.sliceIntoRegions().filter { it.size <= 8 }
        }

        // Prefer: kills opponent-only region
        val killerWall = candidates.firstOrNull { wall ->
            val smallRegions = wallRegions(wall)
            smallRegions.any { region -> region.any { it in opponentPositions } } &&
            smallRegions.none { region -> region.any { it in myPositions } }
        }
        if (killerWall != null) return SimMove(movement?.first, movement?.second, killerWall)

        // Otherwise: avoid self-killing wall
        val safeWall = candidates.firstOrNull { wall ->
            val smallRegions = wallRegions(wall)
            smallRegions.none { region -> region.any { it in myPositions } }
        }
        val wallPosition = safeWall ?: candidates.first()

        return SimMove(movement?.first, movement?.second, wallPosition)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.PlayoutPolicyTest"
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/me/leonunes/games/rooksandwalls/ai/PlayoutPolicy.kt \
        src/test/kotlin/me/leonunes/games/rooksandwalls/ai/PlayoutPolicyTest.kt
git commit -m "feat: add HeuristicPlayoutPolicy"
```

---

### Task 7: MctsNode, MctsConfig, and difficulty presets

**Files:**
- Create: `src/main/kotlin/me/leonunes/games/rooksandwalls/ai/MctsAiStrategy.kt`

- [ ] **Step 1: Create the file with MctsNode and MctsConfig**

No tests for these data structures in isolation — they'll be covered by the integration test in Task 8.

```kotlin
// src/main/kotlin/me/leonunes/games/rooksandwalls/ai/MctsAiStrategy.kt
package me.leonunes.games.rooksandwalls.ai

import me.leonunes.games.rooksandwalls.model.*
import kotlin.math.ln
import kotlin.math.sqrt

private const val MAX_CHILDREN = 30
private const val SQRT_2 = 1.4142135623730951  // UCB1 exploration constant C = sqrt(2)

class MctsNode(
    val state: SimGame,
    val move: SimMove?,
    val parent: MctsNode?
) {
    val children = mutableListOf<MctsNode>()
    var visits = 0
    val wins = IntArray(state.playerCount)

    val isFullyExpanded: Boolean
        get() = state.isTerminal() || children.size >= MAX_CHILDREN

    fun addChild(move: SimMove): MctsNode {
        val newState = state.clone()
        newState.applyMove(move)
        return MctsNode(newState, move, this).also { children.add(it) }
    }

    /**
     * Selects the best child from the perspective of [state.currentPlayerIndex] (the player
     * who will choose among these children). Unvisited children always get priority.
     */
    fun bestChild(): MctsNode {
        val parentPlayerIndex = state.currentPlayerIndex
        val logParentVisits = ln(visits.toDouble())
        return children.maxBy { child ->
            if (child.visits == 0) Double.MAX_VALUE
            else {
                val exploitation = child.wins[parentPlayerIndex].toDouble() / child.visits
                val exploration = SQRT_2 * sqrt(logParentVisits / child.visits)
                exploitation + exploration
            }
        }
    }
}

data class MctsConfig(
    val simulationCount: Int,
    val timeoutMs: Long?,
    val playoutPolicy: PlayoutPolicy
) {
    companion object {
        val EASY    = MctsConfig(50,             null,   RandomPlayoutPolicy())
        val MEDIUM  = MctsConfig(500,            null,   RandomPlayoutPolicy())
        val HARD    = MctsConfig(3000,           null,   HeuristicPlayoutPolicy())
        val MAXIMUM = MctsConfig(Int.MAX_VALUE,  5000L,  HeuristicPlayoutPolicy())
    }
}

enum class AiDifficulty {
    RANDOM, EASY, MEDIUM, HARD, MAXIMUM;

    fun toAiStrategy(): AiStrategy = when (this) {
        RANDOM  -> RandomAiStrategy()
        EASY    -> MctsAiStrategy(MctsConfig.EASY)
        MEDIUM  -> MctsAiStrategy(MctsConfig.MEDIUM)
        HARD    -> MctsAiStrategy(MctsConfig.HARD)
        MAXIMUM -> MctsAiStrategy(MctsConfig.MAXIMUM)
    }
}
```

- [ ] **Step 2: Verify the file compiles**

```
./gradlew compileKotlin
```
Expected: BUILD SUCCESSFUL (MctsAiStrategy referenced but not yet defined — add a stub if needed)

If the compiler complains about `MctsAiStrategy` not existing, add a temporary stub at the bottom of the file:
```kotlin
class MctsAiStrategy(private val config: MctsConfig) : AiStrategy {
    override fun chooseAction(game: Game, playerId: PlayerId): GameAction = TODO()
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/me/leonunes/games/rooksandwalls/ai/MctsAiStrategy.kt
git commit -m "feat: add MctsNode, MctsConfig presets, and AiDifficulty enum"
```

---

### Task 8: MCTS loop and MctsAiStrategy

**Files:**
- Modify: `src/main/kotlin/me/leonunes/games/rooksandwalls/ai/MctsAiStrategy.kt`
- Create: `src/test/kotlin/me/leonunes/games/rooksandwalls/ai/MctsAiStrategyTest.kt`

- [ ] **Step 1: Write the failing integration test**

```kotlin
// src/test/kotlin/me/leonunes/games/rooksandwalls/ai/MctsAiStrategyTest.kt
package me.leonunes.games.rooksandwalls.ai

import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.coord
import me.leonunes.games.rooksandwalls.model.*
import kotlin.test.Test
import kotlin.test.assertIs

class MctsAiStrategyTest {

    @Test
    fun `chooseAction delegates to RandomAiStrategy during PiecePlacement stage`() {
        val game = createGameWithPlayers(GameConfig(numberOfPlayers = 2, piecesPerPlayer = 2, boardRows = 8, boardColumns = 8))
        val strategy = MctsAiStrategy(MctsConfig.EASY)
        val action = strategy.chooseAction(game, game.currentTurn!!.id)
        assertIs<AddPieceAction>(action)
    }

    @Test
    fun `chooseAction returns MoveAction during Moves stage`() {
        val game = createGameWithPlayers(GameConfig(numberOfPlayers = 2, piecesPerPlayer = 2, boardRows = 8, boardColumns = 8))
        game.runAddPieceActions()
        val strategy = MctsAiStrategy(MctsConfig.EASY)
        val action = strategy.chooseAction(game, game.currentTurn!!.id)
        assertIs<MoveAction>(action)
    }

    @Test
    fun `MCTS selects a wall that kills the opponent when given enough simulations`() {
        // Build a game where player 0's only good move is to place a wall that
        // closes a small region around player 1's lone piece
        //
        // Board setup (3x3):
        //   P0 at (2,2)  — safe in bottom-right
        //   P1 at (0,0)  — already enclosed on 2 sides (board edges)
        //   Existing walls: below (0,0) and right of (0,0) are already there except one
        //   Killing wall: right of (0,0) → closes 1-square region around P1
        //
        // With enough simulations MCTS should identify the killing wall as winning.
        val game = createGameWithPlayers(GameConfig(numberOfPlayers = 2, piecesPerPlayer = 1, boardRows = 3, boardColumns = 3))
        // Manually advance past placement by placing pieces at target positions
        game.processAction(AddPieceAction(game.player1, coord(2, 2)))
        game.processAction(AddPieceAction(game.player2, coord(0, 0)))
        // Now in Moves stage. Player 1 (index 0) acts first (depending on snake order).
        // Place walls to set up the near-terminal scenario — but let MCTS pick the final wall.
        // Place wall below P1's piece (so P1 is enclosed on top, left by board, and below by wall)
        game.processAction(MoveAction(game.currentTurn!!.id, null,
            WallPlacement(EdgeCoordinate(coord(0, 0), coord(1, 0)))))

        // Now it's the other player's turn. The killing wall closes P1 in a 1-square region.
        val playerId = game.currentTurn!!.id
        val killingWall = EdgeCoordinate(coord(0, 0), coord(0, 1))

        val strategy = MctsAiStrategy(MctsConfig.HARD)  // 3000 sims
        val action = strategy.chooseAction(game, playerId) as MoveAction
        assert(action.wallPlacement.wallPosition == killingWall) {
            "Expected MCTS to select killing wall $killingWall but got ${action.wallPlacement.wallPosition}"
        }
    }
}
```

Note: the test setup depends on which player acts when, based on snake order. Adjust `game.currentTurn!!.id` calls accordingly if the test fails due to turn ordering. Use `game.player1` / `game.player2` extension properties from `GameTestHelper.kt` as needed.

- [ ] **Step 2: Run test to verify it fails**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.MctsAiStrategyTest"
```
Expected: FAIL with "Not implemented" (TODO stub) or similar

- [ ] **Step 3: Implement the MCTS loop in MctsAiStrategy**

Replace the stub in `MctsAiStrategy.kt`:

```kotlin
class MctsAiStrategy(private val config: MctsConfig) : AiStrategy {
    private val placement = RandomAiStrategy()

    override fun chooseAction(game: Game, playerId: PlayerId): GameAction =
        when (game.gameStage) {
            GameStage.PiecePlacement -> placement.chooseAction(game, playerId)
            GameStage.Moves -> chooseMoveAction(game, playerId)
            else -> throw IllegalStateException("AI asked to act in stage ${game.gameStage}")
        }

    private fun chooseMoveAction(game: Game, playerId: PlayerId): MoveAction {
        val simGame = SimGame.from(game)
        val root = MctsNode(simGame, move = null, parent = null)

        val startTime = System.currentTimeMillis()
        var simCount = 0

        while (simCount < config.simulationCount &&
            (config.timeoutMs == null || System.currentTimeMillis() - startTime < config.timeoutMs)) {
            runSimulation(root, config.playoutPolicy)
            simCount++
        }

        val bestMove = root.children.maxBy { it.visits }.move!!
        return bestMove.toMoveAction(game, playerId)
    }

    private fun runSimulation(root: MctsNode, policy: PlayoutPolicy) {
        // Phase 1: Selection
        var node = root
        while (node.isFullyExpanded && !node.state.isTerminal()) {
            node = node.bestChild()
        }

        // Phase 2: Expansion
        // Note: we add ONE policy-sampled child per expansion visit rather than enumerating
        // all legal moves. The branching factor (piece destinations × wall positions) can be
        // thousands, making full enumeration impractical. Each visit that reaches an
        // unexpanded node adds one new child until MAX_CHILDREN is reached. This is a
        // standard MCTS adaptation for games with large action spaces.
        if (!node.state.isTerminal()) {
            val move = policy.sampleMove(node.state)
            node = node.addChild(move)
        }

        // Phase 3: Simulation (playout)
        val simState = node.state.clone()
        while (!simState.isTerminal()) {
            simState.applyMove(policy.sampleMove(simState))
        }

        // Phase 4: Backpropagation
        val winner = simState.winner()
        var current: MctsNode? = node
        while (current != null) {
            current.visits++
            if (winner != null) current.wins[winner]++
            current = current.parent
        }
    }
}

// SimMove.pieceIndex is an index into the root SimGame.pieces list, which is built from
// game.pieces in order (no eliminations applied). The root SimGame.state is never mutated
// during MCTS (only clones are), so this index remains aligned with game.pieces at call time.
// Only use this function with moves produced from the root node's children.
private fun SimMove.toMoveAction(game: Game, playerId: PlayerId): MoveAction {
    val pieceMovement = if (pieceIndex != null && destination != null) {
        PieceMovement(game.pieces[pieceIndex].id, destination)
    } else null
    return MoveAction(playerId, pieceMovement, WallPlacement(wallPosition))
}
```

- [ ] **Step 4: Run all tests**

```
./gradlew test --tests "me.leonunes.games.rooksandwalls.ai.MctsAiStrategyTest"
```
Expected: PASS

- [ ] **Step 5: Run the full test suite to check for regressions**

```
./gradlew test
```
Expected: all PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/me/leonunes/games/rooksandwalls/ai/MctsAiStrategy.kt \
        src/test/kotlin/me/leonunes/games/rooksandwalls/ai/MctsAiStrategyTest.kt
git commit -m "feat: implement MctsAiStrategy MCTS loop"
```

---

## Chunk 3: API Integration

### Task 9: Non-blocking AiPlayerRunner

**Files:**
- Modify: `src/main/kotlin/me/leonunes/games/rooksandwalls/ai/AiPlayerRunner.kt`

- [ ] **Step 1: Update maybeAct to use withContext(Dispatchers.Default)**

```kotlin
// Full updated file:
package me.leonunes.games.rooksandwalls.ai

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.leonunes.games.rooksandwalls.model.Game
import me.leonunes.games.rooksandwalls.model.GameStage
import me.leonunes.games.rooksandwalls.model.PlayerId

class AiPlayerRunner(
    private val game: Game,
    private val playerId: PlayerId,
    private val strategy: AiStrategy
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            val channel = game.createUpdatesChannel()
            try {
                maybeAct()
                for (update in channel) {
                    maybeAct()
                }
            } finally {
                channel.cancel()
            }
        }
    }

    private suspend fun maybeAct() {
        if (game.gameStage == GameStage.Completed) return
        if (game.gameStage == GameStage.WaitingForPlayers) return
        if (game.currentTurn?.id != playerId) return
        val action = withContext(Dispatchers.Default) {
            strategy.chooseAction(game, playerId)
        }
        game.processAction(action)
    }
}
```

- [ ] **Step 2: Run all tests to confirm no regressions**

```
./gradlew test
```
Expected: all PASS

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/me/leonunes/games/rooksandwalls/ai/AiPlayerRunner.kt
git commit -m "feat: run AI chooseAction on Dispatchers.Default to avoid blocking WebSocket coroutine"
```

---

### Task 10: Add difficulty to the AI player endpoint

**Files:**
- Modify: `src/main/kotlin/me/leonunes/games/plugins/Game.kt`

- [ ] **Step 1: Update the routing to accept a difficulty field**

In `Game.kt`, make these two changes:

1. Add `AddAiPlayerRequestBody` with a `difficulty` field (after the existing `AddAiResponse` class):

```kotlin
@Serializable
class AddAiPlayerRequestBody(val difficulty: AiDifficulty = AiDifficulty.RANDOM)
```

2. Update the `post<AddAiPlayerRequest>` handler to read the body and pass the strategy:

```kotlin
post<AddAiPlayerRequest> { request ->
    val gameId: GameId = request.gameId.asId()
    val manager = AppDependencies.gameManagerFactory.getManager(gameId)
    if (manager == null) {
        call.respond(HttpStatusCode.NotFound)
        return@post
    }
    val body = runCatching { call.receive<AddAiPlayerRequestBody>() }.getOrNull()
    val strategy = (body?.difficulty ?: AiDifficulty.RANDOM).toAiStrategy()
    try {
        val gameView = manager.addAiPlayer(AppDependencies.coroutineScope, strategy)
        logger.info { "Bot added successfully to game $gameId with difficulty ${body?.difficulty ?: AiDifficulty.RANDOM}" }
        call.respond(AddAiResponse(playerId = gameView.player.id.get(), displayName = gameView.player.displayName))
    } catch (e: GameFullException) {
        call.respond(HttpStatusCode.Conflict, "Game is full")
    } catch (e: GameAlreadyStartedException) {
        call.respond(HttpStatusCode.Conflict, "Game has already started")
    }
}
```

3. Add the import at the top of `Game.kt`:

```kotlin
import me.leonunes.games.rooksandwalls.ai.AiDifficulty
```

- [ ] **Step 2: Run the full test suite**

```
./gradlew test
```
Expected: all PASS

- [ ] **Step 3: Smoke test the endpoint manually (optional)**

```bash
# Create a game
curl -s -X POST http://localhost:5000/rw/game/ | jq .

# Add a HARD AI player
curl -s -X POST http://localhost:5000/rw/game/0/ai \
  -H "Content-Type: application/json" \
  -d '{"difficulty": "HARD"}' | jq .

# Omitting the body still works (defaults to RANDOM)
curl -s -X POST http://localhost:5000/rw/game/1/ai | jq .
```

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/me/leonunes/games/plugins/Game.kt
git commit -m "feat: accept difficulty field on POST /rw/game/{gameId}/ai endpoint"
```

---

## Reminder

> At the end of this implementation, revisit the piece placement strategy. Currently, `MctsAiStrategy` delegates placement to `RandomAiStrategy`. Consider whether running MCTS during placement — or at least a heuristic (e.g., spread pieces out to maximize initial region size) — would improve play quality.
