package me.leonunes.common

import kotlinx.serialization.Serializable
import kotlin.math.abs

interface Step<TCoordinate> {
    fun takeStep(fromPosition: TCoordinate) : TCoordinate
}

@Serializable
data class SquareCoordinate(val row: Int, val column: Int) {
    fun isAdjacentTo(other: SquareCoordinate) : Boolean {
        return (this.row == other.row && abs(this.column - other.column) == 1) ||
                (this.column == other.column && abs(this.row - other.row) == 1)
    }
}

@Serializable
data class EdgeCoordinate(val square1: SquareCoordinate, val square2: SquareCoordinate) {
    init {
        if (!square1.isAdjacentTo(square2)) {
            throw IllegalArgumentException("EdgeCoordinate with parameters $square1 and $square2 is invalid")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EdgeCoordinate) return false
        return (
                (square1 == other.square1 && square2 == other.square2) ||
                (square1 == other.square2 && square2 == other.square1)
        )
    }

    override fun hashCode(): Int {
        listOf(square1, square2).sortedWith { it, other ->
            if (it.row == other.row ) it.column - other.column else it.row - other.row
        }.let {
            return 31 * it[0].hashCode() + it[1].hashCode()
        }
    }
}

@Serializable
data class SquareCoordinateStep(val rowDelta: Int, val columnDelta: Int) : Step<SquareCoordinate> {
    override fun takeStep(fromPosition: SquareCoordinate): SquareCoordinate {
        return SquareCoordinate(fromPosition.row + rowDelta, fromPosition.column + columnDelta)
    }
}

fun coord(x: Int, y: Int) = SquareCoordinate(x, y)
fun coordStep(x: Int, y: Int) = SquareCoordinateStep(x, y)

fun SquareCoordinate.edgeUp() = EdgeCoordinate(this, coord(row - 1, column))
fun SquareCoordinate.edgeDown() = EdgeCoordinate(this, coord(row + 1, column))
fun SquareCoordinate.edgeLeft() = EdgeCoordinate(this, coord(row, column - 1))
fun SquareCoordinate.edgeRight() = EdgeCoordinate(this, coord(row, column + 1))

operator fun SquareCoordinateStep.plus(coordinate: SquareCoordinate) : SquareCoordinate {
    return takeStep(coordinate)
}
operator fun SquareCoordinate.plus(step: SquareCoordinateStep) : SquareCoordinate {
    return step.takeStep(this)
}
operator fun SquareCoordinate.minus(other: SquareCoordinate) : SquareCoordinateStep {
    return SquareCoordinateStep(row - other.row, column - other.column)
}
