package me.leonunes.common

import java.util.*

interface Board<TCoord> {
    fun isInsideBoard(position: TCoord) : Boolean
    fun allPositions() : Set<TCoord>
}

interface BoardPlaceable<TCoord> {
    val position: TCoord
}

interface WithPieces<out TPiece : BoardPlaceable<*>> {
    val pieces: List<TPiece>
}

interface WithWalls<out TWall : BoardPlaceable<*>> {
    val walls: List<TWall>
}

interface GridBoard : Board<SquareCoordinate> {
    val rows: Int
    val columns: Int

    override fun isInsideBoard(position: SquareCoordinate) : Boolean {
        return position.row in 0 until rows && position.column in 0 until columns
    }

    override fun allPositions() : Set<SquareCoordinate> {
        return sequence {
            for (x in 0 until rows) {
                for (y in 0 until columns) {
                    yield(coord(x, y))
                }
            }
        }.toSet()
    }
}

fun Board<SquareCoordinate>.isInsideBoard(position: EdgeCoordinate) : Boolean {
    return isInsideBoard(position.square1) && isInsideBoard(position.square2)
}

fun <TBoard> TBoard.sliceIntoRegions() : Set<Set<SquareCoordinate>>
    where TBoard : GridBoard, TBoard : WithWalls<BoardPlaceable<EdgeCoordinate>> {

    fun connectedTo(square: SquareCoordinate) : List<SquareCoordinate> {
        return listOf(coordStep(0, 1), coordStep(0, -1), coordStep(1, 0), coordStep(-1, 0))
            .map { it.takeStep(square) }
            .filter { isInsideBoard(it) }
            .filter { coord -> walls.find { wall -> wall.position == EdgeCoordinate(coord, square) } == null }
    }

    val visited = mutableSetOf<SquareCoordinate>()
    val regions = mutableMapOf<Int, MutableSet<SquareCoordinate>>()
    var currentRegion = 0

    for (position in allPositions()) {
        if (position in visited) continue

        regions[currentRegion] = mutableSetOf()

        val squaresToProcess = Stack<SquareCoordinate>()
        squaresToProcess.add(position)
        visited.add(position)

        while (!squaresToProcess.isEmpty()) {
            val coord = squaresToProcess.pop()
            regions[currentRegion]!!.add(coord)

            val connectedSquares = connectedTo(coord).filter { it !in visited }

            visited.addAll(connectedSquares)
            squaresToProcess.addAll(connectedSquares)
        }

        currentRegion++
    }

    return regions.values.toSet()
}
