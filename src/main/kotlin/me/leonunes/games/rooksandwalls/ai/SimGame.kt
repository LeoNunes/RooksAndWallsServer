// src/main/kotlin/me/leonunes/games/rooksandwalls/ai/SimGame.kt
package me.leonunes.games.rooksandwalls.ai

import me.leonunes.games.common.*
import me.leonunes.games.rooksandwalls.model.Game
import me.leonunes.games.rooksandwalls.model.Wall

data class SimPiece(val ownerIndex: Int, var position: SquareCoordinate)

data class SimMove(
    val pieceIndex: Int?,                // index into SimGame.pieces; null = no piece movement
    val destination: SquareCoordinate?,  // null = no piece movement
    val wallPosition: EdgeCoordinate
)

class SimGame(
    override val rows: Int,
    override val columns: Int,
    val playerCount: Int,
    var currentPlayerIndex: Int,
    val pieces: MutableList<SimPiece>,
    internal val wallSet: MutableSet<EdgeCoordinate>,  // `internal` so tests can inspect walls directly
    val eliminatedPlayers: MutableSet<Int>
) : GridBoard, WithWalls<Wall> {

    // Satisfies WithWalls<Wall> so sliceIntoRegions() can be called on this object
    override val walls: List<Wall> get() = wallSet.map { Wall(it) }

    fun clone(): SimGame = SimGame(
        rows = rows,
        columns = columns,
        playerCount = playerCount,
        currentPlayerIndex = currentPlayerIndex,
        pieces = pieces.map { it.copy() }.toMutableList(),
        wallSet = wallSet.toMutableSet(),
        eliminatedPlayers = eliminatedPlayers.toMutableSet()
    )

    fun addWall(edge: EdgeCoordinate) { wallSet.add(edge) }

    internal fun rookDestinations(from: SquareCoordinate): Set<SquareCoordinate> {
        val occupiedSquares = pieces.map { it.position }.toSet()
        val result = mutableSetOf<SquareCoordinate>()
        val directions = listOf(coordStep(0, 1), coordStep(0, -1), coordStep(1, 0), coordStep(-1, 0))

        for (dir in directions) {
            var cur = from
            while (true) {
                val next = dir.takeStep(cur)
                if (!isInsideBoard(next)) break
                if (EdgeCoordinate(cur, next) in wallSet) break
                if (next in occupiedSquares) break
                result.add(next)
                cur = next
            }
        }
        return result
    }

    private fun allEdgePositions(): List<EdgeCoordinate> =
        (0 until rows).flatMap { row ->
            (0 until columns).flatMap { col ->
                buildList {
                    if (row + 1 < rows) add(EdgeCoordinate(coord(row, col), coord(row + 1, col)))
                    if (col + 1 < columns) add(EdgeCoordinate(coord(row, col), coord(row, col + 1)))
                }
            }
        }

    fun availableWalls(): List<EdgeCoordinate> = allEdgePositions().filter { it !in wallSet }

    fun applyMove(move: SimMove) {
        // 1. Move piece if specified
        if (move.pieceIndex != null && move.destination != null) {
            pieces[move.pieceIndex].position = move.destination
        }

        // 2. Place wall
        wallSet.add(move.wallPosition)

        // 3. Eliminate pieces in regions of ≤ 8 squares
        val deadSquares = sliceIntoRegions()
            .filter { it.size <= 8 }
            .flatten()
            .toSet()
        pieces.removeAll { it.position in deadSquares }

        // 4. Update eliminated players
        val activePlayers = pieces.map { it.ownerIndex }.toSet()
        for (p in 0 until playerCount) {
            if (p !in activePlayers) eliminatedPlayers.add(p)
        }

        // 5. Advance turn, skipping eliminated players
        if (!isTerminal()) {
            do {
                currentPlayerIndex = (currentPlayerIndex + 1) % playerCount
            } while (currentPlayerIndex in eliminatedPlayers)
        }
    }

    fun isTerminal(): Boolean {
        val activePlayers = pieces.map { it.ownerIndex }.toSet()
        return activePlayers.size <= 1
    }

    /**
     * Only call this when isTerminal() is true.
     * Returns the winning player index, or null if it's a draw (all pieces eliminated).
     */
    fun winner(): Int? {
        val activePlayers = pieces.map { it.ownerIndex }.toSet()
        return if (activePlayers.size == 1) activePlayers.first() else null
    }

    fun getLegalMoves(): List<SimMove> {
        val available = availableWalls()
        val myPieceIndices = pieces.indices.filter { pieces[it].ownerIndex == currentPlayerIndex }
        val pieceMoves: List<Pair<Int, SquareCoordinate>> = myPieceIndices.flatMap { idx ->
            rookDestinations(pieces[idx].position).map { dest -> idx to dest }
        }

        return if (pieceMoves.isEmpty()) {
            available.map { wall -> SimMove(null, null, wall) }
        } else {
            pieceMoves.flatMap { (pieceIdx, dest) ->
                available.map { wall -> SimMove(pieceIdx, dest, wall) }
            }
        }
    }

    companion object {
        fun from(game: Game): SimGame {
            val players = game.players
            val playerIndexById = players.mapIndexed { idx, p -> p.id to idx }.toMap()
            val currentPlayerIndex = playerIndexById[game.currentTurn?.id] ?: 0
            val eliminatedIds = players.map { it.id }.toSet() - game.remainingPlayers.map { it.id }.toSet()
            val eliminatedIndices = eliminatedIds.map { playerIndexById[it]!! }.toMutableSet()

            return SimGame(
                rows = game.config.boardRows,
                columns = game.config.boardColumns,
                playerCount = players.size,
                currentPlayerIndex = currentPlayerIndex,
                pieces = game.pieces.map { piece ->
                    SimPiece(playerIndexById[piece.owner.id]!!, piece.position)
                }.toMutableList(),
                wallSet = game.walls.map { it.position }.toMutableSet(),
                eliminatedPlayers = eliminatedIndices
            )
        }
    }
}
