package me.leonunes.common

interface PieceMovement<TCoordinate> {
    fun canMoveTo(destination: TCoordinate) : Boolean
    fun getPossibleDestinations() : Set<TCoordinate>
}
