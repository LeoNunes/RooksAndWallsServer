package me.leonunes.games.common.player

import me.leonunes.games.users.User

typealias PlayerNumber = Int

enum class ConnectionStatus { Connected, Disconnected }

data class Player(
    val user: User,
    val playerNumber: PlayerNumber,
    var connectionStatus: ConnectionStatus = ConnectionStatus.Connected
) {
    val displayName: String get() = user.displayName
}
