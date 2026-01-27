package me.leonunes.model

import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.unmockkAll
import me.leonunes.common.*
import me.leonunes.rooksandwalls.model.Board
import me.leonunes.rooksandwalls.model.Piece
import me.leonunes.rooksandwalls.model.Player
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BoardTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @Test
    fun `Piece movement is defined correctly`() {
        val piece = Piece(3.asId(), mockk<Player>(), coord(2, 5), mockk<Board>())

        assertEquals(3, piece.movement.validations.size)
        assertNotNull(piece.movement.validations.find { it is InsideBoardStepValidation })
        assertNotNull(piece.movement.validations.find { it is BlockedByPiecesStepValidation })
        assertNotNull(piece.movement.validations.find { it is BlockedByWallsStepValidation<*, *, *> })

        unmockkAll()
    }
}
