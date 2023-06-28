package me.leonunes.dto

import kotlinx.serialization.Serializable
import me.leonunes.common.EdgeCoordinate
import me.leonunes.common.SquareCoordinate
import me.leonunes.model.*

@Serializable
data class GameStateDTO(
    val gameId: Int,
    val gameStage: GameStage,
    val currentTurn: Int?,
    val playerId: Int,
    val players: List<PlayerDTO>,
    val pieces: List<PieceDTO>,
    val walls: List<WallDTO>,
)

@Serializable
data class PlayerDTO(val id: Int)
fun Player.toDto() : PlayerDTO = PlayerDTO(this.id.get())

@Serializable
data class PieceDTO(val id: Int, val owner: Int, val position: SquareCoordinate)
fun Piece.toDto() : PieceDTO = PieceDTO(id.get(), owner.id.get(), position)

@Serializable
data class WallDTO(val position: EdgeCoordinate)
fun Wall.toDto() : WallDTO = WallDTO(position)

fun Game.getStateDto(playerId: PlayerId) : GameStateDTO {
    return GameStateDTO(
        gameId = this.id.get(),
        gameStage = gameStage,
        currentTurn = currentTurn?.id?.get(),
        playerId = playerId.get(),
        players = players.map { it.toDto() }.toList(),
        pieces = pieces.map { it.toDto() }.toList(),
        walls = walls.map { it.toDto() }.toList(),
    )
}
