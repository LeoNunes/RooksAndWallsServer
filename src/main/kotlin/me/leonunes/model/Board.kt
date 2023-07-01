package me.leonunes.model

import me.leonunes.common.*

typealias PieceId = Id<Piece, Int>
class Piece(val id: PieceId, val owner: Player, override var position: SquareCoordinate, board: Board) :
    BoardPlaceable<SquareCoordinate> {
    val movement = SteppedMovement(this, board, linearMovementDirections) {
        validateInsideBoard()
        validateBlockedByPieces()
        validateBlockedByWalls()
    }
}

data class Wall(override val position: EdgeCoordinate) : BoardPlaceable<EdgeCoordinate>

class Board(override val rows: Int, override val columns: Int) : GridBoardWithWalls {
    override val pieces = mutableListOf<Piece>()
    override val walls = mutableListOf<Wall>()
    val deadPieces = mutableListOf<Piece>()
}
