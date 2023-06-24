package me.leonunes.dto

import kotlinx.serialization.Serializable
import me.leonunes.common.*
import me.leonunes.model.*
import kotlin.reflect.full.memberProperties

@Serializable
data class ActionDTO(
    val placePiece: PlacePieceDTO? = null,
    val move: MoveActionDTO? = null,
) {
    fun getAction() : GameAction {
        val nonNullProperties = this::class.memberProperties
            .mapNotNull { it.getter.call(this) as? ActionDTOBase }

        if (nonNullProperties.size != 1) {
            throw Exception("There must be exactly one action")
        }

        return nonNullProperties[0].toModel()
    }
}

interface ActionDTOBase {
    fun toModel() : GameAction
}

@Serializable
data class MoveActionDTO(val pieceId: Int, val position: SquareCoordinate) : ActionDTOBase {
    override fun toModel() : MoveAction = MoveAction(pieceId.asId(), position)
}

@Serializable
data class PlacePieceDTO(val position: SquareCoordinate) : ActionDTOBase {
    override fun toModel() : PlacePieceAction = PlacePieceAction(position)
}
