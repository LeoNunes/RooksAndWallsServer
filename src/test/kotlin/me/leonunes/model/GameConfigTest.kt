package me.leonunes.model

import me.leonunes.rooksandwalls.model.GameConfig
import me.leonunes.rooksandwalls.model.GameConfigDefaultValues
import me.leonunes.rooksandwalls.model.InvalidConfigurationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GameConfigTest {
    @Test
    fun `GameConfig uses default values for null parameters`() {
        val config = GameConfig()

        assertEquals(GameConfigDefaultValues.numberOfPlayers, config.numberOfPlayers)
        assertEquals(GameConfigDefaultValues.piecesPerPlayer, config.piecesPerPlayer)
        assertEquals(GameConfigDefaultValues.boardRows, config.boardRows)
        assertEquals(GameConfigDefaultValues.boardColumns, config.boardColumns)
    }

    @Test
    fun `Config must be valid`() {
        assertFailsWith<InvalidConfigurationException> {
            GameConfig(numberOfPlayers = 1)
        }
        assertFailsWith<InvalidConfigurationException> {
            GameConfig(piecesPerPlayer = 0)
        }
        assertFailsWith<InvalidConfigurationException> {
            GameConfig(boardRows = 0)
        }
        assertFailsWith<InvalidConfigurationException> {
            GameConfig(boardColumns = 0)
        }
    }

    @Test
    fun `can compare configs`() {
        assertEquals(GameConfigDefaultValues, GameConfig())
        assertEquals(GameConfig(2, 5, 10, 18), GameConfig(2, 5, 10, 18))
        assertEquals(GameConfigDefaultValues.hashCode(), GameConfig().hashCode())
        assertEquals(GameConfig(2, 5, 10, 18).hashCode(), GameConfig(2, 5, 10, 18).hashCode())
    }
}
