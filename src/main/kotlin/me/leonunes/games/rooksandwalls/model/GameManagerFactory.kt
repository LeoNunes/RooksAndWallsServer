package me.leonunes.games.rooksandwalls.model

import java.util.concurrent.ConcurrentHashMap

class GameManagerFactory(private val gameFactory: GameFactory, private val playerManagerFactory: PlayersManagerFactory) {
    private val managers = ConcurrentHashMap<GameId, GameManager>()

    fun createGame(config: GameConfig): GameManager {
        val playerManager = playerManagerFactory.createPlayerManager(config.numberOfPlayers)
        val game = gameFactory.createGame(config)
        val manager = GameManager(game, playerManager)
        managers[game.id] = manager
        return manager
    }

    fun getManager(gameId: GameId): GameManager? = managers[gameId]
}
