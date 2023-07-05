package me.leonunes.common

val linearMovementDirections = setOf(coordStep(0, 1), coordStep(0, -1), coordStep(1, 0), coordStep(-1, 0))
val diagonalMovementDirections = setOf(coordStep(1, 1), coordStep(1, -1), coordStep(-1, 1), coordStep(-1, -1))

open class SteppedMovement<TCoord, TPlaceable : BoardPlaceable<TCoord>, TBoard : Board<*>>(
    val piece: TPlaceable,
    val board: TBoard,
    val possibleSteps: Set<Step<TCoord>>,
) : PieceMovement<TCoord> {

    private val _validations = mutableListOf<StepValidation<TCoord, TPlaceable, TBoard>>()
    val validations
        get() = _validations.toList()

    // TODO: Add conditions to addStepValidation. If the condition is met the validation is executed
    // A condition can have a type (TCondition.() -> Boolean) and getPossibleDestinations executes it before adding the validation
    // getPossibleDestinations and canMoveTo would need to have a TCondition parameter
    // This probably needs to be done in the board interface, leaving both Board<TCoord> and Boar<TCoord, TCondition>
    fun addStepValidation(validation: StepValidation<TCoord, TPlaceable, TBoard>) {
        _validations.add(validation)
    }

    override fun getPossibleDestinations(): Set<TCoord> {
        val destinations = mutableSetOf<TCoord>()
        for (step in possibleSteps) {
            val initialPosition = piece.position
            var currentPosition = initialPosition
            var index = 0

            while (true) {
                val nextPosition = step.takeStep(currentPosition)
                val params = StepValidationParameters(index, initialPosition, currentPosition, nextPosition, piece, board)
                val result = validateStep(params)

                if (result.valid) destinations.add(nextPosition)
                if (result.terminal) break

                index++
                currentPosition = nextPosition
            }
        }

        return destinations
    }

    private fun validateStep(parameters: StepValidationParameters<TCoord, TPlaceable, TBoard>) : StepValidationResult {
        var valid = true
        var terminal = false
        for (validation in validations) {
            val partialResult = validation.validate(parameters)
            valid = partialResult.valid && valid
            terminal = partialResult.terminal || terminal

            if (!valid && terminal) break
        }

        return StepValidationResult.getBy(valid, terminal)
    }
}

fun interface StepValidation<TCoord, TPlaceable : BoardPlaceable<TCoord>, TBoard : Board<*>> {
    fun validate(parameters: StepValidationParameters<TCoord, TPlaceable, TBoard>) : StepValidationResult
}

class StepValidationParameters<TCoord, TPlaceable : BoardPlaceable<TCoord>, TBoard : Board<*>>(
    val stepIndex: Int,
    val initialPosition: TCoord,
    val currentPosition: TCoord,
    val nextPosition: TCoord,
    val piece: TPlaceable,
    val board: TBoard,
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


class MaxNumberOfStepsStepValidation<TCoord, TPlaceable : BoardPlaceable<TCoord>, TBoard : Board<*>>(
    val max: Int
) : StepValidation<TCoord, TPlaceable, TBoard> {
    override fun validate(parameters: StepValidationParameters<TCoord, TPlaceable, TBoard>): StepValidationResult {
        with(parameters) {
            return if (stepIndex < max) StepValidationResult.Valid else StepValidationResult.InvalidTerminal
        }
    }
}
fun <TCoord, TPlaceable : BoardPlaceable<TCoord>, TBoard : Board<TCoord>> SteppedMovement<TCoord, TPlaceable, TBoard>.validateMaxNumberOfSteps(max: Int) {
    addStepValidation(MaxNumberOfStepsStepValidation(max))
}

class InsideBoardStepValidation<TCoord, TPlaceable : BoardPlaceable<TCoord>, TBoard : Board<TCoord>> : StepValidation<TCoord, TPlaceable, TBoard> {
    override fun validate(parameters: StepValidationParameters<TCoord, TPlaceable, TBoard>): StepValidationResult {
        with(parameters) {
            return if (board.isInsideBoard(nextPosition)) StepValidationResult.Valid else StepValidationResult.InvalidTerminal
        }
    }
}
fun <TCoord, TPlaceable : BoardPlaceable<TCoord>, TBoard : Board<TCoord>> SteppedMovement<TCoord, TPlaceable, TBoard>.validateInsideBoard() {
    addStepValidation(InsideBoardStepValidation())
}

class BlockedByPiecesStepValidation<TCoord, TPlaceable : BoardPlaceable<TCoord>, TBoard>(
    private val considerPiece: (TPlaceable, StepValidationParameters<TCoord, TPlaceable, TBoard>) -> Boolean = { _, _ -> true }
) : StepValidation<TCoord, TPlaceable, TBoard> where TBoard: Board<*>, TBoard : WithPieces<TPlaceable>{
    override fun validate(parameters: StepValidationParameters<TCoord, TPlaceable, TBoard>): StepValidationResult {
        with(parameters) {
            val piece = board.pieces.find { it.position == nextPosition }
            if (piece != null && considerPiece(piece, this))
                return StepValidationResult.InvalidTerminal
            return StepValidationResult.Valid
        }
    }
}
fun <TCoord, TPlaceable : BoardPlaceable<TCoord>, TBoard> SteppedMovement<TCoord, TPlaceable, TBoard>.validateBlockedByPieces(
    considerPiece: (TPlaceable, StepValidationParameters<TCoord, TPlaceable, TBoard>) -> Boolean = { _, _ -> true }
)  where TBoard: Board<*>, TBoard : WithPieces<TPlaceable> {

    addStepValidation(BlockedByPiecesStepValidation(considerPiece))
}

class BlockedByWallsStepValidation<TPlaceable: BoardPlaceable<SquareCoordinate>, TWall: BoardPlaceable<EdgeCoordinate>, TBoard>
    : StepValidation<SquareCoordinate, TPlaceable, TBoard> where TBoard : WithWalls<TWall>, TBoard: Board<*> {
    override fun validate(parameters: StepValidationParameters<SquareCoordinate, TPlaceable, TBoard>): StepValidationResult {
        with(parameters) {
            val edgePosition = EdgeCoordinate(currentPosition, nextPosition)
            if (board.walls.find { it.position == edgePosition } != null)
                return StepValidationResult.InvalidTerminal
            return StepValidationResult.Valid
        }
    }
}
fun <TPlaceable: BoardPlaceable<SquareCoordinate>, TWall: BoardPlaceable<EdgeCoordinate>, TBoard>
    SteppedMovement<SquareCoordinate, TPlaceable, TBoard>.validateBlockedByWalls() where TBoard : WithWalls<TWall>, TBoard : Board<*> {

    addStepValidation(BlockedByWallsStepValidation())
}
