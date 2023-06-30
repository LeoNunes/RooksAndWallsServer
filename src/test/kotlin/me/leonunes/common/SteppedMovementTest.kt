package me.leonunes.common

import kotlin.test.*

class SteppedMovementTest {
    @Test
    fun `movement must be inside board`() {
        val movement = createMovement(coord(1, 2), 4) {
            validateInsideBoard()
        }

        assertEquals(setOf(
            coord(1, 3),
            coord(1, 1),
            coord(1, 0),
            coord(2, 2),
            coord(3, 2),
            coord(0, 2)
        ), movement.getPossibleDestinations())

        assertTrue(movement.canMoveTo(coord(1, 0)))
        assertFalse(movement.canMoveTo(coord(3, 1)))
    }

    @Test
    fun `movement is blocked by pieces`() {
        val movement = createMovement(coord(1, 2), 4, pieces=listOf(coord(1, 1))) {
            validateInsideBoard()
            validateBlockedByPieces()
        }

        assertEquals(setOf(
            coord(1, 3),
            coord(2, 2),
            coord(3, 2),
            coord(0, 2)
        ), movement.getPossibleDestinations())
    }

    @Test
    fun `movement is limited by number of steps`() {
        val movement = createMovement(coord(1, 2), 4) {
            validateInsideBoard()
            validateMaxNumberOfSteps(1)
        }

        assertEquals(setOf(
            coord(1, 3),
            coord(1, 1),
            coord(2, 2),
            coord(0, 2)
        ), movement.getPossibleDestinations())
    }

    @Test
    fun `movement is blocked by walls`() {
        val movement = createMovement(coord(1, 2), 4, walls=listOf(EdgeCoordinate(coord(1, 0), coord(1, 1)))) {
            validateInsideBoard()
            validateBlockedByWalls()
        }

        assertEquals(setOf(
            coord(1, 3),
            coord(1, 1),
            coord(2, 2),
            coord(3, 2),
            coord(0, 2)
        ), movement.getPossibleDestinations())
    }

    @Test
    fun `movement can skip positions`() {
        val movement = createMovement(coord(1, 0), 8, directions = setOf(coordStep(0, 1))) {
            validateInsideBoard()

            validate {
                if (stepIndex % 2 == 0) StepValidationResult.Invalid else StepValidationResult.Valid
            }
        }

        assertEquals(setOf(
            coord(1, 2),
            coord(1, 4),
            coord(1, 6)
        ), movement.getPossibleDestinations())
    }

    private fun createMovement(
        initialPosition : SquareCoordinate,
        boardSize: Int,
        pieces: List<SquareCoordinate> = emptyList(),
        walls: List<EdgeCoordinate> = emptyList(),
        directions: Set<SquareCoordinateStep> = linearMovementDirections,
        configureStepValidation: StepValidationProcess<SquareCoordinate, GridBoardWithWalls>.() -> Unit
    ) : SteppedMovement<SquareCoordinate, GridBoardWithWalls> {

        return SteppedMovement(
            piece = object : BoardPlaceable<SquareCoordinate> {
                override val position = initialPosition
            },
            board = object : GridBoardWithWalls {
                override val rows = boardSize
                override val columns = boardSize
                override val pieces = pieces.map { object : BoardPlaceable<SquareCoordinate> { override val position = it }}
                override val walls = walls.map { object : BoardPlaceable<EdgeCoordinate> { override val position = it }}
            },
            possibleSteps = directions,
            configureStepValidation = configureStepValidation
        )
    }
}