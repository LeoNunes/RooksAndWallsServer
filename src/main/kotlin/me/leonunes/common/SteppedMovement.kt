package me.leonunes.common

open class SteppedMovement<TCoordinate, TBoard : Board<TCoordinate>>(
    private val piece: BoardPlaceable<TCoordinate>,
    private val board: TBoard,
    private val possibleSteps: Set<Step<TCoordinate>>,
    configureStepValidation: StepValidationScope<TCoordinate, TBoard>.() -> Unit) : PieceMovement<TCoordinate> {

    private val validator = StepValidator<TCoordinate, TBoard>()
    init {
        val process = StepValidationScope(validator)
        configureStepValidation(process)
    }

    override fun getPossibleDestinations(): Set<TCoordinate> {
        val destinations = mutableSetOf<TCoordinate>()
        for (step in possibleSteps) {
            val initialPosition = piece.position
            var currentPosition = initialPosition
            var index = 0

            while (true) {
                val nextPosition = step.takeStep(currentPosition)
                val params = StepValidationParameters(index, initialPosition, currentPosition, nextPosition, board)
                val result = validator.validate(params)

                if (result.valid) destinations.add(nextPosition)
                if (result.terminal) break

                index++
                currentPosition = nextPosition
            }
        }

        return destinations
    }
}

class StepValidationScope<TCoordinate, TBoard : Board<TCoordinate>>(private val validator: StepValidator<TCoordinate, TBoard>) {
    fun validate(condition: StepValidationParameters<TCoordinate, TBoard>.() -> StepValidationResult) {
        validator.addCondition(condition)
    }
}

class StepValidator<TCoordinate, TBoard : Board<TCoordinate>> {
    private val conditions = mutableListOf<(StepValidationParameters<TCoordinate, TBoard>) -> StepValidationResult>()

    fun addCondition(condition: (StepValidationParameters<TCoordinate, TBoard>) -> StepValidationResult) {
        conditions.add(condition)
    }

    fun validate(parameters: StepValidationParameters<TCoordinate, TBoard>) : StepValidationResult {
        var valid = true
        var terminal = false
        for (condition in conditions) {
            val partialResult = condition(parameters)
            valid = partialResult.valid && valid
            terminal = partialResult.terminal || terminal

            if (!valid && terminal) break
        }

        return StepValidationResult.getBy(valid, terminal)
    }
}

class StepValidationParameters<TCoordinate, TBoard : Board<TCoordinate>>(
    val stepIndex: Int,
    val initialPosition: TCoordinate,
    val currentPosition: TCoordinate,
    val nextPosition: TCoordinate,
    val board: TBoard
)

enum class StepValidationResult(val valid: Boolean, val terminal: Boolean) {
    // Movement is valid, but piece could move further
    Valid(valid = true, terminal = false),
    // Movement is invalid, but piece could move further
    Invalid(valid = false, terminal = false),
    // Movement is valid, but piece can't move further
    ValidTerminal(valid = true, terminal = true),
    // Movement is invalid, but piece can't move further
    InvalidTerminal(valid = false, terminal = true);

    companion object {
        fun getBy(valid: Boolean, terminal: Boolean) : StepValidationResult {
            return when {
                valid && !terminal -> Valid
                !valid && !terminal -> Invalid
                valid && terminal -> ValidTerminal
                else -> InvalidTerminal
            }
        }
    }
}

val linearMovementDirections = setOf(coordStep(0, 1), coordStep(0, -1), coordStep(1, 0), coordStep(-1, 0))
val diagonalMovementDirections = setOf(coordStep(1, 1), coordStep(1, -1), coordStep(-1, 1), coordStep(-1, -1))

fun <TCoordinate, TBoard : Board<TCoordinate>> StepValidationScope<TCoordinate, TBoard>.validateMaxNumberOfSteps(max: Int) {
    validate {
        if (stepIndex < max) StepValidationResult.Valid else StepValidationResult.InvalidTerminal
    }
}

fun <TCoordinate, TBoard : Board<TCoordinate>> StepValidationScope<TCoordinate, TBoard>.validateInsideBoard() {
    validate {
        if (board.isInsideBoard(nextPosition)) StepValidationResult.Valid else StepValidationResult.InvalidTerminal
    }
}

fun <TCoordinate, TBoard : Board<TCoordinate>> StepValidationScope<TCoordinate, TBoard>.validateBlockedByPieces() {
    validate {
        if (board.pieces.find { it.position == nextPosition } != null)
            return@validate StepValidationResult.InvalidTerminal
        return@validate StepValidationResult.Valid
    }
}

fun <TBoard : GridBoardWithWalls> StepValidationScope<SquareCoordinate, TBoard>.validateBlockedByWalls() {
    validate {
        val edgePosition = EdgeCoordinate(currentPosition, nextPosition)
        if (board.walls.find { it.position == edgePosition } != null)
            return@validate StepValidationResult.InvalidTerminal
        return@validate StepValidationResult.Valid
    }
}
