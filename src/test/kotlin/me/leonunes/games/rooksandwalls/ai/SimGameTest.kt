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

    @Test
    fun `rookDestinations returns all reachable squares in open row and column`() {
        val sim = SimGame(
            rows = 8, columns = 8, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(4, 4)), SimPiece(1, coord(0, 0))),
            wallSet = mutableSetOf(), eliminatedPlayers = mutableSetOf()
        )
        val dests = sim.rookDestinations(coord(4, 4))
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
        // 4x4 board. (0,0) is enclosed by: top board edge, left board edge,
        // wall below (row0→row1 on col0), and wall right (col0→col1 on row0).
        // That leaves a 1-square region at (0,0) while the rest (15 squares) is > 8.
        val sim = SimGame(
            rows = 4, columns = 4, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(0, 0)), SimPiece(1, coord(3, 3))),
            wallSet = mutableSetOf(
                EdgeCoordinate(coord(0, 0), coord(1, 0)),  // below (0,0)
                EdgeCoordinate(coord(0, 0), coord(0, 1)),  // right of (0,0)
            ),
            eliminatedPlayers = mutableSetOf()
        )
        val safeWall = EdgeCoordinate(coord(1, 1), coord(2, 1))  // far from (0,0), region B stays size 15
        sim.applyMove(SimMove(null, null, safeWall))
        // Player 0's piece at (0,0) is in a 1-square region → eliminated; player 1 at (3,3) survives
        assertEquals(1, sim.pieces.size)
        assertEquals(coord(3, 3), sim.pieces[0].position)
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
}
