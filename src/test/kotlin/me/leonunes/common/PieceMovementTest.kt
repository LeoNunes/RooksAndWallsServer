package me.leonunes.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PieceMovementTest {
    @Test
    fun `PieceMovementUnion works properly`() {
        val movement1 = object : PieceMovement<SquareCoordinate> {
            override fun getPossibleDestinations() = setOf(coord(0, 0), coord(1, 1), coord(2, 2))
        }

        val movement2 = object : PieceMovement<SquareCoordinate> {
            override fun getPossibleDestinations() = setOf(coord(0, 0), coord(0, 1), coord(0, 2))
        }

        val union = PieceMovementUnion(movement1, movement2)

        assertEquals(
            setOf(coord(0, 0), coord(0, 1), coord(0, 2), coord(1, 1), coord(2, 2)),
            union.getPossibleDestinations()
        )
        assertTrue(union.canMoveTo(coord(0, 0)))
        assertTrue(union.canMoveTo(coord(1, 1)))
        assertFalse(union.canMoveTo(coord(1, 2)))
    }

    @Test
    fun `PieceMovementIntersection works properly`() {
        val movement1 = object : PieceMovement<SquareCoordinate> {
            override fun getPossibleDestinations() = setOf(coord(0, 0), coord(1, 1), coord(2, 2))
        }

        val movement2 = object : PieceMovement<SquareCoordinate> {
            override fun getPossibleDestinations() = setOf(coord(0, 0), coord(0, 1), coord(0, 2))
        }

        val union = PieceMovementIntersection(movement1, movement2)

        assertEquals(
            setOf(coord(0, 0)),
            union.getPossibleDestinations()
        )
        assertTrue(union.canMoveTo(coord(0, 0)))
        assertFalse(union.canMoveTo(coord(1, 1)))
        assertFalse(union.canMoveTo(coord(0, 1)))
        assertFalse(union.canMoveTo(coord(1, 2)))
    }
}
