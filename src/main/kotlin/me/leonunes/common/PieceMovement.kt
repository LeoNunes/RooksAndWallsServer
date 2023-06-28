package me.leonunes.common

import kotlin.math.abs

interface PieceMovement<TCoordinate> {
    fun canMoveTo(destination: TCoordinate) : Boolean
    fun getPossibleDestinations() : List<TCoordinate>
}

class LinearPieceMovementWithWalls(private val board: GridBoardWithWalls, private val piece: Piece<SquareCoordinate>) : PieceMovement<SquareCoordinate> {
    private val fourDirections = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))

    override fun canMoveTo(destination: SquareCoordinate) : Boolean {
        if (!board.isInsideBoard(destination))
            return false

        val direction = run {
            val rowDelta = destination.row - piece.position.row
            val columnDelta = destination.column - piece.position.column
            Pair(if (rowDelta == 0) 0 else rowDelta / abs(rowDelta),
                 if (columnDelta == 0) 0 else columnDelta / abs(columnDelta))
        }

        if (direction !in fourDirections)
            return false

        var currentPosition = piece.position
        while (currentPosition != destination) {
            val nextPosition = SquareCoordinate(
                currentPosition.row + direction.first,
                currentPosition.column + direction.second)

            if (board.pieces.find { it.position == nextPosition } != null)
                return false

            val edgePosition = EdgeCoordinate(currentPosition, nextPosition)
            if (board.walls.find { it.position == edgePosition } != null)
                return false

            currentPosition = nextPosition
        }

        return true
    }

    override fun getPossibleDestinations(): List<SquareCoordinate> {
        TODO("Not yet implemented")
    }
}