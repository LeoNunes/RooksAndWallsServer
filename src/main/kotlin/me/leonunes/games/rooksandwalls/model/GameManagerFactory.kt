package me.leonunes.games.rooksandwalls.model

import java.util.concurrent.ConcurrentHashMap

class GameManagerFactory {
    private val managers = ConcurrentHashMap<GameId, GameManager>()

    fun createGame(config: GameConfig? = null): GameManager {
        val game = if (config != null) GameFactory.createGame(config) else GameFactory.createGame()
        val manager = GameManager(game)
        managers[game.id] = manager
        return manager
    }

    fun getManager(gameId: GameId): GameManager? = managers[gameId]
}
