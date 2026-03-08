package me.leonunes.games.rooksandwalls.model

import me.leonunes.games.common.asId
import me.leonunes.games.users.User

class GameManager(val game: Game) {
    private val _players = mutableListOf<Player>()
    val players: List<Player> get() = _players.toList()

    suspend fun joinGame(user: User): Player {
        val existingIndex = _players.indexOfFirst { it.id == user.id.asId<Player>() }
        if (existingIndex >= 0) {
            return reconnect(existingIndex)
        }
        if (_players.size >= game.config.numberOfPlayers) throw GameFullException()

        val player = Player(user)
        _players.add(player)

        if (_players.size == game.config.numberOfPlayers) {
            game.start(_players.toList())
        }
        return player
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
