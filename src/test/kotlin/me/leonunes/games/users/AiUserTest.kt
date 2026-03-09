package me.leonunes.games.users

import kotlin.test.Test
import kotlin.test.assertEquals

class AiUserTest {
    @Test
    fun `AiUser has correct id and displayName`() {
        val user = AiUser(id = "ai-3-0")
        assertEquals("ai-3-0", user.id)
        assertEquals("AI", user.displayName)
    }

    @Test
    fun `AiUser implements User interface`() {
        val user: User = AiUser(id = "ai-1-0")
        assertEquals("ai-1-0", user.id)
    }
}
