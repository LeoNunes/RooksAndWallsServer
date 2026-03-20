package me.leonunes.games.rooksandwalls.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.ReceiveChannel
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
        game.observe(object : GameObserver {
            override suspend fun onGameUpdated() {
                updateChannels.forEach { it.send(GameUpdate()) }
                if (game.gameStage == GameStage.Completed) {
                    updateChannels.forEach { it.close() }
                }
            }
        })

        playersManager.observe(object : PlayersManagerObserver {
            override suspend fun onPlayerAdded(player: Player) = onPlayerUpdate()
            override suspend fun onPlayerConnected(player: Player) = onPlayerUpdate()
            override suspend fun onPlayerDisconnected(player: Player) = onPlayerUpdate()
        })
    }

    private suspend fun onPlayerUpdate() {
        if (game.gameStage == GameStage.NotStarted && playersManager.areAllPlayersConnected()) {
            game.start()  // triggers onGameUpdated observer above
        } else {
            updateChannels.forEach { it.send(GameUpdate()) }
        }
    }

    private fun createAndRegisterChannel(): ReceiveChannel<GameUpdate> {
        val channel = Channel<GameUpdate>(CONFLATED)
        updateChannels.add(channel)
        return channel
    }

    suspend fun connectPlayer(user: User): GameView = mutex.withLock {
        val player = playersManager.addPlayer(user)
        playersManager.connectPlayer(user)
        GameView(this, player, createAndRegisterChannel())
    }

    suspend fun disconnectPlayer(user: User) = mutex.withLock {
        playersManager.disconnectPlayer(user)
    }

    suspend fun addAiPlayer(scope: CoroutineScope, difficulty: AiDifficulty): GameView = mutex.withLock {
        val aiUser = difficulty.getUser()
        val player = playersManager.addPlayer(aiUser)
        val gameView = GameView(this, player, createAndRegisterChannel())
        AiPlayerRunner(gameView, difficulty.toAiStrategy()).start(scope)
        gameView
    }

    suspend fun processAction(action: GameAction) = mutex.withLock {
        game.processAction(action)
    }
}
