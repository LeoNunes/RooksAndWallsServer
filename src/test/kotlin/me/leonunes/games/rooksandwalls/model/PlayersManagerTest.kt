package me.leonunes.games.rooksandwalls.model

import kotlinx.coroutines.runBlocking
import me.leonunes.games.common.player.ConnectionStatus
import me.leonunes.games.common.player.GameFullException
import me.leonunes.games.common.player.Player
import me.leonunes.games.common.player.PlayersManager
import me.leonunes.games.common.player.PlayersManagerObserver
import me.leonunes.games.common.player.UserNotInGameException
import me.leonunes.games.users.GuestUserImpl
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayersManagerTest {

    private fun manager(
        numberOfPlayers: Int = 2,
        onPlayerAdded: (Player) -> Unit = {},
        onPlayerConnected: (Player) -> Unit = {},
        onPlayerDisconnected: (Player) -> Unit = {}
    ) = PlayersManager(numberOfPlayers).apply {
        observe(object : PlayersManagerObserver {
            override suspend fun onPlayerAdded(player: Player) = onPlayerAdded(player)
            override suspend fun onPlayerConnected(player: Player) = onPlayerConnected(player)
            override suspend fun onPlayerDisconnected(player: Player) = onPlayerDisconnected(player)
        })
    }

    // addPlayer

    @Test
    fun `addPlayer adds a new player and returns it`() = runBlocking {
        val pm = manager()
        val user = GuestUserImpl("u1")

        val player = pm.addPlayer(user)

        assertEquals(user, player.user)
        assertEquals(1, pm.players.size)
    }

    @Test
    fun `addPlayer is idempotent for the same user`() = runBlocking {
        val pm = manager()
        val user = GuestUserImpl("u1")

        val first = pm.addPlayer(user)
        val second = pm.addPlayer(user)

        assertEquals(first, second)
        assertEquals(1, pm.players.size)
    }

    @Test
    fun `addPlayer throws GameFullException when capacity is reached`(): Unit = runBlocking {
        val pm = manager(numberOfPlayers = 1)
        pm.addPlayer(GuestUserImpl("u1"))

        assertFailsWith<GameFullException> {
            pm.addPlayer(GuestUserImpl("u2"))
        }
    }

    @Test
    fun `addPlayer invokes onPlayerAdded callback`() = runBlocking {
        val added = mutableListOf<Player>()
        val pm = manager(onPlayerAdded = { added.add(it) })

        pm.addPlayer(GuestUserImpl("u1"))

        assertEquals(1, added.size)
        assertEquals("u1", added[0].user.id)
    }

    @Test
    fun `addPlayer does not invoke onPlayerAdded for an existing player`() = runBlocking {
        val added = mutableListOf<Player>()
        val pm = manager(onPlayerAdded = { added.add(it) })
        val user = GuestUserImpl("u1")

        pm.addPlayer(user)
        pm.addPlayer(user)

        assertEquals(1, added.size)
    }

    // getPlayer

    @Test
    fun `getPlayer returns the player for a known user`() = runBlocking {
        val pm = manager()
        val user = GuestUserImpl("u1")
        pm.addPlayer(user)

        val player = pm.getPlayer(user)

        assertEquals(user, player.user)
    }

    @Test
    fun `getPlayer throws UserNotInGameException for an unknown user`(): Unit {
        val pm = manager()

        assertFailsWith<UserNotInGameException> {
            pm.getPlayer(GuestUserImpl("unknown"))
        }
    }

    // isGameFull

    @Test
    fun `isGameFull returns false when not all seats are taken`() = runBlocking {
        val pm = manager(numberOfPlayers = 2)
        pm.addPlayer(GuestUserImpl("u1"))

        assertEquals(false, pm.isGameFull())
    }

    @Test
    fun `isGameFull returns true when all seats are taken`() = runBlocking {
        val pm = manager(numberOfPlayers = 2)
        pm.addPlayer(GuestUserImpl("u1"))
        pm.addPlayer(GuestUserImpl("u2"))

        assertEquals(true, pm.isGameFull())
    }

    // connectedPlayers

    @Test
    fun `connectedPlayers returns only players with Connected status`() = runBlocking {
        val pm = manager(numberOfPlayers = 3)
        pm.addPlayer(GuestUserImpl("u1"))
        pm.addPlayer(GuestUserImpl("u2"))
        pm.addPlayer(GuestUserImpl("u3"))
        pm.disconnectPlayer(GuestUserImpl("u2"))

        val connected = pm.connectedPlayers()

        assertEquals(2, connected.size)
        assertEquals(true, connected.all { it.connectionStatus == ConnectionStatus.Connected })
    }

    @Test
    fun `connectedPlayers returns empty list when no players are connected`() = runBlocking {
        val pm = manager(numberOfPlayers = 1)
        pm.addPlayer(GuestUserImpl("u1"))
        pm.disconnectPlayer(GuestUserImpl("u1"))

        assertEquals(emptyList<Player>(), pm.connectedPlayers())
    }

    // connectPlayer

    @Test
    fun `connectPlayer sets status to Connected`() = runBlocking {
        val pm = manager()
        val user = GuestUserImpl("u1")
        pm.addPlayer(user)
        pm.disconnectPlayer(user)

        pm.connectPlayer(user)

        assertEquals(ConnectionStatus.Connected, pm.getPlayer(user).connectionStatus)
    }

    @Test
    fun `connectPlayer invokes onPlayerConnected callback`() = runBlocking {
        val connected = mutableListOf<Player>()
        val pm = manager(onPlayerConnected = { connected.add(it) })
        val user = GuestUserImpl("u1")
        pm.addPlayer(user)
        pm.disconnectPlayer(user)

        pm.connectPlayer(user)

        assertEquals(1, connected.size)
    }

    @Test
    fun `connectPlayer does not invoke callback when player is already connected`() = runBlocking {
        val connected = mutableListOf<Player>()
        val pm = manager(onPlayerConnected = { connected.add(it) })
        val user = GuestUserImpl("u1")
        pm.addPlayer(user)

        pm.connectPlayer(user)

        assertEquals(0, connected.size)
    }

    @Test
    fun `connectPlayer throws UserNotInGameException for unknown user`(): Unit = runBlocking {
        val pm = manager()

        assertFailsWith<UserNotInGameException> {
            pm.connectPlayer(GuestUserImpl("unknown"))
        }
    }

    // disconnectPlayer

    @Test
    fun `disconnectPlayer sets status to Disconnected`() = runBlocking {
        val pm = manager()
        val user = GuestUserImpl("u1")
        pm.addPlayer(user)

        pm.disconnectPlayer(user)

        assertEquals(ConnectionStatus.Disconnected, pm.getPlayer(user).connectionStatus)
    }

    @Test
    fun `disconnectPlayer invokes onPlayerDisconnected callback`() = runBlocking {
        val disconnected = mutableListOf<Player>()
        val pm = manager(onPlayerDisconnected = { disconnected.add(it) })
        val user = GuestUserImpl("u1")
        pm.addPlayer(user)

        pm.disconnectPlayer(user)

        assertEquals(1, disconnected.size)
    }

    @Test
    fun `disconnectPlayer does not invoke callback when player is already disconnected`() = runBlocking {
        val disconnected = mutableListOf<Player>()
        val pm = manager(onPlayerDisconnected = { disconnected.add(it) })
        val user = GuestUserImpl("u1")
        pm.addPlayer(user)
        pm.disconnectPlayer(user)

        pm.disconnectPlayer(user)

        assertEquals(1, disconnected.size)
    }

    @Test
    fun `disconnectPlayer throws UserNotInGameException for unknown user`(): Unit = runBlocking {
        val pm = manager()

        assertFailsWith<UserNotInGameException> {
            pm.disconnectPlayer(GuestUserImpl("unknown"))
        }
    }
}
