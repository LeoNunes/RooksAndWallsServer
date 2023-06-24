package me.leonunes.model

import me.leonunes.common.SquareCoordinate

// TODO: make it thread safe

class Game {
    private val playerTurn: Int = 0

    private val players = mutableListOf<Player>()
    private val pieces = mutableListOf<Piece>(
        Piece(Player(1.asId(), 0), SquareCoordinate(1,2))
    )

    fun getPieces() : List<Piece> = pieces.toList()
}

data class Piece(val owner: Player, val position: SquareCoordinate)

data class Player(val id: Id<Player>, val number: Int)

interface Id<T> {
    fun get(): Int
}
class IdImp<T>(private val value: Int) : Id<T> {
    override fun get(): Int {
        return value
    }
}
fun <T> Int.asId() : Id<T> {
    return IdImp(this)
}
