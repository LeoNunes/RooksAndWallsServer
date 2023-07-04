package me.leonunes.model

import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import me.leonunes.assertEach
import me.leonunes.common.coord
import org.junit.Rule
import kotlin.test.*

@ExperimentalCoroutinesApi
class GameTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @Test
    fun `game start in WaitingForPlayers stage`() {
        val game = GameFactory.createGame()

        assertEquals(GameStage.WaitingForPlayers, game.gameStage)
    }

    @Test
    fun `player can join game`() = runBlocking {
        val game = GameFactory.createGame()
        val playerId = game.joinGame()

        assertTrue(game.players.filter { it.id == playerId }.size == 1)
    }

    @Test
    @Ignore
    fun `player cant join if game is full`() {
        assertTrue(false)
    }

    @Test
    @Ignore
    fun `player can reconnect to game`() {
        assertTrue(false)
    }

    @Test
    fun `channels are notified when player joins`() = runBlocking {
        val game = GameFactory.createGame()
        val channel1 = game.createUpdatesChannel()
        val channel2 = game.createUpdatesChannel()

        game.joinGame()

        assertTrue(channel1.receiveInstant() != null)
        assertTrue(channel2.receiveInstant() != null)
    }

    @Test
    fun `game starts after all players join`() = runBlocking {
        val game = GameFactory.createGame()
        game.joinGame()
        game.joinGame()
        game.joinGame()

        assertEquals(GameStage.PiecePlacement, game.gameStage)
    }

    @Test
    fun `channels are notified when action is processed`() = runBlocking {
        val game = createGameWithPlayers()

        val channel1 = game.createUpdatesChannel()
        val channel2 = game.createUpdatesChannel()

        game.processAction(AddPieceAction(game.currentTurn!!.id, coord(0, 0)))

        assertTrue(channel1.receiveInstant() != null)
        assertTrue(channel2.receiveInstant() != null)
    }

    @Test
    fun `players can add pieces in alternating order`() = runBlocking {
        with(createGameWithPlayers()) {
            val actions = addPieceActions()

            actions.forEach { processAction(it) }

            assertEquals(GameStage.Moves, gameStage)
            assertEquals(actions.size, pieces.size)
            assertEach(actions) { act ->
                pieces.filter { it.owner.id == act.playerId && it.position == act.position }.size == 1
            }
        }
    }

    @Test
    fun `player cant add piece if its not their turn`() : Unit = runBlocking {
        with(createGameWithPlayers()) {
            val action = addPieceActions().drop(1).first()

            assertFailsWith<NotPlayersTurnException> { processAction(action) }
        }
    }

    @Test
    fun `players can move pieces`() = runBlocking {
        with(createGameWithPlayers()) {
            runAddPieceActions()

            val action = winningMovePieceActions().first()
            processAction(action)

            assertTrue(action.playerId != currentTurn!!.id)
            assertTrue(pieces.find {
                it.id == action.pieceId && it.position == action.piecePosition && it.owner.id == action.playerId
            } != null)
        }
    }

    @Test
    fun `player cant move if its not their turn`() : Unit = runBlocking {
        with(createGameWithPlayers()) {
            runAddPieceActions()

            val action = winningMovePieceActions().drop(1).first()

            assertFailsWith<NotPlayersTurnException> { processAction(action) }
        }
    }

    @Test
    fun `piece die if on a region with 8 squares or less`() = runBlocking {
        with (createGameWithPlayers()) {
            runAddPieceActions()
            winningMovePieceActions().take(8).forEach { processAction(it) }

            assertEquals(1, deadPieces.size)
            assertEquals(8, pieces.size)
            assertTrue(pieces.find { it.owner.id == players[0].id && it.position == coord(0, 5) } == null)
            assertTrue(deadPieces.find { it.owner.id == players[0].id && it.position == coord(0, 5) } != null)
        }
    }

    @Test
    fun `player is eliminated if all their pieces are dead`() = runBlocking {
        with(createGameWithPlayers()) {
            runAddPieceActions()
            winningMovePieceActions().take(9).forEach { processAction((it)) }

            assertEquals(3, deadPieces.size)
            assertEquals(6, pieces.size)
            assertTrue(pieces.none { it.owner == players[0] })
            assertTrue(deadPieces.all { it.owner == players[0] })
            assertTrue(deadPieces.distinctBy { it.position }.size == 3)
            assertTrue(remainingPlayers.none { it == players[0] })
            assertEquals(players[1], currentTurn)
        }
    }

    @Test
    @Ignore
    fun `player wins game after all other players are eliminated`() {
        with(createGameWithPlayers()) {
            runAddPieceActions()
            runWinningMovePieceActions()

            assertEquals(GameStage.Completed, gameStage)
            assertEquals(1, remainingPlayers.size)
            assertEquals(players[1], remainingPlayers[0])
            assertNull(currentTurn)
            assertEquals("result", "won")
        }
    }

    @Test
    @Ignore
    fun `game draws if every piece is eliminated`() {
        with(createGameWithPlayers()) {
            runAddPieceActions()
            runDrawingMovePieceActions()

            assertEquals(GameStage.Completed, gameStage)
            assertEquals(0, remainingPlayers.size)
            assertNull(currentTurn)
            assertEquals("result", "draw")
        }
    }

    @Test
    fun `channels are closed after game ends`() = runBlocking {
        with(createGameWithPlayers()) {
            val channel1 = createUpdatesChannel()
            val channel2 = createUpdatesChannel()

            runAddPieceActions()
            runWinningMovePieceActions()

            channel1.receiveInstant()
            channel2.receiveInstant()

            assertTrue(channel1.isClosedForReceive)
            assertTrue(channel2.isClosedForReceive)
        }
    }
}
