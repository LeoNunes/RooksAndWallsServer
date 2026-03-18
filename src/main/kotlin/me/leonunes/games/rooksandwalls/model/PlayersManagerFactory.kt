package me.leonunes.games.rooksandwalls.model

class PlayersManagerFactory {
    fun createPlayerManager(numberOfPlayers: Int): PlayersManager {
        return PlayersManager(numberOfPlayers)
    }
}
