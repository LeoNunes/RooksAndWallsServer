package me.leonunes.games.rooksandwalls.dto

import kotlinx.serialization.Serializable
import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.SquareCoordinate
import me.leonunes.games.common.asId
import me.leonunes.games.rooksandwalls.model.AddPieceAction
import me.leonunes.games.rooksandwalls.model.GameAction
import me.leonunes.games.rooksandwalls.model.MoveAction
import me.leonunes.games.rooksandwalls.model.PieceMovement
import me.leonunes.games.common.player.PlayerNumber
import me.leonunes.games.rooksandwalls.model.WallPlacement
import kotlin.reflect.full.memberProperties

@Serializable
data class ActionDTO(
    val addPiece: AddPieceDTO? = null,
    val move: MoveActionDTO? = null,
) {
    fun getAction(playerNumber: PlayerNumber) : GameAction {
        val nonNullProperties = this::class.memberProperties
            .mapNotNull { it.getter.call(this) as? ActionDTOBase }

        if (nonNullProperties.size != 1) {
            throw Exception("There must be exactly one action")
        }

        return nonNullProperties[0].toModel(playerNumber)
    }
}

interface ActionDTOBase {
    fun toModel(playerNumber: PlayerNumber) : GameAction
}

@Serializable
data class AddPieceDTO(val position: SquareCoordinate) : ActionDTOBase {
    override fun toModel(playerNumber: PlayerNumber) = AddPieceAction(playerNumber, position)
}

@Serializable
data class PieceMovementDTO(val pieceId: Int, val position: SquareCoordinate)

@Serializable
data class WallPlacementDTO(val wallPosition: EdgeCoordinate)

@Serializable
data class MoveActionDTO(
    val pieceMovement: PieceMovementDTO?,
    val wallPlacement: WallPlacementDTO
) : ActionDTOBase {
    override fun toModel(playerNumber: PlayerNumber) = MoveAction(
        playerNumber,
        pieceMovement?.let { PieceMovement(it.pieceId.asId(), it.position) },
        WallPlacement(wallPlacement.wallPosition)
    )
}
