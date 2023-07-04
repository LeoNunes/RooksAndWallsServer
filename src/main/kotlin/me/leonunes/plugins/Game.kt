package me.leonunes.plugins

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.routing
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.leonunes.common.asId
import me.leonunes.dto.ActionDTO
import me.leonunes.dto.getStateDto
import me.leonunes.model.GameFactory
import me.leonunes.model.GameId

const val apiPathPrefix = "/rw"

fun Application.configureGame() {
    routing {
        post<CreateGameRequest> { request ->
            val game = GameFactory.createGame()
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

            val playerId = game.joinGame()
            // TODO: Handle disconnect

            sendSerialized(game.getStateDto(playerId))

            launch {
                val channel = game.createUpdatesChannel()
                try {
                    for (update in channel) {
                        sendSerialized(game.getStateDto(playerId))
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
                        game.processAction(dto.getAction(playerId))
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
class CreateGameResponse(val gameId: Int)
