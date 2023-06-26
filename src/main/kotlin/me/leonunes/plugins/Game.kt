package me.leonunes.plugins

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.routing
import io.ktor.server.websocket.*
import io.ktor.util.reflect.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import me.leonunes.dto.*
import me.leonunes.model.*
import java.nio.charset.Charset

const val apiPathPrefix = "/rw"

fun Application.configureGame() {
    routing {
        post<CreateGameRequest> { request ->
            val game = GameFactory.createGame()
            call.respond(CreateGameResponse(game.id))
        }

        webSocket("$apiPathPrefix/game/{gameId}") {
            val gameId = call.parameters["gameId"]?.toIntOrNull()
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
                for (update in game.createUpdatesChannel()) {
                    sendSerialized(game.getStateDto(playerId))
                }
            }

            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val dto = converter?.deserialize(Charset.defaultCharset(), typeInfo<ActionDTO>(), frame) as ActionDTO
                    println("Received DTO: $dto")
                    game.processAction(dto.getAction(playerId))
                }
            }
        }
    }
}

@Serializable
@Resource("$apiPathPrefix/game/")
class CreateGameRequest()

@Serializable
class CreateGameResponse(val gameId: Int)
