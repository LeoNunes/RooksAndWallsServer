package me.leonunes.games.rooksandwalls.model

import me.leonunes.games.common.Id
import me.leonunes.games.common.asId
import me.leonunes.games.users.User

typealias PlayerId = Id<Player, String>
typealias PlayerNumber = Int

enum class ConnectionStatus { Connected, Disconnected }

data class Player(
    val user: User,
    val playerNumber: PlayerNumber,
    var connectionStatus: ConnectionStatus = ConnectionStatus.Connected
) {
    val id: PlayerId get() = user.id.asId()
    val displayName: String get() = user.displayName
}