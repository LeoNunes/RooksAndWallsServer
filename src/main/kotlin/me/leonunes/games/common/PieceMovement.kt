package me.leonunes.games.common

interface PieceMovement<TCoordinate> {
    fun canMoveTo(destination: TCoordinate) : Boolean = destination in getPossibleDestinations()
    fun getPossibleDestinations() : Set<TCoordinate>
}

class PieceMovementUnion<TCoordinate>(private vararg val movements: PieceMovement<TCoordinate>) : PieceMovement<TCoordinate> {
    override fun canMoveTo(destination: TCoordinate): Boolean {
        return movements.any { it.canMoveTo(destination) }
    }

    override fun getPossibleDestinations(): Set<TCoordinate> {
        return movements.fold(setOf()) { acc, movement -> acc.union(movement.getPossibleDestinations()) }
    }
}

class PieceMovementIntersection<TCoordinate>(private vararg val movements: PieceMovement<TCoordinate>) : PieceMovement<TCoordinate> {
    override fun canMoveTo(destination: TCoordinate): Boolean {
        return movements.all { it.canMoveTo(destination) }
    }

    override fun getPossibleDestinations(): Set<TCoordinate> {
        return movements
            .map { it.getPossibleDestinations() }
            .reduceOrNull { acc, current -> acc.intersect(current) } ?: setOf()
    }
}
