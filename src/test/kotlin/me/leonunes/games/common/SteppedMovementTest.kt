package me.leonunes.games.common

import kotlin.test.*

class SteppedMovementTest {
    @Test
    fun `movement must be inside board`() {
        val movement = createMovement(coord(1, 2), 4).apply {
            validateInsideBoard()
        }

        assertEquals(1, movement.validations.size)
        assertNotNull(movement.validations.find { it is InsideBoardStepValidation })
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
        val movement = createMovement(coord(1, 2), 4, pieces=listOf(coord(1, 1))).apply {
            validateInsideBoard()
            validateBlockedByPieces()
        }

        assertEquals(2, movement.validations.size)
        assertNotNull(movement.validations.find { it is InsideBoardStepValidation })
        assertNotNull(movement.validations.find { it is BlockedByPiecesStepValidation })
        assertEquals(setOf(
            coord(1, 3),
            coord(2, 2),
            coord(3, 2),
            coord(0, 2)
        ), movement.getPossibleDestinations())
    }

    @Test
    fun `movement is blocked by selected pieces`() {
        val movement = createMovement(coord(1, 2), 4, pieces=listOf(coord(1, 1), coord(0, 2))).apply {
            validateInsideBoard()
            validateBlockedByPieces { piece, _ -> piece.position == coord(1, 1) }
        }

        assertEquals(2, movement.validations.size)
        assertNotNull(movement.validations.find { it is InsideBoardStepValidation })
        assertNotNull(movement.validations.find { it is BlockedByPiecesStepValidation })
        assertEquals(setOf(
            coord(1, 3),
            coord(2, 2),
            coord(3, 2),
            coord(0, 2)
        ), movement.getPossibleDestinations())
    }

    @Test
    fun `movement is limited by number of steps`() {
        val movement = createMovement(coord(1, 2), 4).apply {
            validateInsideBoard()
            validateMaxNumberOfSteps(1)
        }

        assertEquals(2, movement.validations.size)
        assertNotNull(movement.validations.find { it is InsideBoardStepValidation })
        assertNotNull(movement.validations.find { it is MaxNumberOfStepsStepValidation && it.max == 1 })
        assertEquals(setOf(
            coord(1, 3),
            coord(1, 1),
            coord(2, 2),
            coord(0, 2)
        ), movement.getPossibleDestinations())
    }

    @Test
    fun `movement is blocked by walls`() {
        val movement = createMovement(coord(1, 2), 4, walls=listOf(EdgeCoordinate(coord(1, 0), coord(1, 1)))).apply {
            validateInsideBoard()
            validateBlockedByWalls()
        }

        assertEquals(2, movement.validations.size)
        assertNotNull(movement.validations.find { it is InsideBoardStepValidation })
        assertNotNull(movement.validations.find { it is BlockedByWallsStepValidation<*, *, *> })
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
        val movement = createMovement(coord(1, 0), 8, directions = setOf(coordStep(0, 1)))
        movement.validateInsideBoard()
        movement.addStepValidation {
            if (it.stepIndex % 2 == 0) StepValidationResult.Invalid else StepValidationResult.Valid
        }

        assertEquals(setOf(
            coord(1, 2),
            coord(1, 4),
            coord(1, 6)
        ), movement.getPossibleDestinations())
    }

    interface GridBoardWithPiecesAndWalls : GridBoard, WithPieces<BoardPlaceable<SquareCoordinate>>, WithWalls<BoardPlaceable<EdgeCoordinate>>
    private fun createMovement(
        initialPosition : SquareCoordinate,
        boardSize: Int,
        pieces: List<SquareCoordinate> = emptyList(),
        walls: List<EdgeCoordinate> = emptyList(),
        directions: Set<SquareCoordinateStep> = linearMovementDirections,
    ) : SteppedMovement<SquareCoordinate, BoardPlaceable<SquareCoordinate>, GridBoardWithPiecesAndWalls> {

        return SteppedMovement(
            piece = object : BoardPlaceable<SquareCoordinate> {
                override val position = initialPosition
            },
            board = object : GridBoardWithPiecesAndWalls {
                override val rows = boardSize
                override val columns = boardSize
                override val pieces = pieces.map { object : BoardPlaceable<SquareCoordinate> { override val position = it }}
                override val walls = walls.map { object : BoardPlaceable<EdgeCoordinate> { override val position = it }}
            },
            possibleSteps = directions,
        )
    }
}
