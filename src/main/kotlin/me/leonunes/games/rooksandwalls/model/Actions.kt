package me.leonunes.games.rooksandwalls.model

import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.SquareCoordinate

data class PieceMovement(val pieceId: PieceId, val position: SquareCoordinate)
data class WallPlacement(val wallPosition: EdgeCoordinate)

sealed interface GameAction {
    val playerId: PlayerId
}

data class AddPieceAction(override val playerId: PlayerId, val position: SquareCoordinate) : GameAction

data class MoveAction(
    override val playerId: PlayerId,
    val pieceMovement: PieceMovement?,
    val wallPlacement: WallPlacement
) : GameAction
