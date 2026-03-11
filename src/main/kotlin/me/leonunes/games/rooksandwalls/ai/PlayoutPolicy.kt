package me.leonunes.games.rooksandwalls.ai

import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.SquareCoordinate
import me.leonunes.games.common.sliceIntoRegions

interface PlayoutPolicy {
    fun sampleMove(game: SimGame): SimMove
}

class RandomPlayoutPolicy : PlayoutPolicy {
    override fun sampleMove(game: SimGame): SimMove {
        val myPieceIndices = game.pieces.indices.filter { game.pieces[it].ownerIndex == game.currentPlayerIndex }
        val pieceMoves: List<Pair<Int, SquareCoordinate>> = myPieceIndices.flatMap { idx ->
            game.rookDestinations(game.pieces[idx].position).map { dest -> idx to dest }
        }
        val movement = pieceMoves.randomOrNull()
        val wallPosition = game.availableWalls().random()
        return SimMove(movement?.first, movement?.second, wallPosition)
    }
}

class HeuristicPlayoutPolicy : PlayoutPolicy {
    override fun sampleMove(game: SimGame): SimMove {
        val myPieceIndices = game.pieces.indices.filter { game.pieces[it].ownerIndex == game.currentPlayerIndex }
        val pieceMoves: List<Pair<Int, SquareCoordinate>> = myPieceIndices.flatMap { idx ->
            game.rookDestinations(game.pieces[idx].position).map { dest -> idx to dest }
        }
        val movement = pieceMoves.randomOrNull()

        // Clone game state and apply piece movement manually (without running applyMove's full
        // elimination logic). This is a deliberate simplification: we only need to evaluate
        // wall candidates, and the piece-move-triggered elimination edge case is rare enough
        // that the heuristic quality is not materially affected.
        val testGame = game.clone()
        if (movement != null) testGame.pieces[movement.first].position = movement.second

        val myPositions = testGame.pieces
            .filter { it.ownerIndex == testGame.currentPlayerIndex }
            .map { it.position }.toSet()
        val opponentPositions = testGame.pieces
            .filter { it.ownerIndex != testGame.currentPlayerIndex }
            .map { it.position }.toSet()

        // Sample up to 10 candidate walls and classify them
        val available = testGame.availableWalls()
        val candidates = available.shuffled().take(minOf(10, available.size))

        fun wallRegions(wall: EdgeCoordinate): List<Set<SquareCoordinate>> {
            val g = testGame.clone()
            g.addWall(wall)
            return g.sliceIntoRegions().filter { it.size <= 8 }
        }

        // Prefer: kills opponent-only region
        val killerWall = candidates.firstOrNull { wall ->
            val smallRegions = wallRegions(wall)
            smallRegions.any { region -> region.any { it in opponentPositions } } &&
            smallRegions.none { region -> region.any { it in myPositions } }
        }
        if (killerWall != null) return SimMove(movement?.first, movement?.second, killerWall)

        // Otherwise: avoid self-killing wall
        val safeWall = candidates.firstOrNull { wall ->
            val smallRegions = wallRegions(wall)
            smallRegions.none { region -> region.any { it in myPositions } }
        }
        val wallPosition = safeWall ?: candidates.first()

        return SimMove(movement?.first, movement?.second, wallPosition)
    }
}
