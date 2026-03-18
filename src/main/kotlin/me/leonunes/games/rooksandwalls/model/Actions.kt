package me.leonunes.games.rooksandwalls.model

import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.SquareCoordinate

data class PieceMovement(val pieceId: PieceId, val position: SquareCoordinate)
data class WallPlacement(val wallPosition: EdgeCoordinate)

sealed interface GameAction {
    val playerNumber: PlayerNumber
}

data class AddPieceAction(override val playerNumber: PlayerNumber, val position: SquareCoordinate) : GameAction

data class MoveAction(
    override val playerNumber: PlayerNumber,
    val pieceMovement: PieceMovement?,
    val wallPlacement: WallPlacement
) : GameAction
