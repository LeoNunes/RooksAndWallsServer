package me.leonunes.games.rooksandwalls.ai

import kotlinx.coroutines.runBlocking
import me.leonunes.games.common.coord
import me.leonunes.games.rooksandwalls.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RandomAiStrategyTest {
    private val strategy = RandomAiStrategy()

    @Test
    fun `chooseAction returns AddPieceAction during piece placement`() = runBlocking {
        val game = createGameWithPlayers(GameConfig(numberOfPlayers = 2, piecesPerPlayer = 2, boardRows = 8, boardColumns = 8))
        val playerId = game.player1

        val action = strategy.chooseAction(game, playerId)

        assertIs<AddPieceAction>(action)
        assertEquals(playerId, action.playerId)
    }

    @Test
    fun `AddPieceAction targets an empty square`() = runBlocking {
        val game = createGameWithPlayers(GameConfig(numberOfPlayers = 2, piecesPerPlayer = 2, boardRows = 8, boardColumns = 8))
        val playerId = game.player1

        val action = strategy.chooseAction(game, playerId) as AddPieceAction

        assertTrue(game.pieces.none { it.position == action.position })
    }

    @Test
    fun `chooseAction returns MoveAction during moves stage`() = runBlocking {
        val game = createGameWithPlayers(GameConfig(numberOfPlayers = 2, piecesPerPlayer = 2, boardRows = 8, boardColumns = 8))
        // Place all pieces to enter Moves stage
        repeat(4) { i ->
            val playerId = game.currentTurn!!.id
            game.processAction(AddPieceAction(playerId, coord(i, 0)))
        }

        val playerId = game.currentTurn!!.id
        val action = strategy.chooseAction(game, playerId)

        assertIs<MoveAction>(action)
        assertEquals(playerId, action.playerId)
    }

    @Test
    fun `MoveAction wall is not already occupied`() = runBlocking {
        val game = createGameWithPlayers(GameConfig(numberOfPlayers = 2, piecesPerPlayer = 2, boardRows = 8, boardColumns = 8))
        repeat(4) { i ->
            val playerId = game.currentTurn!!.id
            game.processAction(AddPieceAction(playerId, coord(i, 0)))
        }

        val playerId = game.currentTurn!!.id
        val action = strategy.chooseAction(game, playerId) as MoveAction

        assertTrue(game.walls.none { it.position == action.wallPlacement.wallPosition })
    }
}
