package me.leonunes.dto

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import me.leonunes.common.EdgeCoordinate
import me.leonunes.common.asId
import me.leonunes.common.coord
import me.leonunes.rooksandwalls.model.Board
import me.leonunes.rooksandwalls.model.Game
import me.leonunes.rooksandwalls.model.GameConfigDefaultValues
import me.leonunes.rooksandwalls.model.GameStage
import me.leonunes.rooksandwalls.model.Piece
import me.leonunes.rooksandwalls.model.Player
import me.leonunes.rooksandwalls.model.Wall
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class GameStateTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @Test
    fun `Game getStateDto works properly`() {
        val game = mockk<Game>()
        val board = mockk<Board>()
        val config = GameConfigDefaultValues
        val player1 = Player(0.asId())
        val player2 = Player(1.asId())
        val players = listOf(player1, player2)
        val pieces = listOf(
            Piece(0.asId(), player1, coord(0, 0), board),
            Piece(1.asId(), player1, coord(1, 3), board),
            Piece(2.asId(), player1, coord(2, 5), board),
            Piece(3.asId(), player2, coord(8, 4), board),
            Piece(4.asId(), player2, coord(3, 1), board),
            Piece(5.asId(), player2, coord(5, 2), board)
        )
        val walls = listOf(
            Wall(EdgeCoordinate(coord(2, 3), coord(3, 3))),
            Wall(EdgeCoordinate(coord(5, 1), coord(5, 0))),
            Wall(EdgeCoordinate(coord(7, 2), coord(6, 2))),
        )
        val deadPieces = listOf(
            Piece(6.asId(), player1, coord(7, 7), board),
            Piece(7.asId(), player1, coord(4, 7), board),
            Piece(8.asId(), player2, coord(6, 5), board),
        )

        every { game.id } returns 20.asId()
        every { game.config } returns config
        every { game.gameStage } returns GameStage.PiecePlacement
        every { game.currentTurn } returns player2
        every { game.players } returns players
        every { game.pieces } returns pieces
        every { game.walls } returns walls
        every { game.deadPieces } returns deadPieces

        val dto = game.getStateDto(player1.id)
        assertEquals(player1.id.get(), dto.playerId)
        assertEquals(20, dto.gameId)
        assert(dto.config.numberOfPlayers == config.numberOfPlayers)
        assert(dto.config.piecesPerPlayer == config.piecesPerPlayer)
        assert(dto.config.boardRows == config.boardRows)
        assert(dto.config.boardColumns == config.boardColumns)
        assertEquals(GameStage.PiecePlacement, dto.stage)
        assertEquals(player2.id.get(), dto.currentTurn)
        assertEquals(
            players.map { PlayerDTO(it.id.get()) }.toSet(),
            dto.players.toSet()
        )
        assertEquals(
            pieces.map { PieceDTO(it.id.get(), it.owner.id.get(), it.position) }.toSet(),
            dto.pieces.toSet()
        )
        assertEquals(
            walls.map { WallDTO(it.position) }.toSet(),
            dto.walls.toSet()
        )
        assertEquals(
            deadPieces.map { PieceDTO(it.id.get(), it.owner.id.get(), it.position) }.toSet(),
            dto.deadPieces.toSet()
        )

        // This is needed, even though no statics/objects/constructor are being mocked, because the mocks are "leaking"
        // to other tests and confirmVerified() fails, as the mocks in this test where not verified
        // https://github.com/mockk/mockk/issues/821
        clearAllMocks()
    }
}
