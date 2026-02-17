package me.leonunes.games.common

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerTurnOrderTest {
    @Test
    fun `sequentialPlayerTurnOrder works properly with default configuration`() {
        val turnOrder = sequentialPlayerTurnOrder(3).asSequence().take(10).toList()

        assertEquals(listOf(0, 1, 2, 0, 1, 2, 0, 1, 2, 0), turnOrder)
    }

    @Test
    fun `sequentialPlayerTurnOrder works properly with limited turns`() {
        val turnOrder = sequentialPlayerTurnOrder(3, 7).asSequence().toList()

        assertEquals(listOf(0, 1, 2, 0, 1, 2, 0), turnOrder)
    }

    @Test
    fun `sequentialPlayerTurnOrder works properly with different starting player`() {
        val turnOrder = sequentialPlayerTurnOrder(3, startPlayer = 2).asSequence().take(10).toList()

        assertEquals(listOf(2, 0, 1, 2, 0, 1, 2, 0, 1, 2), turnOrder)
    }

    @Test
    fun `sequentialPlayerTurnOrder works properly when reversed`() {
        val turnOrder = sequentialPlayerTurnOrder(3, startPlayer = 1, reversed = true).asSequence().take(10).toList()

        assertEquals(listOf(1, 0, 2, 1, 0, 2, 1, 0, 2, 1), turnOrder)
    }

    @Test
    fun `alternatingSequencePlayerTurnOrder works properly with default configuration`() {
        val turnOrder = alternatingSequencePlayerTurnOrder(3).asSequence().take(10).toList()

        assertEquals(listOf(0, 1, 2, 2, 1, 0, 0, 1, 2, 2), turnOrder)
    }

    @Test
    fun `alternatingSequencePlayerTurnOrder works properly with limited turns`() {
        val turnOrder = alternatingSequencePlayerTurnOrder(3, 7).asSequence().toList()

        assertEquals(listOf(0, 1, 2, 2, 1, 0, 0), turnOrder)
    }

    @Test
    fun `alternatingSequencePlayerTurnOrder works properly with different starting player`() {
        val turnOrder = alternatingSequencePlayerTurnOrder(3, startPlayer = 2).asSequence().take(10).toList()

        assertEquals(listOf(2, 0, 1, 1, 0, 2, 2, 0, 1, 1), turnOrder)
    }
}
