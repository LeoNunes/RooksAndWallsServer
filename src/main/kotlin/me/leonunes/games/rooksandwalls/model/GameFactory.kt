package me.leonunes.games.rooksandwalls.model

import me.leonunes.games.common.asId
import java.util.concurrent.atomic.AtomicInteger

class GameFactory {
    private val nextId = AtomicInteger(0)

    fun createGame(config: GameConfig): Game {
        val id: GameId = nextId.getAndIncrement().toString().asId()
        return GameImp(id, config)
    }
}
