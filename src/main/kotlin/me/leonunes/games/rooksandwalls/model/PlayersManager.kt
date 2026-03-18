package me.leonunes.games.rooksandwalls.model

import me.leonunes.games.users.User

interface PlayersManagerObserver {
    suspend fun onPlayerAdded(player: Player) = Unit
    suspend fun onPlayerConnected(player: Player) = Unit
    suspend fun onPlayerDisconnected(player: Player) = Unit
}

class PlayersManager(val numberOfPlayers: Int) {
    private val _players = mutableListOf<Player>()
    private val observers = mutableListOf<PlayersManagerObserver>()

    val players: List<Player> get() = _players.toList()

    fun observe(observer: PlayersManagerObserver) = observers.add(observer)

    fun getPlayer(user: User): Player = _players.find { it.user.id == user.id } ?: throw UserNotInGameException()

    fun isGameFull() = numberOfPlayers == _players.size

    fun connectedPlayers() = _players.filter { it.connectionStatus == ConnectionStatus.Connected }

    fun areAllPlayersConnected() = isGameFull() && _players.all { it.connectionStatus == ConnectionStatus.Connected }

    suspend fun addPlayer(user: User): Player {
        val existingPlayer = _players.find { it.user.id == user.id }

        if (existingPlayer != null) {
            return existingPlayer
        }

        if (_players.size >= numberOfPlayers) throw GameFullException()

        val player = Player(user, _players.size)
        _players.add(player)

        observers.forEach { it.onPlayerAdded(player) }

        return player
    }

    suspend fun connectPlayer(user: User) {
        val player = getPlayer(user)

        if (player.connectionStatus != ConnectionStatus.Connected) {
            player.connectionStatus = ConnectionStatus.Connected
            observers.forEach { it.onPlayerConnected(player) }
        }
    }

    suspend fun disconnectPlayer(user: User) {
        val player = getPlayer(user)

        if (player.connectionStatus != ConnectionStatus.Disconnected) {
            player.connectionStatus = ConnectionStatus.Disconnected
            observers.forEach { it.onPlayerDisconnected(player) }
        }
    }
}
