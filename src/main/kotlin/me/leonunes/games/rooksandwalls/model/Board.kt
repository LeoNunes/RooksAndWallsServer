package me.leonunes.games.rooksandwalls.model

import me.leonunes.games.common.GridBoard
import me.leonunes.games.common.WithPieces
import me.leonunes.games.common.WithWalls

class Board(override val rows: Int, override val columns: Int) : GridBoard, WithPieces<Piece>, WithWalls<Wall> {
    override val pieces = mutableListOf<Piece>()
    override val walls = mutableListOf<Wall>()
    val deadPieces = mutableListOf<Piece>()
}
