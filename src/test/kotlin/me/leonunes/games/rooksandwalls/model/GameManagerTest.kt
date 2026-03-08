package me.leonunes.games.rooksandwalls.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import me.leonunes.games.common.asId
import me.leonunes.games.users.GuestUserImpl
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
class GameManagerTest {

    @Test
    fun `joinGame creates player with Connected status`() = runBlocking {
        val game = GameFactory.createGame()
        val manager = GameManager(game)
        val user = GuestUserImpl("user-1")

        val player = manager.joinGame(user)

        assertEquals(user, player.user)
        assertEquals(ConnectionStatus.Connected, player.connectionStatus)
    }

    @Test
    fun `joinGame adds player to manager player list`() = runBlocking {
        val game = GameFactory.createGame()
        val manager = GameManager(game)

        manager.joinGame(GuestUserImpl("user-1"))

        assertEquals(1, manager.players.size)
    }

    @Test
    fun `game starts in PiecePlacement stage when all players join`() = runBlocking {
        val game = GameFactory.createGame()
        val manager = GameManager(game)

        repeat(game.config.numberOfPlayers) { i -> manager.joinGame(GuestUserImpl("user-$i")) }

        assertEquals(GameStage.PiecePlacement, game.gameStage)
    }

    @Test
    fun `game has correct players after all join`() = runBlocking {
        val game = GameFactory.createGame()
        val manager = GameManager(game)

        repeat(game.config.numberOfPlayers) { i -> manager.joinGame(GuestUserImpl("user-$i")) }

        assertEquals(game.config.numberOfPlayers, game.players.size)
    }

    @Test
    fun `joinGame throws GameFullException when game is full`(): Unit = runBlocking {
        val game = GameFactory.createGame()
        val manager = GameManager(game)

        repeat(game.config.numberOfPlayers) { i -> manager.joinGame(GuestUserImpl("user-$i")) }

        assertFailsWith<GameFullException> {
            manager.joinGame(GuestUserImpl("extra-user"))
        }
    }

    @Test
    fun `joinGame reconnects existing player and returns Connected status`() = runBlocking {
        val game = GameFactory.createGame()
        val manager = GameManager(game)
        val user = GuestUserImpl("user-1")

        manager.joinGame(user)
        manager.disconnectPlayer("user-1".asId())
        val reconnected = manager.joinGame(user)

        assertEquals(ConnectionStatus.Connected, reconnected.connectionStatus)
        assertEquals(1, manager.players.size)
    }

    @Test
    fun `disconnectPlayer sets player connectionStatus to Disconnected`() = runBlocking {
        val game = GameFactory.createGame()
        val manager = GameManager(game)

        repeat(game.config.numberOfPlayers) { i -> manager.joinGame(GuestUserImpl("user-$i")) }
        manager.disconnectPlayer("user-0".asId())

        assertEquals(ConnectionStatus.Disconnected, manager.players.find { it.id.get() == "user-0" }?.connectionStatus)
    }

    @Test
    fun `disconnectPlayer before game starts does not crash`() = runBlocking {
        val game = GameFactory.createGame()
        val manager = GameManager(game)

        manager.joinGame(GuestUserImpl("user-1"))
        manager.disconnectPlayer("user-1".asId())  // game not started yet — should not throw

        assertEquals(ConnectionStatus.Disconnected, manager.players[0].connectionStatus)
    }
}
