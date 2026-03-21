package me.leonunes.games.rooksandwalls.ai

import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.SquareCoordinate
import me.leonunes.games.common.coord
import me.leonunes.games.rooksandwalls.model.AddPieceAction
import me.leonunes.games.rooksandwalls.model.Game
import me.leonunes.games.rooksandwalls.model.GameAction
import me.leonunes.games.rooksandwalls.model.GameStage
import me.leonunes.games.rooksandwalls.model.MoveAction
import me.leonunes.games.rooksandwalls.model.PieceMovement
import me.leonunes.games.common.player.PlayerNumber
import me.leonunes.games.rooksandwalls.model.WallPlacement

interface AiStrategy {
    fun chooseAction(game: Game, playerNumber: PlayerNumber): GameAction
}

class RandomAiStrategy : AiStrategy {
    override fun chooseAction(game: Game, playerNumber: PlayerNumber): GameAction {
        return when (game.gameStage) {
            GameStage.PiecePlacement -> choosePiecePlacement(game, playerNumber)
            GameStage.Moves -> chooseMoveAction(game, playerNumber)
            else -> throw IllegalStateException("AI asked to act in stage ${game.gameStage}")
        }
    }

    private fun choosePiecePlacement(game: Game, playerNumber: PlayerNumber): AddPieceAction {
        val occupied = game.pieces.map { it.position }.toSet()
        val empty = allSquares(game).filter { it !in occupied }
        return AddPieceAction(playerNumber, empty.random())
    }

    private fun chooseMoveAction(game: Game, playerNumber: PlayerNumber): MoveAction {
        val myPieces = game.pieces.filter { it.owner == playerNumber }
        val moves = myPieces.flatMap { piece ->
            piece.movement.getPossibleDestinations().map { dest -> piece to dest }
        }
        val pieceMovement = if (moves.isNotEmpty()) {
            val (piece, dest) = moves.random()
            PieceMovement(piece.id, dest)
        } else null

        val occupiedWalls = game.walls.map { it.position }.toSet()
        val availableWalls = allEdges(game).filter { it !in occupiedWalls }
        val wallPosition = availableWalls.random()

        return MoveAction(playerNumber, pieceMovement, WallPlacement(wallPosition))
    }

    private fun allSquares(game: Game): List<SquareCoordinate> =
        (0 until game.config.boardRows).flatMap { row ->
            (0 until game.config.boardColumns).map { col -> coord(row, col) }
        }

    private fun allEdges(game: Game): List<EdgeCoordinate> =
        (0 until game.config.boardRows).flatMap { row ->
            (0 until game.config.boardColumns).flatMap { col ->
                buildList {
                    if (row + 1 < game.config.boardRows)
                        add(EdgeCoordinate(coord(row, col), coord(row + 1, col)))
                    if (col + 1 < game.config.boardColumns)
                        add(EdgeCoordinate(coord(row, col), coord(row, col + 1)))
                }
            }
        }
}
