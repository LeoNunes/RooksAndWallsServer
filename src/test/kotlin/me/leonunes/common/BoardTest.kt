package me.leonunes.common

import kotlin.test.*

class BoardTest {
    @Test
    fun `GridBoard isInsideBoard works correctly`() {
        val board = object : GridBoard {
            override val rows = 8
            override val columns = 8
            override val pieces = emptyList<BoardPlaceable<SquareCoordinate>>()
        }

        assertTrue(board.isInsideBoard(SquareCoordinate(3, 6)))
        assertTrue(board.isInsideBoard(SquareCoordinate(0, 0)))
        assertTrue(board.isInsideBoard(SquareCoordinate(7, 7)))
        assertFalse(board.isInsideBoard(SquareCoordinate(-1, 2)))
        assertFalse(board.isInsideBoard(SquareCoordinate(3, 8)))
    }

    @Test
    fun `GridBoardWithWalls isInsideBoard works correctly`() {
        val board = object : GridBoardWithWalls {
            override val rows = 8
            override val columns = 8
            override val pieces = emptyList<BoardPlaceable<SquareCoordinate>>()
            override val walls = emptyList<BoardPlaceable<EdgeCoordinate>>()
        }

        assertTrue(board.isInsideBoard(EdgeCoordinate(SquareCoordinate(5, 5), SquareCoordinate(5, 6))))
        assertTrue(board.isInsideBoard(EdgeCoordinate(SquareCoordinate(0, 0), SquareCoordinate(0, 1))))
        assertTrue(board.isInsideBoard(EdgeCoordinate(SquareCoordinate(7, 7), SquareCoordinate(7, 6))))
        assertFalse(board.isInsideBoard(EdgeCoordinate(SquareCoordinate(7, 7), SquareCoordinate(7, 8))))
        assertFalse(board.isInsideBoard(EdgeCoordinate(SquareCoordinate(-1, 0), SquareCoordinate(0, 0))))
    }
}