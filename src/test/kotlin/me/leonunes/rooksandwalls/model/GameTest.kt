package me.leonunes.rooksandwalls.model

import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import me.leonunes.assertEach
import me.leonunes.common.EdgeCoordinate
import me.leonunes.common.coord
import me.leonunes.common.edgeUp
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
    fun `player cant join if game is full`() : Unit = runBlocking {
        with(createGameWithPlayers()) {
            assertFailsWith<GameFullException> {
                joinGame()
            }
        }
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
    fun `player cant add a piece outside of the board`() : Unit = runBlocking {
        with(createGameWithPlayers()) {
            assertFailsWith<InvalidActionException> {
                processAction(AddPieceAction(player1, coord(8, 4)))
            }
        }
    }

    @Test
    fun `move stage starting player depends on the placement piece player order`() = runBlocking {
        with(createGameWithPlayers(GameConfig(piecesPerPlayer = 2))) {
            listOf(
                AddPieceAction(player1, coord(0, 0)),
                AddPieceAction(player2, coord(0, 1)),
                AddPieceAction(player3, coord(0, 2)),
                AddPieceAction(player3, coord(0, 3)),
                AddPieceAction(player2, coord(0, 4)),
                AddPieceAction(player1, coord(0, 5)),
            ).forEach { processAction(it) }

            assertEquals(player3, currentTurn?.id)
            processAction(MoveAction(player3, piece3_1, coord(1, 2), coord(7, 7).edgeUp()))
            assertEquals(player2, currentTurn?.id)
            processAction(MoveAction(player2, piece2_1, coord(1, 1), coord(7, 6).edgeUp()))
            assertEquals(player1, currentTurn?.id)
        }

        with (createGameWithPlayers(GameConfig(piecesPerPlayer = 1))){
            listOf(
                AddPieceAction(player1, coord(0, 0)),
                AddPieceAction(player2, coord(0, 1)),
                AddPieceAction(player3, coord(0, 2)),
            ).forEach { processAction(it) }

            assertEquals(player1, currentTurn?.id)
            processAction(MoveAction(player1, piece1_1, coord(1, 0), coord(7, 7).edgeUp()))
            assertEquals(player2, currentTurn?.id)
            processAction(MoveAction(player2, piece2_1, coord(1, 1), coord(7, 6).edgeUp()))
            assertEquals(player3, currentTurn?.id)
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
    fun `player cant move pieces owned by other players`() : Unit = runBlocking {
        with(createGameWithPlayers()) {
            runAddPieceActions()

            assertFailsWith<InvalidActionException> {
                processAction(MoveAction(player1, piece2_3, coord(4, 7), EdgeCoordinate(coord(5, 5), coord(5, 6))))
            }
        }
    }

    @Test
    @Ignore
    fun `player can skip moving a piece if there is no valid destination`() = runBlocking {
        with(createGameWithPlayers()) {
            listOf(
                AddPieceAction(player1, coord(0, 0)),
                AddPieceAction(player2, coord(1, 0)),
                AddPieceAction(player3, coord(2, 0)),
                AddPieceAction(player3, coord(2, 1)),
                AddPieceAction(player2, coord(1, 1)),
                AddPieceAction(player1, coord(0, 1)),
                AddPieceAction(player1, coord(0, 2)),
                AddPieceAction(player2, coord(1, 2)),
                AddPieceAction(player3, coord(0, 3)),
            ).forEach { processAction(it) }

            //processAction(MoveAction(player1, null, null, coord(5, 5).edgeDown()))
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

    @Test
    fun `game can be configured`() {
        assertEquals(GameConfigDefaultValues, GameFactory.createGame().config)

        assertEquals(
            GameConfig(
                numberOfPlayers = 5,
                piecesPerPlayer = 2,
                boardRows = 4,
                boardColumns = GameConfigDefaultValues.boardColumns
            ),
            GameFactory.createGame(GameConfig(numberOfPlayers = 5, piecesPerPlayer = 2, boardRows = 4)).config
        )
    }

    @Test
    fun `game can be configured for other number of players`() : Unit = runBlocking {
        with(createGameWithPlayers(GameConfig(numberOfPlayers = 4))) {
            assertEquals(4, players.size)
            assertFailsWith<GameFullException> { joinGame() }
        }
    }

    @Test
    fun `game can be configured for other number of pieces per player`() : Unit = runBlocking {
        with(createGameWithPlayers(GameConfig(piecesPerPlayer = 2))) {
            listOf(
                AddPieceAction(player1, coord(0, 0)),
                AddPieceAction(player2, coord(4, 0)),
                AddPieceAction(player3, coord(7, 0)),
                AddPieceAction(player3, coord(7, 1)),
                AddPieceAction(player2, coord(4, 1)),
                AddPieceAction(player1, coord(0, 1)),
            ).forEach { processAction(it) }

            assertEquals(6, pieces.size)
            assertEquals(GameStage.Moves, gameStage)
        }
    }

    @Test
    fun `game can be configured for other board sizes`() = runBlocking {
        with(createGameWithPlayers(GameConfig(boardRows = 4, boardColumns = 5))) {
            processAction(AddPieceAction(player1, coord(3, 4)))

            assertFailsWith<InvalidActionException> {
                processAction(AddPieceAction(player2, coord(4, 0)))
            }
            assertFailsWith<InvalidActionException> {
                processAction(AddPieceAction(player2, coord(3, 5)))
            }

            processAction(AddPieceAction(player2, coord(3, 3)))
        }
    }
}
