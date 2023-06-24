package me.leonunes.dto

import kotlinx.serialization.Serializable
import me.leonunes.common.SquareCoordinate
import me.leonunes.model.Game
import me.leonunes.model.Piece

@Serializable
data class GameStateDTO(val pieces: List<PieceDTO>)

@Serializable
data class PieceDTO(val owner: Int, val position: SquareCoordinate)

fun Piece.toDto() : PieceDTO = PieceDTO(owner.id.get(), position)

fun Game.getStateDto() : GameStateDTO {
    return GameStateDTO(
        pieces = getPieces().map { it.toDto() }.toList()
    )
}
