package me.leonunes.games.rooksandwalls.model

import kotlinx.coroutines.runBlocking
import me.leonunes.games.common.player.Player
import me.leonunes.games.common.player.PlayersManager
import me.leonunes.games.common.player.PlayersManagerFactory
import me.leonunes.games.common.player.PlayersManagerObserver
import me.leonunes.games.users.GuestUserImpl
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PlayerManagerFactoryTest {

    private val factory = PlayersManagerFactory()

    @Test
    fun `createPlayerManager returns a PlayersManager with the given numberOfPlayers`() {
        val pm = factory.createPlayerManager(3)

        assertEquals(3, pm.numberOfPlayers)
    }

    @Test
    fun `createPlayerManager returns a non-null PlayersManager`() {
        val pm = factory.createPlayerManager(2)

        assertNotNull(pm)
    }

    @Test
    fun `createPlayerManager produces independent managers`() = runBlocking {
        val pm1 = factory.createPlayerManager(2)
        val pm2 = factory.createPlayerManager(2)

        pm1.addPlayer(GuestUserImpl("u1"))

        assertEquals(1, pm1.players.size)
        assertEquals(0, pm2.players.size)
    }

    @Test
    fun `observers added after creation are notified`() = runBlocking {
        val added = mutableListOf<Player>()
        val pm = factory.createPlayerManager(2)
        pm.observe(object : PlayersManagerObserver {
            override suspend fun onPlayerAdded(player: Player) { added.add(player) }
        })

        pm.addPlayer(GuestUserImpl("u1"))

        assertEquals(1, added.size)
        assertEquals("u1", added[0].user.id)
    }
}
