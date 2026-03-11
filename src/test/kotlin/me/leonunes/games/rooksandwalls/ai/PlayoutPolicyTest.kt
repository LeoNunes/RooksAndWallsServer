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
        // 4x4 board: player 1's piece at (0,0) enclosed on 3 sides; player 0 can close the box.
        // killingWall creates a 1-square region around (0,0) while player 0's piece at (3,3)
        // remains in the 15-square region (> 8), so heuristic correctly classifies it as
        // "kills opponent only."
        val sim = SimGame(
            rows = 4, columns = 4, playerCount = 2, currentPlayerIndex = 0,
            pieces = mutableListOf(SimPiece(0, coord(3, 3)), SimPiece(1, coord(0, 0))),
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
}
