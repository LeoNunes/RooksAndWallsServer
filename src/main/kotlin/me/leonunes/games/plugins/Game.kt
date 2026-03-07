package me.leonunes.games.plugins

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.routing
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.leonunes.games.common.asId
import me.leonunes.games.dto.ActionDTO
import me.leonunes.games.dto.getStateDto
import me.leonunes.games.AppDependencies
import me.leonunes.games.rooksandwalls.model.GameConfig
import me.leonunes.games.rooksandwalls.model.GameFactory
import me.leonunes.games.rooksandwalls.model.GameId
import java.util.UUID

const val apiPathPrefix = "/rw"

fun Application.configureGame() {
    routing {
        post<CreateGameRequest> {
            val body = runCatching { call.receive<CreateGameRequestBody>() }.getOrNull()

            val game = if (body == null) GameFactory.createGame() else
                GameFactory.createGame(GameConfig(body.numberOfPlayers, body.piecesPerPlayer, body.boardRows, body.boardColumns))

            call.respond(CreateGameResponse(game.id.get()))
        }

        webSocket("$apiPathPrefix/game/{gameId}") {
            val gameId : GameId? = call.parameters["gameId"]?.toIntOrNull()?.asId()
            //val spectate = call.parameters["spectate"].toBoolean()

            val game = gameId?.let { GameFactory.getGameById(it) }
            if (game == null) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Websocket closed due to nonexistent game"))
                return@webSocket
            }

            val token = call.parameters["token"]
            val playerId: String = if (token != null) {
                val sub = AppDependencies.jwtValidator?.validate(token)
                if (sub == null) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid token"))
                    return@webSocket
                }
                sub
            } else {
                UUID.randomUUID().toString()
            }
            val displayName: String = if (token != null) {
                AppDependencies.userRepository.getDisplayName(playerId) ?: "User"
            } else {
                "Guest"
            }
            val playerIdResult = game.joinGame(playerId, displayName)
            // TODO: Handle disconnect

            sendSerialized(game.getStateDto(playerIdResult))

            launch {
                val channel = game.createUpdatesChannel()
                try {
                    for (update in channel) {
                        sendSerialized(game.getStateDto(playerIdResult))
                    }
                }
                finally {
                    channel.cancel()
                }
            }

            launch {
                while (isActive) {
                    try {
                        val dto = receiveDeserialized<ActionDTO>()
                        game.processAction(dto.getAction(playerIdResult))
                    }
                    // TODO: Handle fails properly
                    catch (e: Exception) {
                        send("Error while execution action: ${e.javaClass.name} ${e.message}")
                    }
                }
            }.join()
        }
    }
}

@Serializable
@Resource("$apiPathPrefix/game/")
class CreateGameRequest
@Serializable
class CreateGameRequestBody(
    val numberOfPlayers: Int? = null,
    val piecesPerPlayer: Int? = null,
    val boardRows: Int? = null,
    val boardColumns: Int? = null,
    /*val users: List<String>? = null*/) // this list will be for when there is authentication and a game is created for specific users
@Serializable
class CreateGameResponse(val gameId: Int)
