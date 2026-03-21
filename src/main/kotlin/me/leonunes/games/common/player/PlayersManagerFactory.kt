package me.leonunes.games.common.player

class PlayersManagerFactory {
    fun createPlayerManager(numberOfPlayers: Int): PlayersManager {
        return PlayersManager(numberOfPlayers)
    }
}
