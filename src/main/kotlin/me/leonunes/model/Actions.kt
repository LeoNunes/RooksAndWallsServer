package me.leonunes.model

import me.leonunes.common.SquareCoordinate

sealed interface GameAction {
    val playerId: PlayerId
    suspend fun process(game: GameImp)
}

data class AddPieceAction(override val playerId: PlayerId, val position: SquareCoordinate) : GameAction {
    override suspend fun process(game: GameImp) {
        game.addPiece(game.getPlayerById(playerId)!!, position)
    }
}

data class MoveAction(
    override val playerId: PlayerId,
    val piece: PieceId,
    val position: SquareCoordinate) : GameAction {

    override suspend fun process(game: GameImp) {
        println("Move action processing")
    }
}
