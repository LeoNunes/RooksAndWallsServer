package me.leonunes.dto

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import me.leonunes.common.*
import me.leonunes.model.*
import me.leonunes.model.Board
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
        every { game.gameStage } returns GameStage.PiecePlacement
        every { game.currentTurn } returns player2
        every { game.players } returns players
        every { game.pieces } returns pieces
        every { game.walls } returns walls
        every { game.deadPieces } returns deadPieces

        val dto = game.getStateDto(player1.id)
        assertEquals(player1.id.get(), dto.playerId)
        assertEquals(20, dto.gameId)
        assertEquals(GameStage.PiecePlacement, dto.gameStage)
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
