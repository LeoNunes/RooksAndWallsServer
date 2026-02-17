package me.leonunes.games.dto

import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.asId
import me.leonunes.games.common.coord
import kotlin.test.Test
import kotlin.test.assertEquals

class ActionsTest {
    @Test
    fun `AddPieceDTO toModel works properly`() {
        val dto = AddPieceDTO(coord(2, 5))
        val model = dto.toModel(10.asId())

        assertEquals(dto.position, model.position)
        assertEquals(10.asId(), model.playerId)
    }

    @Test
    fun `MoveActionDTO toModel works properly`() {
        val dto = MoveActionDTO(3, coord(1, 5), EdgeCoordinate(coord(5, 3), coord(5, 4)))
        val model = dto.toModel(2.asId())

        assertEquals(dto.pieceId.asId(), model.pieceId)
        assertEquals(dto.position, model.piecePosition)
        assertEquals(dto.wallPosition, model.wallPosition)
    }
}
