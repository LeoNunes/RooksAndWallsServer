package me.leonunes.games.rooksandwalls.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.coord
import me.leonunes.games.users.GuestUser

fun createGameWithPlayers(config: GameConfig = GameConfigDefaultValues): Game = runBlocking {
    val game = GameFactory.createGame(config)
    val players = (0 until config.numberOfPlayers).map { Player(GuestUser("player-$it")) }
    game.start(players)
    game
}

/**
 * Final Result:
 * piece1_1: coord(0, 0)),
 * piece1_2: coord(0, 1)),
 * piece1_3: coord(0, 2)),
 * piece2_1: coord(4, 0)),
 * piece2_2: coord(4, 1)),
 * piece2_3: coord(4, 2)),
 * piece3_1: coord(7, 0)),
 * piece3_2: coord(7, 1)),
 * piece3_3: coord(7, 2)),
 */
fun Game.addPieceActions() : List<AddPieceAction> {
    return listOf(
        AddPieceAction(player1, coord(0, 0)),
        AddPieceAction(player2, coord(4, 0)),
        AddPieceAction(player3, coord(7, 0)),
        AddPieceAction(player3, coord(7, 1)),
        AddPieceAction(player2, coord(4, 1)),
        AddPieceAction(player1, coord(0, 1)),
        AddPieceAction(player1, coord(0, 2)),
        AddPieceAction(player2, coord(4, 2)),
        AddPieceAction(player3, coord(7, 2)),
    )
}

fun Game.runAddPieceActions() = runBlocking {
    addPieceActions().forEach { processAction(it) }
}

fun Game.winningMovePieceActions() : List<MoveAction> {
    return listOf(
        MoveAction(player1, PieceMovement(piece1_3, coord(0, 7)), WallPlacement(EdgeCoordinate(coord(0, 0), coord(1, 0)))),
        MoveAction(player2, PieceMovement(piece2_3, coord(4, 7)), WallPlacement(EdgeCoordinate(coord(0, 1), coord(1, 1)))),
        MoveAction(player3, PieceMovement(piece3_3, coord(7, 7)), WallPlacement(EdgeCoordinate(coord(0, 2), coord(1, 2)))),
        MoveAction(player1, PieceMovement(piece1_2, coord(0, 6)), WallPlacement(EdgeCoordinate(coord(0, 3), coord(1, 3)))),
        MoveAction(player2, PieceMovement(piece2_2, coord(4, 6)), WallPlacement(EdgeCoordinate(coord(0, 4), coord(1, 4)))),
        MoveAction(player3, PieceMovement(piece3_2, coord(7, 6)), WallPlacement(EdgeCoordinate(coord(0, 5), coord(1, 5)))),
        MoveAction(player1, PieceMovement(piece1_1, coord(0, 5)), WallPlacement(EdgeCoordinate(coord(0, 6), coord(1, 6)))),
        MoveAction(player2, PieceMovement(piece2_1, coord(4, 5)), WallPlacement(EdgeCoordinate(coord(0, 5), coord(0, 6)))),
        MoveAction(player3, PieceMovement(piece3_1, coord(7, 5)), WallPlacement(EdgeCoordinate(coord(0, 7), coord(1, 7)))),
        MoveAction(player2, PieceMovement(piece2_1, coord(4, 0)), WallPlacement(EdgeCoordinate(coord(6, 0), coord(7, 0)))),
        MoveAction(player3, PieceMovement(piece3_1, coord(7, 0)), WallPlacement(EdgeCoordinate(coord(6, 1), coord(7, 1)))),
        MoveAction(player2, PieceMovement(piece2_2, coord(4, 1)), WallPlacement(EdgeCoordinate(coord(6, 2), coord(7, 2)))),
        MoveAction(player3, PieceMovement(piece3_2, coord(7, 1)), WallPlacement(EdgeCoordinate(coord(6, 3), coord(7, 3)))),
        MoveAction(player2, PieceMovement(piece2_3, coord(4, 2)), WallPlacement(EdgeCoordinate(coord(6, 4), coord(7, 4)))),
        MoveAction(player3, PieceMovement(piece3_3, coord(7, 2)), WallPlacement(EdgeCoordinate(coord(6, 5), coord(7, 5)))),
        MoveAction(player2, PieceMovement(piece2_3, coord(4, 7)), WallPlacement(EdgeCoordinate(coord(6, 6), coord(7, 6)))),
        MoveAction(player3, PieceMovement(piece3_3, coord(7, 7)), WallPlacement(EdgeCoordinate(coord(6, 7), coord(7, 7)))),
    )
}

fun Game.runWinningMovePieceActions() = runBlocking {
    winningMovePieceActions().forEach { processAction(it) }
}

fun Game.drawingMovePieceActions() : List<MoveAction> {
    return listOf(
        MoveAction(player1, PieceMovement(piece1_3, coord(0, 7)), WallPlacement(EdgeCoordinate(coord(0, 0), coord(1, 0)))),
        MoveAction(player2, PieceMovement(piece2_3, coord(4, 7)), WallPlacement(EdgeCoordinate(coord(0, 1), coord(1, 1)))),
        MoveAction(player3, PieceMovement(piece3_3, coord(7, 3)), WallPlacement(EdgeCoordinate(coord(0, 2), coord(1, 2)))),
        MoveAction(player1, PieceMovement(piece1_2, coord(0, 6)), WallPlacement(EdgeCoordinate(coord(0, 3), coord(1, 3)))),
        MoveAction(player2, PieceMovement(piece2_2, coord(4, 6)), WallPlacement(EdgeCoordinate(coord(0, 4), coord(1, 4)))),
        MoveAction(player3, PieceMovement(piece3_2, coord(7, 2)), WallPlacement(EdgeCoordinate(coord(0, 5), coord(1, 5)))),
        MoveAction(player1, PieceMovement(piece1_1, coord(0, 5)), WallPlacement(EdgeCoordinate(coord(0, 6), coord(1, 6)))),
        MoveAction(player2, PieceMovement(piece2_1, coord(4, 5)), WallPlacement(EdgeCoordinate(coord(0, 5), coord(0, 6)))),
        MoveAction(player3, PieceMovement(piece3_1, coord(7, 1)), WallPlacement(EdgeCoordinate(coord(0, 7), coord(1, 7)))),
        MoveAction(player2, PieceMovement(piece2_1, coord(7, 5)), WallPlacement(EdgeCoordinate(coord(6, 0), coord(7, 0)))),
        MoveAction(player3, PieceMovement(piece3_1, coord(7, 0)), WallPlacement(EdgeCoordinate(coord(6, 1), coord(7, 1)))),
        MoveAction(player2, PieceMovement(piece2_2, coord(7, 6)), WallPlacement(EdgeCoordinate(coord(6, 2), coord(7, 2)))),
        MoveAction(player3, PieceMovement(piece3_2, coord(7, 1)), WallPlacement(EdgeCoordinate(coord(6, 3), coord(7, 3)))),
        MoveAction(player2, PieceMovement(piece2_3, coord(7, 7)), WallPlacement(EdgeCoordinate(coord(6, 4), coord(7, 4)))),
        MoveAction(player3, PieceMovement(piece3_3, coord(7, 2)), WallPlacement(EdgeCoordinate(coord(6, 5), coord(7, 5)))),
        MoveAction(player2, PieceMovement(piece2_1, coord(7, 4)), WallPlacement(EdgeCoordinate(coord(6, 6), coord(7, 6)))),
        MoveAction(player3, PieceMovement(piece3_3, coord(7, 3)), WallPlacement(EdgeCoordinate(coord(6, 7), coord(7, 7)))),
    )
}

fun Game.runDrawingMovePieceActions() = runBlocking {
    drawingMovePieceActions().forEach { processAction(it) }
}

fun Game.piecesByPlayer() : Map<PlayerId, List<Piece>> {
    return pieces.groupBy { it.owner.id }
}

val Game.player1 get() = players[0].id
val Game.player2 get() = players[1].id
val Game.player3 get() = players[2].id
val Game.piece1_1 get() = piecesByPlayer()[player1]!![0].id
val Game.piece1_2 get() = piecesByPlayer()[player1]!![1].id
val Game.piece1_3 get() = piecesByPlayer()[player1]!![2].id
val Game.piece2_1 get() = piecesByPlayer()[player2]!![0].id
val Game.piece2_2 get() = piecesByPlayer()[player2]!![1].id
val Game.piece2_3 get() = piecesByPlayer()[player2]!![2].id
val Game.piece3_1 get() = piecesByPlayer()[player3]!![0].id
val Game.piece3_2 get() = piecesByPlayer()[player3]!![1].id
val Game.piece3_3 get() = piecesByPlayer()[player3]!![2].id

@ExperimentalCoroutinesApi
suspend fun ReceiveChannel<GameUpdate>.receiveInstant() : GameUpdate? {
    return if (isEmpty) null else receive()
}
