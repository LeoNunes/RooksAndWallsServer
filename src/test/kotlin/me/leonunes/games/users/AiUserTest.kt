package me.leonunes.games.users

import me.leonunes.games.rooksandwalls.ai.AiDifficulty
import me.leonunes.games.rooksandwalls.ai.getUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AiUserTest {
    @Test
    fun `bot users implement GuestUser`() {
        AiDifficulty.entries.forEach { difficulty ->
            assertIs<GuestUser>(difficulty.getUser())
        }
    }

    @Test
    fun `each difficulty produces a distinct display name`() {
        val names = AiDifficulty.entries.map { it.getUser().displayName }.toSet()
        assertEquals(AiDifficulty.entries.size, names.size)
    }

    @Test
    fun `each difficulty produces a distinct id`() {
        val ids = AiDifficulty.entries.map { it.getUser().id }.toSet()
        assertEquals(AiDifficulty.entries.size, ids.size)
    }
}
