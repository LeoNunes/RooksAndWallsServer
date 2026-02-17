package me.leonunes.games.dto

import kotlinx.serialization.Serializable
import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.SquareCoordinate
import me.leonunes.games.common.asId
import me.leonunes.games.rooksandwalls.model.AddPieceAction
import me.leonunes.games.rooksandwalls.model.GameAction
import me.leonunes.games.rooksandwalls.model.MoveAction
import me.leonunes.games.rooksandwalls.model.PlayerId
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
data class AddPieceDTO(val position: SquareCoordinate) : ActionDTOBase {
    override fun toModel(player: PlayerId) = AddPieceAction(player, position)
}

@Serializable
data class MoveActionDTO(val pieceId: Int, val position: SquareCoordinate, val wallPosition: EdgeCoordinate) : ActionDTOBase {
    override fun toModel(player: PlayerId) = MoveAction(player, pieceId.asId(), position, wallPosition)
}
