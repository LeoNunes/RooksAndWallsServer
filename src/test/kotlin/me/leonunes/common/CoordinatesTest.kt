package me.leonunes.common

import kotlin.test.*

class CoordinatesTest {
    @Test
    fun `coord generate Coordinates correctly`() {
        assertEquals(
            SquareCoordinate(3, 8),
            coord(3, 8)
        )

        assertEquals(
            SquareCoordinateStep(3, -5),
            coordStep(3, -5)
        )
    }

    @Test
    fun `SquareCoordinate isAdjacentTo works correctly`() {
        val coordinate = coord(5, 5)
        assertTrue(coordinate.isAdjacentTo(coord(5, 4)))
        assertTrue(coordinate.isAdjacentTo(coord(5, 6)))
        assertTrue(coordinate.isAdjacentTo(coord(4, 5)))
        assertTrue(coordinate.isAdjacentTo(coord(6, 5)))
        assertFalse(coordinate.isAdjacentTo(coord(5, 5)))
        assertFalse(coordinate.isAdjacentTo(coord(6, 6)))
        assertFalse(coordinate.isAdjacentTo(coord(7, 3)))
    }

    @Test
    fun `EdgeCoordinate throws if coordinates are not adjacent`() {
        assertFailsWith<IllegalArgumentException> {
            EdgeCoordinate(coord(2, 5), coord(3, 6))
        }
    }

    @Test
    fun `EdgeCoordinate equality`() {
        assertEquals(
            EdgeCoordinate(coord(4, 2), coord(3, 2)),
            EdgeCoordinate(coord(4, 2), coord(3, 2))
        )

        assertEquals(
            EdgeCoordinate(coord(4, 2), coord(3, 2)),
            EdgeCoordinate(coord(3, 2), coord(4, 2))
        )

        assertNotEquals(
            EdgeCoordinate(coord(4, 2), coord(3, 2)),
            EdgeCoordinate(coord(4, 2), coord(5, 2))
        )
    }

    @Test
    fun `SquareCoordinate operations work correctly`() {
        assertEquals(
            coord(3, 8),
            coord(1, 5) + coordStep(2, 3)
        )
        assertEquals(
            coord(3, -2),
            coordStep(1, -4) + coord(2, 2)
        )
        assertEquals(
            coordStep(4, 2),
            coord(10, 0) - coord(6, -2)
        )
    }

    @Test
    fun `shortcut to generate EdgeCoordinate from SquareCoordinate works`() {
        assertEquals(
            EdgeCoordinate(coord(3, 5), coord(4, 5)),
            coord(3, 5).edgeDown()
        )
        assertEquals(
            EdgeCoordinate(coord(3, 5), coord(4, 5)),
            coord(4, 5).edgeUp()
        )
        assertEquals(
            EdgeCoordinate(coord(5, 4), coord(5, 5)),
            coord(5, 4).edgeRight()
        )
        assertEquals(
            EdgeCoordinate(coord(5, 4), coord(5, 5)),
            coord(5, 5).edgeLeft()
        )
    }
}
