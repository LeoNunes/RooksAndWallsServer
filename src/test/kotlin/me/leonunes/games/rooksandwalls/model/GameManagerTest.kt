package me.leonunes.games.rooksandwalls.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import me.leonunes.games.common.player.ConnectionStatus
import me.leonunes.games.common.player.GameFullException
import me.leonunes.games.common.player.PlayersManagerFactory
import me.leonunes.games.rooksandwalls.ai.AiDifficulty
import me.leonunes.games.users.GuestUserImpl
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
class GameManagerTest {

    private fun createManager(config: GameConfig = GameConfigDefaultValues): GameManager {
        val game = GameFactory().createGame(config)
        val playersManager = PlayersManagerFactory().createPlayerManager(config.numberOfPlayers)
        return GameManager(game, playersManager)
    }

    @Test
    fun `connectPlayer creates GameView with Player with Connected status`() = runBlocking {
        val manager = createManager()
        val user = GuestUserImpl("user-1")

        val gameView = manager.connectPlayer(user)

        assertEquals(manager.game.id, gameView.id)
        assertEquals(user, gameView.player.user)
        assertEquals(ConnectionStatus.Connected, gameView.player.connectionStatus)
    }

    @Test
    fun `connectPlayer adds player to manager player list`() = runBlocking {
        val manager = createManager()

        manager.connectPlayer(GuestUserImpl("user-1"))

        assertEquals(1, manager.players.size)
    }

    @Test
    fun `game starts in PiecePlacement stage when all players connect`() = runBlocking {
        val manager = createManager()

        repeat(manager.game.config.numberOfPlayers) { i -> manager.connectPlayer(GuestUserImpl("user-$i")) }

        assertEquals(GameStage.PiecePlacement, manager.game.gameStage)
    }

    @Test
    fun `connectPlayer throws GameFullException when game is full`() : Unit = runBlocking {
        val manager = createManager()

        repeat(manager.game.config.numberOfPlayers) { i -> manager.connectPlayer(GuestUserImpl("user-$i")) }

        assertFailsWith<GameFullException> {
            manager.connectPlayer(GuestUserImpl("extra-user"))
        }
    }

    @Test
    fun `connectPlayer reconnects existing player and returns Connected status`() = runBlocking {
        val manager = createManager()
        val user = GuestUserImpl("user-1")

        manager.connectPlayer(user)
        manager.disconnectPlayer(user)
        val reconnected = manager.connectPlayer(user)

        assertEquals(ConnectionStatus.Connected, reconnected.player.connectionStatus)
        assertEquals(1, manager.players.size)
    }

    @Test
    fun `disconnectPlayer sets player connectionStatus to Disconnected`() = runBlocking {
        val manager = createManager()
        val user = GuestUserImpl("user-0")

        manager.connectPlayer(user)
        manager.disconnectPlayer(user)

        assertEquals(ConnectionStatus.Disconnected, manager.players.find { it.id.get() == "user-0" }?.connectionStatus)
    }

    @Test
    fun `disconnectPlayer before game starts does not crash`() = runBlocking {
        val manager = createManager()
        val user = GuestUserImpl("user-1")

        manager.connectPlayer(user)
        manager.disconnectPlayer(user)

        assertEquals(ConnectionStatus.Disconnected, manager.players[0].connectionStatus)
    }

    @Test
    fun `addAiPlayer adds AI player to manager`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + Job())
        val manager = createManager()

        manager.addAiPlayer(scope, AiDifficulty.EASY)

        assertEquals(1, manager.players.size)
        scope.cancel()
    }

    @Test
    fun `addAiPlayer throws GameFullException when game is full`() : Unit = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + Job())
        val config = GameConfig(numberOfPlayers = 2, piecesPerPlayer = 1, boardRows = 4, boardColumns = 4)
        val manager = createManager(config)

        repeat(config.numberOfPlayers) { i -> manager.connectPlayer(GuestUserImpl("user-$i")) }

        assertFailsWith<GameFullException> {
            manager.addAiPlayer(scope, AiDifficulty.EASY)
        }
        scope.cancel()
    }

    @Test
    fun `addAiPlayer with one human starts game`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + Job())
        val config = GameConfig(numberOfPlayers = 2, piecesPerPlayer = 1, boardRows = 4, boardColumns = 4)
        val manager = createManager(config)

        manager.connectPlayer(GuestUserImpl("user-1"))
        manager.addAiPlayer(scope, AiDifficulty.EASY)

        assertEquals(2, manager.players.size)
        assertEquals(GameStage.PiecePlacement, manager.game.gameStage)
        scope.cancel()
    }
}
