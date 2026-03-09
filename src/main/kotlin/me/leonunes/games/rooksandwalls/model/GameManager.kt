package me.leonunes.games.rooksandwalls.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.leonunes.games.rooksandwalls.ai.AiPlayerRunner
import me.leonunes.games.rooksandwalls.ai.AiStrategy
import me.leonunes.games.rooksandwalls.ai.RandomAiStrategy
import me.leonunes.games.users.AiUser
import me.leonunes.games.users.User

class GameManager(val game: Game) {
    private val _players = mutableListOf<Player>()
    private val mutex = Mutex()
    private var aiCount = 0
    val players: List<Player> get() = _players.toList()

    suspend fun joinGame(user: User): Player = mutex.withLock {
        joinGameInternal(user)
    }

    suspend fun addAiPlayer(scope: CoroutineScope, strategy: AiStrategy = RandomAiStrategy()): Player = mutex.withLock {
        if (game.gameStage != GameStage.WaitingForPlayers) throw GameAlreadyStartedException()
        val aiUser = AiUser(id = "ai-${game.id.get()}-$aiCount")
        aiCount++
        val player = joinGameInternal(aiUser)
        AiPlayerRunner(game, player.id, strategy).start(scope)
        player
    }

    suspend fun disconnectPlayer(playerId: PlayerId): Unit = mutex.withLock {
        val player = _players.find { it.id == playerId } ?: return@withLock
        player.connectionStatus = ConnectionStatus.Disconnected
    }

    private suspend fun joinGameInternal(user: User): Player {
        val existingPlayer = _players.find { it.user.id == user.id }
        if (existingPlayer != null) {
            reconnect(existingPlayer)
            return existingPlayer
        }
        if (_players.size >= game.config.numberOfPlayers) throw GameFullException()

        val player = Player(user)
        _players.add(player)

        if (_players.size == game.config.numberOfPlayers) {
            game.start(_players.toList())
        }
        return player
    }

    private fun reconnect(player: Player) {
        player.connectionStatus = ConnectionStatus.Connected
    }
}
