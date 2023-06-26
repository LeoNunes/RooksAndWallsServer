package me.leonunes.model

import me.leonunes.common.SquareCoordinate

sealed interface GameAction {
    val playerId: Id<Player>
    suspend fun process(game: GameImp)
}

data class MoveAction(
    override val playerId: Id<Player>,
    val piece: Id<Piece>,
    val position: SquareCoordinate) : GameAction {

    override suspend fun process(game: GameImp) {
        println("Move action processing")
    }
}

data class AddPieceAction(override val playerId: Id<Player>, val position: SquareCoordinate) : GameAction {
    override suspend fun process(game: GameImp) {
        game.addPiece(game.getPlayerById(playerId)!!, position)
    }
}
