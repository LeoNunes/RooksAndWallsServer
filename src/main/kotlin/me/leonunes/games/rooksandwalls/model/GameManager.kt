package me.leonunes.games.rooksandwalls.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.leonunes.games.rooksandwalls.ai.AiDifficulty
import me.leonunes.games.rooksandwalls.ai.AiPlayerRunner
import me.leonunes.games.rooksandwalls.ai.getUser
import me.leonunes.games.rooksandwalls.ai.toAiStrategy
import me.leonunes.games.users.User

class GameUpdate

class GameManager(val game: Game, val playersManager: PlayersManager) {
    private val mutex = Mutex()
    private val updateChannels: MutableList<SendChannel<GameUpdate>> = mutableListOf()

    val players: List<Player> get() = playersManager.players

    init {
        playersManager.observe(object : PlayersManagerObserver {
            override suspend fun onPlayerAdded(player: Player) = onUpdate()
            override suspend fun onPlayerConnected(player: Player) = onUpdate()
            override suspend fun onPlayerDisconnected(player: Player) = onUpdate()
        })
    }

    private suspend fun onUpdate() {
        if (game.gameStage == GameStage.NotStarted && playersManager.areAllPlayersConnected()) {
            game.start()
        }

        updateChannels.forEach {
            it.send(GameUpdate())
        }
    }

    suspend fun connectPlayer(user: User): GameView = mutex.withLock {
        val player = playersManager.addPlayer(user)
        playersManager.connectPlayer(user)
        return GameView(this, player)
    }

    suspend fun disconnectPlayer(user: User) = mutex.withLock {
        playersManager.disconnectPlayer(user)
    }

    suspend fun addAiPlayer(scope: CoroutineScope, difficulty: AiDifficulty): GameView = mutex.withLock {
        val aiUser = difficulty.getUser()
        val player = playersManager.addPlayer(aiUser)
        val gameView = GameView(this, player)
        AiPlayerRunner(game, gameView.player.playerNumber, difficulty.toAiStrategy()).start(scope)
        gameView
    }
}
