package me.leonunes.games.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardTest {
    @Test
    fun `GridBoard isInsideBoard works correctly`() {
        val board = object : GridBoard {
            override val rows = 8
            override val columns = 8
        }

        assertTrue(board.isInsideBoard(SquareCoordinate(3, 6)))
        assertTrue(board.isInsideBoard(SquareCoordinate(0, 0)))
        assertTrue(board.isInsideBoard(SquareCoordinate(7, 7)))
        assertFalse(board.isInsideBoard(SquareCoordinate(-1, 2)))
        assertFalse(board.isInsideBoard(SquareCoordinate(3, 8)))
    }

    @Test
    fun `GridBoard isInsideBoard for EdgeCoordinate works correctly`() {
        val board = object : GridBoard {
            override val rows = 8
            override val columns = 8
        }

        assertTrue(board.isInsideBoard(EdgeCoordinate(SquareCoordinate(5, 5), SquareCoordinate(5, 6))))
        assertTrue(board.isInsideBoard(EdgeCoordinate(SquareCoordinate(0, 0), SquareCoordinate(0, 1))))
        assertTrue(board.isInsideBoard(EdgeCoordinate(SquareCoordinate(7, 7), SquareCoordinate(7, 6))))
        assertFalse(board.isInsideBoard(EdgeCoordinate(SquareCoordinate(7, 7), SquareCoordinate(7, 8))))
        assertFalse(board.isInsideBoard(EdgeCoordinate(SquareCoordinate(-1, 0), SquareCoordinate(0, 0))))
    }

    @Test
    fun `GridBoard allSquares works properly`() {
        val board = object : GridBoard {
            override val rows = 5
            override val columns = 4
        }

        assertEquals(
            setOf(coord(0, 0), coord(0, 1), coord(0, 2), coord(0, 3),
                  coord(1, 0), coord(1, 1), coord(1, 2), coord(1, 3),
                  coord(2, 0), coord(2, 1), coord(2, 2), coord(2, 3),
                  coord(3, 0), coord(3, 1), coord(3, 2), coord(3, 3),
                  coord(4, 0), coord(4, 1), coord(4, 2), coord(4, 3),
            ),
            board.allPositions()
        )
    }

    @Test
    fun `GridBoardWithWalls sliceIntoRegions works properly`() {
        val board = object : GridBoard, WithWalls<BoardPlaceable<EdgeCoordinate>> {
            override val rows = 4
            override val columns = 4
            override val walls = listOf(
                EdgeCoordinate(coord(0, 0), coord(0, 1)),
                EdgeCoordinate(coord(1, 0), coord(1, 1)),
                EdgeCoordinate(coord(2, 0), coord(2, 1)),
                EdgeCoordinate(coord(3, 1), coord(2, 1)),
                EdgeCoordinate(coord(3, 2), coord(2, 2)),
                EdgeCoordinate(coord(3, 3), coord(2, 3)),
                EdgeCoordinate(coord(0, 2), coord(0, 3)),
                EdgeCoordinate(coord(1, 2), coord(1, 3)),
                EdgeCoordinate(coord(2, 2), coord(2, 3)),
            ).map { object : BoardPlaceable<EdgeCoordinate> { override val position = it } }
        }

        assertEquals(
            setOf(
                setOf(coord(0, 0), coord(1, 0), coord(2, 0), coord(3, 0), coord(3, 1), coord(3, 2), coord(3, 3)),
                setOf(coord(0, 1), coord(1, 1), coord(2, 1), coord(2, 2), coord(1, 2), coord(0, 2)),
                setOf(coord(0, 3), coord(1, 3), coord(2, 3))
            ),
            board.sliceIntoRegions()
        )
    }
}
