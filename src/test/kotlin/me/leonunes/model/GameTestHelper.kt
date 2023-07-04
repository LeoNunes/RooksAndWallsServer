package me.leonunes.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import me.leonunes.common.EdgeCoordinate
import me.leonunes.common.coord

fun createGameWithPlayers() : Game = runBlocking {
    GameFactory.createGame().apply {
        joinGame()
        joinGame()
        joinGame()
    }
}

fun Game.addPieceActions() : List<AddPieceAction> {
    val player1 = players[0].id
    val player2 = players[1].id
    val player3 = players[2].id

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
    val pieces = piecesByPlayer()

    val player1 = players[0].id
    val piece1_1 = pieces[player1]!![0].id // Start at: (0, 0)
    val piece1_2 = pieces[player1]!![1].id // Start at: (0, 1)
    val piece1_3 = pieces[player1]!![2].id // Start at: (0, 2)

    val player2 = players[1].id
    val piece2_1 = pieces[player2]!![0].id // Start at: (4, 0)
    val piece2_2 = pieces[player2]!![1].id // Start at: (4, 1)
    val piece2_3 = pieces[player2]!![2].id // Start at: (4, 2)

    val player3 = players[2].id
    val piece3_1 = pieces[player3]!![0].id // Start at: (7, 0)
    val piece3_2 = pieces[player3]!![1].id // Start at: (7, 1)
    val piece3_3 = pieces[player3]!![2].id // Start at: (7, 2)

    return listOf(
        MoveAction(player1, piece1_3, coord(0, 7), EdgeCoordinate(coord(0, 0), coord(1, 0))),
        MoveAction(player2, piece2_3, coord(4, 7), EdgeCoordinate(coord(0, 1), coord(1, 1))),
        MoveAction(player3, piece3_3, coord(7, 7), EdgeCoordinate(coord(0, 2), coord(1, 2))),
        MoveAction(player1, piece1_2, coord(0, 6), EdgeCoordinate(coord(0, 3), coord(1, 3))),
        MoveAction(player2, piece2_2, coord(4, 6), EdgeCoordinate(coord(0, 4), coord(1, 4))),
        MoveAction(player3, piece3_2, coord(7, 6), EdgeCoordinate(coord(0, 5), coord(1, 5))),
        MoveAction(player1, piece1_1, coord(0, 5), EdgeCoordinate(coord(0, 6), coord(1, 6))),
        MoveAction(player2, piece2_1, coord(4, 5), EdgeCoordinate(coord(0, 5), coord(0, 6))), // piece1_1 dies
        MoveAction(player3, piece3_1, coord(7, 5), EdgeCoordinate(coord(0, 7), coord(1, 7))), // player1 eliminated
        MoveAction(player2, piece2_1, coord(4, 0), EdgeCoordinate(coord(6, 0), coord(7, 0))),
        MoveAction(player3, piece3_1, coord(7, 0), EdgeCoordinate(coord(6, 1), coord(7, 1))),
        MoveAction(player2, piece2_2, coord(4, 1), EdgeCoordinate(coord(6, 2), coord(7, 2))),
        MoveAction(player3, piece3_2, coord(7, 1), EdgeCoordinate(coord(6, 3), coord(7, 3))),
        MoveAction(player2, piece2_3, coord(4, 2), EdgeCoordinate(coord(6, 4), coord(7, 4))),
        MoveAction(player3, piece3_3, coord(7, 2), EdgeCoordinate(coord(6, 5), coord(7, 5))),
        MoveAction(player2, piece2_3, coord(4, 7), EdgeCoordinate(coord(6, 6), coord(7, 6))),
        MoveAction(player3, piece3_3, coord(7, 7), EdgeCoordinate(coord(6, 7), coord(7, 7))),
    )
}

fun Game.runWinningMovePieceActions() = runBlocking {
    winningMovePieceActions().forEach { processAction(it) }
}

fun Game.drawingMovePieceActions() : List<MoveAction> {
    val pieces = piecesByPlayer()

    val player1 = players[0].id
    val piece1_1 = pieces[player1]!![0].id // Start at: (0, 0)
    val piece1_2 = pieces[player1]!![1].id // Start at: (0, 1)
    val piece1_3 = pieces[player1]!![2].id // Start at: (0, 2)

    val player2 = players[1].id
    val piece2_1 = pieces[player2]!![0].id // Start at: (4, 0)
    val piece2_2 = pieces[player2]!![1].id // Start at: (4, 1)
    val piece2_3 = pieces[player2]!![2].id // Start at: (4, 2)

    val player3 = players[2].id
    val piece3_1 = pieces[player3]!![0].id // Start at: (7, 0)
    val piece3_2 = pieces[player3]!![1].id // Start at: (7, 1)
    val piece3_3 = pieces[player3]!![2].id // Start at: (7, 2)

    return listOf(
        MoveAction(player1, piece1_3, coord(0, 7), EdgeCoordinate(coord(0, 0), coord(1, 0))),
        MoveAction(player2, piece2_3, coord(4, 7), EdgeCoordinate(coord(0, 1), coord(1, 1))),
        MoveAction(player3, piece3_3, coord(7, 3), EdgeCoordinate(coord(0, 2), coord(1, 2))),
        MoveAction(player1, piece1_2, coord(0, 6), EdgeCoordinate(coord(0, 3), coord(1, 3))),
        MoveAction(player2, piece2_2, coord(4, 6), EdgeCoordinate(coord(0, 4), coord(1, 4))),
        MoveAction(player3, piece3_2, coord(7, 2), EdgeCoordinate(coord(0, 5), coord(1, 5))),
        MoveAction(player1, piece1_1, coord(0, 5), EdgeCoordinate(coord(0, 6), coord(1, 6))),
        MoveAction(player2, piece2_1, coord(4, 5), EdgeCoordinate(coord(0, 5), coord(0, 6))), // piece1_1 dies
        MoveAction(player3, piece3_1, coord(7, 1), EdgeCoordinate(coord(0, 7), coord(1, 7))), // player1 eliminated
        MoveAction(player2, piece2_1, coord(7, 5), EdgeCoordinate(coord(6, 0), coord(7, 0))),
        MoveAction(player3, piece3_1, coord(7, 0), EdgeCoordinate(coord(6, 1), coord(7, 1))),
        MoveAction(player2, piece2_2, coord(7, 6), EdgeCoordinate(coord(6, 2), coord(7, 2))),
        MoveAction(player3, piece3_2, coord(7, 1), EdgeCoordinate(coord(6, 3), coord(7, 3))),
        MoveAction(player2, piece2_3, coord(7, 7), EdgeCoordinate(coord(6, 4), coord(7, 4))),
        MoveAction(player3, piece3_3, coord(7, 2), EdgeCoordinate(coord(6, 5), coord(7, 5))),
        MoveAction(player2, piece2_1, coord(7, 4), EdgeCoordinate(coord(6, 6), coord(7, 6))),
        MoveAction(player3, piece3_3, coord(7, 3), EdgeCoordinate(coord(6, 7), coord(7, 7))),
    )
}

fun Game.runDrawingMovePieceActions() = runBlocking {
    drawingMovePieceActions().forEach { processAction(it) }
}

fun Game.piecesByPlayer() : Map<PlayerId, List<Piece>> {
    return pieces.groupBy { it.owner.id }
}

@ExperimentalCoroutinesApi
suspend fun ReceiveChannel<GameUpdate>.receiveInstant() : GameUpdate? {
    return if (isEmpty) null else receive()
}
