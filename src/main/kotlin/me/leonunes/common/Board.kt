package me.leonunes.common

interface Piece<TCoordinate> {
    val position: TCoordinate
}

interface Board<TCoordinate> {
    val pieces: List<Piece<TCoordinate>>
    fun isInsideBoard(position: TCoordinate) : Boolean
}

interface GridBoard : Board<SquareCoordinate> {
    val rows: Int
    val columns: Int
    override fun isInsideBoard(position: SquareCoordinate) : Boolean {
        return position.row in 0..rows && position.column in 0..columns
    }
}

interface GridBoardWithWalls : GridBoard {
    val walls: List<Piece<EdgeCoordinate>>

    fun isInsideBoard(position: EdgeCoordinate) : Boolean {
        return isInsideBoard(position.square1) && isInsideBoard(position.square2)
    }
}
