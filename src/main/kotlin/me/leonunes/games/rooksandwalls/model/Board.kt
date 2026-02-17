package me.leonunes.games.rooksandwalls.model

import me.leonunes.games.common.*

typealias PieceId = Id<Piece, Int>
class Piece(val id: PieceId, val owner: Player, override var position: SquareCoordinate, board: Board) :
    BoardPlaceable<SquareCoordinate> {
    val movement = SteppedMovement(this, board, linearMovementDirections).apply {
        validateInsideBoard()
        validateBlockedByPieces()
        validateBlockedByWalls()
    }
}

data class Wall(override val position: EdgeCoordinate) : BoardPlaceable<EdgeCoordinate>

class Board(override val rows: Int, override val columns: Int) : GridBoard, WithPieces<Piece>, WithWalls<Wall> {
    override val pieces = mutableListOf<Piece>()
    override val walls = mutableListOf<Wall>()
    val deadPieces = mutableListOf<Piece>()
}
