package me.leonunes.dto

import kotlinx.serialization.Serializable
import me.leonunes.common.*
import me.leonunes.model.*
import kotlin.reflect.full.memberProperties

@Serializable
data class ActionDTO(
    val addPiece: AddPieceDTO? = null,
    val move: MoveActionDTO? = null,
) {
    fun getAction(player: PlayerId) : GameAction {
        val nonNullProperties = this::class.memberProperties
            .mapNotNull { it.getter.call(this) as? ActionDTOBase }

        if (nonNullProperties.size != 1) {
            throw Exception("There must be exactly one action")
        }

        return nonNullProperties[0].toModel(player)
    }
}

interface ActionDTOBase {
    fun toModel(player: PlayerId) : GameAction
}

@Serializable
data class MoveActionDTO(val pieceId: Int, val position: SquareCoordinate) : ActionDTOBase {
    override fun toModel(player: PlayerId) = MoveAction(player, pieceId.asId(), position)
}

@Serializable
data class AddPieceDTO(val position: SquareCoordinate) : ActionDTOBase {
    override fun toModel(player: PlayerId) = AddPieceAction(player, position)
}
