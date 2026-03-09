package me.leonunes.games.dto

import kotlinx.serialization.Serializable
import me.leonunes.games.common.EdgeCoordinate
import me.leonunes.games.common.SquareCoordinate
import me.leonunes.games.rooksandwalls.model.*

@Serializable
data class GameStateDTO(
    val gameId: Int,
    val config: GameConfigDTO,
    val stage: GameStage,
    val currentTurn: String?,
    val playerId: String,
    val players: List<PlayerDTO>,
    val remainingPlayers: List<PlayerDTO>,
    val pieces: List<PieceDTO>,
    val walls: List<WallDTO>,
    val deadPieces: List<PieceDTO>
)

@Serializable
data class GameConfigDTO(val numberOfPlayers: Int, val piecesPerPlayer: Int, val boardRows: Int, val boardColumns: Int)
fun GameConfig.toDto() = GameConfigDTO(numberOfPlayers, piecesPerPlayer, boardRows, boardColumns)

@Serializable
data class PlayerDTO(val id: String, val displayName: String, val connectionStatus: ConnectionStatus)
fun Player.toDto(): PlayerDTO = PlayerDTO(id.get(), displayName, connectionStatus)

@Serializable
data class PieceDTO(val id: Int, val owner: String, val position: SquareCoordinate)
fun Piece.toDto(): PieceDTO = PieceDTO(id.get(), owner.id.get(), position)

@Serializable
data class WallDTO(val position: EdgeCoordinate)
fun Wall.toDto(): WallDTO = WallDTO(position)

fun GameView.getStateDto(): GameStateDTO {
    return GameStateDTO(
        gameId = this.id.get(),
        config = config.toDto(),
        stage = gameStage,
        currentTurn = currentTurn?.id?.get(),
        playerId = player.id.get(),
        players = players.map { it.toDto() },
        remainingPlayers = remainingPlayers.map { it.toDto() },
        pieces = pieces.map { it.toDto() },
        walls = walls.map { it.toDto() },
        deadPieces = deadPieces.map { it.toDto() },
    )
}
