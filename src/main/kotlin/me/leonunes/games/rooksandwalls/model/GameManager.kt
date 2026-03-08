package me.leonunes.games.rooksandwalls.model

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.leonunes.games.common.asId
import me.leonunes.games.users.User

class GameManager(val game: Game) {
    private val _players = mutableListOf<Player>()
    private val mutex = Mutex()
    val players: List<Player> get() = _players.toList()

    suspend fun joinGame(user: User): Player = mutex.withLock {
        val existingIndex = _players.indexOfFirst { it.id == user.id.asId<Player>() }
        if (existingIndex >= 0) {
            return@withLock reconnect(existingIndex)
        }
        if (_players.size >= game.config.numberOfPlayers) throw GameFullException()

        val player = Player(user)
        _players.add(player)

        if (_players.size == game.config.numberOfPlayers) {
            game.start(_players.toList())
        }
        player
    }

    suspend fun disconnectPlayer(playerId: PlayerId) {
        val idx = _players.indexOfFirst { it.id == playerId }
        if (idx < 0) return
        _players[idx] = _players[idx].copy(connectionStatus = ConnectionStatus.Disconnected)
        if (game.gameStage != GameStage.WaitingForPlayers) {
            game.notifyUpdates()
        }
    }

    private suspend fun reconnect(index: Int): Player {
        val updated = _players[index].copy(connectionStatus = ConnectionStatus.Connected)
        _players[index] = updated
        if (game.gameStage != GameStage.WaitingForPlayers) {
            game.notifyUpdates()
        }
        return updated
    }
}
