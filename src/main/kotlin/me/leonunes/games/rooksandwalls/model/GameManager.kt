package me.leonunes.games.rooksandwalls.model

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.leonunes.games.users.User

class GameManager(val game: Game) {
    private val _players = mutableListOf<Player>()
    private val mutex = Mutex()
    val players: List<Player> get() = _players.toList()

    suspend fun joinGame(user: User): GameView = mutex.withLock {
        val existingPlayer = _players.find { it.user.id == user.id }
        if (existingPlayer != null) {
            reconnect(existingPlayer)
            return@withLock GameView(this, existingPlayer)
        }
        if (_players.size >= game.config.numberOfPlayers) throw GameFullException()

        val player = Player(user)
        _players.add(player)

        if (_players.size == game.config.numberOfPlayers) {
            game.start(_players.toList())
        }
        GameView(this, player)
    }

    // TODO: Send a message in the WS
    suspend fun disconnectPlayer(playerId: PlayerId): Unit = mutex.withLock {
        val player = _players.find { it.id == playerId } ?: return@withLock
        player.connectionStatus = ConnectionStatus.Disconnected
    }

    // TODO: Send a message in the WS
    private fun reconnect(player: Player) {
        player.connectionStatus = ConnectionStatus.Connected
    }
}
