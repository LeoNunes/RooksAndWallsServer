// src/test/kotlin/me/leonunes/games/rooksandwalls/ai/MctsAiStrategyTest.kt
package me.leonunes.games.rooksandwalls.ai

import kotlinx.coroutines.runBlocking
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
        // Use the default 3-player config so runAddPieceActions() works correctly
        val game = createGameWithPlayers(GameConfigDefaultValues)
        game.runAddPieceActions()
        val strategy = MctsAiStrategy(MctsConfig.EASY)
        val action = strategy.chooseAction(game, game.currentTurn!!.id)
        assertIs<MoveAction>(action)
    }

    @Test
    fun `MCTS selects a wall that kills the opponent when given enough simulations`() {
        // Setup (4x4 board, 2 players, 1 piece each):
        //   Player 0 piece: placed at (0,2)
        //   Player 1 piece: placed at (0,0)
        //
        // Round 1 — Player 0: moves (0,2) → (0,1) [blocks player 1's rightward escape],
        //   places wall (0,0)-(1,0) [below player 1's piece].
        //   After this, player 1's piece at (0,0) has NO legal moves:
        //     left/up → board edges, right → player 0's piece at (0,1), down → wall
        //
        // Round 1 — Player 1: no legal piece moves → null pieceMovement + any safe wall.
        //
        // Round 2 — Player 0: killing wall (0,0)-(0,1) closes a 1-square region around (0,0)
        //   → player 1 eliminated → game over immediately (player 0 is sole survivor).
        //   This is an instant win, so all MCTS rollouts that pick this wall show 100% win rate.
        //   Even MctsConfig.EASY (50 sims) reliably converges on it.

        val game = createGameWithPlayers(GameConfig(numberOfPlayers = 2, piecesPerPlayer = 1, boardRows = 4, boardColumns = 4))
        runBlocking {
            // Piece placement: player 0 first, then player 1
            game.processAction(AddPieceAction(game.player1, coord(0, 2)))
            game.processAction(AddPieceAction(game.player2, coord(0, 0)))

            // Round 1, Player 0: move (0,2)→(0,1), place wall below (0,0)
            val piece0 = game.pieces.find { it.owner.id == game.player1 }!!
            game.processAction(MoveAction(game.player1,
                PieceMovement(piece0.id, coord(0, 1)),
                WallPlacement(EdgeCoordinate(coord(0, 0), coord(1, 0)))))

            // Round 1, Player 1: (0,0) is fully trapped → no legal piece moves
            game.processAction(MoveAction(game.player2, null,
                WallPlacement(EdgeCoordinate(coord(2, 2), coord(2, 3)))))
        }

        // Round 2, Player 0: MCTS should find the killing wall (0,0)-(0,1).
        // Placing it encloses (0,0) in a 1-square region → player 1 immediately eliminated → instant win.
        val playerId = game.currentTurn!!.id
        val killingWall = EdgeCoordinate(coord(0, 0), coord(0, 1))

        val strategy = MctsAiStrategy(MctsConfig.EASY)  // 50 sims is enough: 100% win on this wall
        val action = strategy.chooseAction(game, playerId) as MoveAction
        assert(action.wallPlacement.wallPosition == killingWall) {
            "Expected MCTS to select killing wall $killingWall but got ${action.wallPlacement.wallPosition}"
        }
    }
}
