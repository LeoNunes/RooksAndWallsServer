package me.leonunes.model

import me.leonunes.common.SquareCoordinate

sealed interface GameAction {
    fun process(game: Game)
}

data class MoveAction(val piece: Id<Piece>, val position: SquareCoordinate) : GameAction {
    override fun process(game: Game) {
        println("Move action processing")
    }
}

data class PlacePieceAction(val position: SquareCoordinate) : GameAction {
    override fun process(game: Game) {
        println("Place piece action processing")
    }
}
