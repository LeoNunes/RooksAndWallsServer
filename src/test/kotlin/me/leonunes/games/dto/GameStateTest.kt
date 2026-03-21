package me.leonunes.games.dto

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import me.leonunes.games.common.player.EdgeCoordinate
import me.leonunes.games.common.player.asId
import me.leonunes.games.common.player.coord
import me.leonunes.games.common.player.ConnectionStatus
import me.leonunes.games.common.player.Player
import me.leonunes.games.rooksandwalls.model.*
import me.leonunes.games.users.GuestUserImpl
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class GameStateTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @Test
    fun `Game getStateDto works properly`() {
        val gameView = mockk<GameView>()
        val board = mockk<Board>()
        val config = GameConfigDefaultValues
        val player1 = Player(GuestUserImpl("player-0"), 0)
        val player2 = Player(GuestUserImpl("player-1"), 1)
        val players = listOf(player1, player2)
        val pieces = listOf(
            Piece(0.asId(), 0, coord(0, 0), board),
            Piece(1.asId(), 0, coord(1, 3), board),
            Piece(2.asId(), 0, coord(2, 5), board),
            Piece(3.asId(), 1, coord(8, 4), board),
            Piece(4.asId(), 1, coord(3, 1), board),
            Piece(5.asId(), 1, coord(5, 2), board)
        )
        val walls = listOf(
            Wall(EdgeCoordinate(coord(2, 3), coord(3, 3))),
            Wall(EdgeCoordinate(coord(5, 1), coord(5, 0))),
            Wall(EdgeCoordinate(coord(7, 2), coord(6, 2))),
        )
        val deadPieces = listOf(
            Piece(6.asId(), 0, coord(7, 7), board),
            Piece(7.asId(), 0, coord(4, 7), board),
            Piece(8.asId(), 1, coord(6, 5), board),
        )

        every { gameView.player } returns player1
        every { gameView.id } returns "20".asId<Game, String>()
        every { gameView.config } returns config
        every { gameView.gameStage } returns GameStage.PiecePlacement
        every { gameView.currentTurn } returns 1
        every { gameView.players } returns players
        every { gameView.remainingPlayers } returns listOf(0, 1)
        every { gameView.pieces } returns pieces
        every { gameView.walls } returns walls
        every { gameView.deadPieces } returns deadPieces

        val dto = gameView.getStateDto()
        assertEquals(player1.playerNumber, dto.playerNumber)
        assertEquals("20", dto.gameId)
        assert(dto.config.numberOfPlayers == config.numberOfPlayers)
        assert(dto.config.piecesPerPlayer == config.piecesPerPlayer)
        assert(dto.config.boardRows == config.boardRows)
        assert(dto.config.boardColumns == config.boardColumns)
        assertEquals(GameStage.PiecePlacement, dto.stage)
        assertEquals(1, dto.currentTurn)
        assertEquals(
            players.map { PlayerDTO(it.playerNumber, it.user.id, "Guest", ConnectionStatus.Connected) }.toSet(),
            dto.players.toSet()
        )
        assertEquals(listOf(0, 1).toSet(), dto.remainingPlayers.toSet())
        assertEquals(
            pieces.map { PieceDTO(it.id.get(), it.owner, it.position) }.toSet(),
            dto.pieces.toSet()
        )
        assertEquals(
            walls.map { WallDTO(it.position) }.toSet(),
            dto.walls.toSet()
        )
        assertEquals(
            deadPieces.map { PieceDTO(it.id.get(), it.owner, it.position) }.toSet(),
            dto.deadPieces.toSet()
        )

        // This is needed, even though no statics/objects/constructor are being mocked, because the mocks are "leaking"
        // to other tests and confirmVerified() fails, as the mocks in this test where not verified
        // https://github.com/mockk/mockk/issues/821
        clearAllMocks()
    }
}
