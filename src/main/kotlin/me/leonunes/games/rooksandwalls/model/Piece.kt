package me.leonunes.games.rooksandwalls.model

import me.leonunes.games.common.*

typealias PieceId = Id<Piece, Int>
class Piece(val id: PieceId, val owner: PlayerNumber, override var position: SquareCoordinate, board: Board) :
    BoardPlaceable<SquareCoordinate> {
    val movement = SteppedMovement(this, board, linearMovementDirections).apply {
        validateInsideBoard()
        validateBlockedByPieces()
        validateBlockedByWalls()
    }
}
