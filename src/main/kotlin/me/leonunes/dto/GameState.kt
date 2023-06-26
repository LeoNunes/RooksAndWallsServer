package me.leonunes.dto

import kotlinx.serialization.Serializable
import me.leonunes.common.Id
import me.leonunes.common.SquareCoordinate
import me.leonunes.model.Game
import me.leonunes.model.Piece
import me.leonunes.model.Player
import me.leonunes.model.PlayerId

@Serializable
data class GameStateDTO(
    val gameId: Int,
    val playerId: Int,
    val players: List<PlayerDTO>,
    val pieces: List<PieceDTO>,
)

@Serializable
data class PlayerDTO(val id: Int)

fun Player.toDto() : PlayerDTO = PlayerDTO(this.id.get())

@Serializable
data class PieceDTO(val id: Int, val owner: Int, val position: SquareCoordinate)

fun Piece.toDto() : PieceDTO = PieceDTO(id.get(), owner.id.get(), position)

fun Game.getStateDto(playerId: PlayerId) : GameStateDTO {
    return GameStateDTO(
        gameId = this.id.get(),
        playerId = playerId.get(),
        pieces = getPieces().map { it.toDto() }.toList(),
        players = getPlayers().map { it.toDto() }.toList(),
    )
}
