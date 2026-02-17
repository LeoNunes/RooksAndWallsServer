package me.leonunes.games.rooksandwalls.model

import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.SquareCoordinate

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
    val pieceId: PieceId,
    val piecePosition: SquareCoordinate,
    val wallPosition: EdgeCoordinate) : GameAction {

    override fun process(game: GameImp) {
        game.move(playerId, pieceId, piecePosition, wallPosition)
    }
}
