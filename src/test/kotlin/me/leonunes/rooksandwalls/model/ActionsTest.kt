package me.leonunes.rooksandwalls.model

import io.mockk.confirmVerified
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.verify
import me.leonunes.common.EdgeCoordinate
import me.leonunes.common.asId
import me.leonunes.common.coord
import org.junit.Rule
import kotlin.test.Test

class ActionsTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @Test
    fun `AddPieceAction works properly`() {
        val game = mockk<GameImp>(relaxUnitFun = true)
        val action = AddPieceAction(3.asId(), coord(2, 5))

        action.process(game)

        verify(exactly = 1) { game.addPiece(3.asId(), coord(2, 5)) }

        confirmVerified(game)
    }

    @Test
    fun `MoveAction works properly`() {
        val game = mockk<GameImp>(relaxUnitFun = true)
        val action = MoveAction(3.asId(), 5.asId(), coord(2, 5), EdgeCoordinate(coord(2, 5), coord(3, 5)))

        action.process(game)

        verify(exactly = 1) { game.move(3.asId(), 5.asId(), coord(2, 5), EdgeCoordinate(coord(2, 5), coord(3, 5))) }

        confirmVerified(game)
    }
}
