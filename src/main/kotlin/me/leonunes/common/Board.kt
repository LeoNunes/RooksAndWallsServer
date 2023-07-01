package me.leonunes.common

import java.util.Stack

interface BoardPlaceable<TCoordinate> {
    val position: TCoordinate
}

interface Board<TCoordinate> {
    val pieces: List<BoardPlaceable<TCoordinate>>
    fun isInsideBoard(position: TCoordinate) : Boolean
    fun allPositions() : Set<TCoordinate>
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

interface GridBoardWithWalls : GridBoard {
    val walls: List<BoardPlaceable<EdgeCoordinate>>

    fun isInsideBoard(position: EdgeCoordinate) : Boolean {
        return isInsideBoard(position.square1) && isInsideBoard(position.square2)
    }
}

fun GridBoardWithWalls.sliceIntoRegions() : Set<Set<SquareCoordinate>>{
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
