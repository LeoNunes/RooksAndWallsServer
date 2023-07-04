package me.leonunes.model

import io.mockk.*
import io.mockk.junit4.MockKRule
import me.leonunes.common.*
import org.junit.Rule
import kotlin.test.Test

class BoardTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @Test
    fun `Piece movement is defined correctly`() {
        mockkConstructor(StepValidationScope::class)
        mockkStatic(StepValidationScope<SquareCoordinate, Board>::validateBlockedByPieces)
        mockkStatic(StepValidationScope<SquareCoordinate, Board>::validateBlockedByWalls)
        mockkStatic(StepValidationScope<SquareCoordinate, Board>::validateInsideBoard)

        every { anyConstructed<StepValidationScope<SquareCoordinate, Board>>().validate(any()) } just Runs
        every { any<StepValidationScope<SquareCoordinate, Board>>().validateBlockedByPieces() } answers { callOriginal() }
        every { any<StepValidationScope<SquareCoordinate, Board>>().validateBlockedByWalls() } answers { callOriginal() }
        every { any<StepValidationScope<SquareCoordinate, Board>>().validateInsideBoard() } answers { callOriginal() }

        Piece(3.asId(), mockk<Player>(), coord(2, 5), mockk<Board>())

        verify(exactly = 1) { any<StepValidationScope<SquareCoordinate, Board>>().validateBlockedByPieces() }
        verify(exactly = 1) { any<StepValidationScope<SquareCoordinate, Board>>().validateBlockedByWalls() }
        verify(exactly = 1) { any<StepValidationScope<SquareCoordinate, Board>>().validateInsideBoard() }

        unmockkAll()
    }
}
