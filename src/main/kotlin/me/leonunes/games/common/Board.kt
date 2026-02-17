package me.leonunes.games.common

import java.util.*

interface Board<TCoord> {
    fun isInsideBoard(position: TCoord) : Boolean
    fun allPositions() : Set<TCoord>
}

interface BoardPlaceable<TCoord> {
    val position: TCoord
}

interface WithPieces<out TPiece : me.leonunes.games.common.BoardPlaceable<*>> {
    val pieces: List<TPiece>
}

interface WithWalls<out TWall : me.leonunes.games.common.BoardPlaceable<*>> {
    val walls: List<TWall>
}

interface GridBoard : me.leonunes.games.common.Board<me.leonunes.games.common.SquareCoordinate> {
    val rows: Int
    val columns: Int

    override fun isInsideBoard(position: me.leonunes.games.common.SquareCoordinate) : Boolean {
        return position.row in 0 until rows && position.column in 0 until columns
    }

    override fun allPositions() : Set<me.leonunes.games.common.SquareCoordinate> {
        return sequence {
            for (x in 0 until rows) {
                for (y in 0 until columns) {
                    yield(_root_ide_package_.me.leonunes.games.common.coord(x, y))
                }
            }
        }.toSet()
    }
}

fun me.leonunes.games.common.Board<me.leonunes.games.common.SquareCoordinate>.isInsideBoard(position: me.leonunes.games.common.EdgeCoordinate) : Boolean {
    return isInsideBoard(position.square1) && isInsideBoard(position.square2)
}

fun <TBoard> TBoard.sliceIntoRegions() : Set<Set<me.leonunes.games.common.SquareCoordinate>>
    where TBoard : me.leonunes.games.common.GridBoard, TBoard : me.leonunes.games.common.WithWalls<me.leonunes.games.common.BoardPlaceable<me.leonunes.games.common.EdgeCoordinate>> {

    fun connectedTo(square: me.leonunes.games.common.SquareCoordinate) : List<me.leonunes.games.common.SquareCoordinate> {
        return listOf(
            _root_ide_package_.me.leonunes.games.common.coordStep(0, 1),
            _root_ide_package_.me.leonunes.games.common.coordStep(0, -1),
            _root_ide_package_.me.leonunes.games.common.coordStep(1, 0),
            _root_ide_package_.me.leonunes.games.common.coordStep(-1, 0)
        )
            .map { it.takeStep(square) }
            .filter { isInsideBoard(it) }
            .filter { coord -> walls.find { wall -> wall.position == _root_ide_package_.me.leonunes.games.common.EdgeCoordinate(
                coord,
                square
            )
            } == null }
    }

    val visited = mutableSetOf<me.leonunes.games.common.SquareCoordinate>()
    val regions = mutableMapOf<Int, MutableSet<me.leonunes.games.common.SquareCoordinate>>()
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
