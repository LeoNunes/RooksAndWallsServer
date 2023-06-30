package me.leonunes.common

interface BoardPlaceable<TCoordinate> {
    val position: TCoordinate
}

interface Board<TCoordinate> {
    val pieces: List<BoardPlaceable<TCoordinate>>
    fun isInsideBoard(position: TCoordinate) : Boolean
}

interface GridBoard : Board<SquareCoordinate> {
    val rows: Int
    val columns: Int
    override fun isInsideBoard(position: SquareCoordinate) : Boolean {
        return position.row in 0 until rows && position.column in 0 until columns
    }
}

interface GridBoardWithWalls : GridBoard {
    val walls: List<BoardPlaceable<EdgeCoordinate>>

    fun isInsideBoard(position: EdgeCoordinate) : Boolean {
        return isInsideBoard(position.square1) && isInsideBoard(position.square2)
    }
}
