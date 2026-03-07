package me.leonunes.games.rooksandwalls.model

import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.SquareCoordinate

data class PieceMovement(val pieceId: PieceId, val position: SquareCoordinate)
data class WallPlacement(val wallPosition: EdgeCoordinate)

sealed interface GameAction {
    val playerId: PlayerId
    fun process(game: GameImp)
}

data class AddPieceAction(override val playerId: PlayerId, val position: SquareCoordinate) : GameAction {
    override fun process(game: GameImp) {
        game.addPiece(playerId, position)
    }
}

data class MoveAction(
    override val playerId: PlayerId,
    val pieceMovement: PieceMovement?,
    val wallPlacement: WallPlacement
) : GameAction {
    override fun process(game: GameImp) {
        game.move(playerId, pieceMovement, wallPlacement)
    }
}
