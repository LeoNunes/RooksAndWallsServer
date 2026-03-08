package me.leonunes.games.dto

import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.asId
import me.leonunes.games.common.coord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActionsTest {
    @Test
    fun `AddPieceDTO toModel works properly`() {
        val dto = AddPieceDTO(coord(2, 5))
        val model = dto.toModel("player-10".asId())

        assertEquals(dto.position, model.position)
        assertEquals("player-10".asId(), model.playerId)
    }

    @Test
    fun `MoveActionDTO toModel works properly with piece movement`() {
        val pieceMovementDto = PieceMovementDTO(3, coord(1, 5))
        val wallPlacementDto = WallPlacementDTO(EdgeCoordinate(coord(5, 3), coord(5, 4)))
        val dto = MoveActionDTO(pieceMovementDto, wallPlacementDto)
        val model = dto.toModel("player-2".asId())

        assertEquals(pieceMovementDto.pieceId.asId(), model.pieceMovement!!.pieceId)
        assertEquals(pieceMovementDto.position, model.pieceMovement!!.position)
        assertEquals(wallPlacementDto.wallPosition, model.wallPlacement.wallPosition)
        assertEquals("player-2".asId(), model.playerId)
    }

    @Test
    fun `MoveActionDTO toModel works properly without piece movement`() {
        val wallPlacementDto = WallPlacementDTO(EdgeCoordinate(coord(5, 3), coord(5, 4)))
        val dto = MoveActionDTO(null, wallPlacementDto)
        val model = dto.toModel("player-2".asId())

        assertNull(model.pieceMovement)
        assertEquals(wallPlacementDto.wallPosition, model.wallPlacement.wallPosition)
        assertEquals("player-2".asId(), model.playerId)
    }
}
