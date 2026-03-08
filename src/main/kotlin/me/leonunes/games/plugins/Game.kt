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
import me.leonunes.games.AppDependencies
import me.leonunes.games.common.asId
import me.leonunes.games.dto.ActionDTO
import me.leonunes.games.dto.getStateDto
import me.leonunes.games.rooksandwalls.model.GameConfig
import me.leonunes.games.rooksandwalls.model.GameId
import me.leonunes.games.users.InvalidTokenException
import me.leonunes.games.users.User

const val apiPathPrefix = "/rw"

fun Application.configureGame() {
    routing {
        post<CreateGameRequest> {
            val body = runCatching { call.receive<CreateGameRequestBody>() }.getOrNull()
            val config = body?.let {
                GameConfig(it.numberOfPlayers, it.piecesPerPlayer, it.boardRows, it.boardColumns)
            }
            val manager = AppDependencies.gameManagerFactory.createGame(config)
            call.respond(CreateGameResponse(manager.game.id.get()))
        }

        webSocket("$apiPathPrefix/game/{gameId}") {
            val user: User = resolveUser(call) ?: run {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid token"))
                return@webSocket
            }

            val gameId: GameId? = call.parameters["gameId"]?.toIntOrNull()?.asId()
            val manager = gameId?.let { AppDependencies.gameManagerFactory.getManager(it) }
            if (manager == null) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Websocket closed due to nonexistent game"))
                return@webSocket
            }

            val player = manager.joinGame(user)
            val playerId = player.id

            // TODO: In the future, make `manager.joinGame` automatically call `createUpdatesChannel` in the game
            //  and return a `GameView` object (which is a view of the game from the perspective of a player)
            sendSerialized(manager.game.getStateDto(playerId))

            launch {
                val channel = manager.game.createUpdatesChannel()
                try {
                    for (update in channel) {
                        sendSerialized(manager.game.getStateDto(playerId))
                    }
                } finally {
                    channel.cancel()
                }
            }

            try {
                launch {
                    while (isActive) {
                        try {
                            val dto = receiveDeserialized<ActionDTO>()
                            manager.game.processAction(dto.getAction(playerId))
                        } catch (e: Exception) {
                            send("Error while executing action: ${e.javaClass.name} ${e.message}")
                        }
                    }
                }.join()
            } finally {
                // TODO: Find a way to keep track of the connections so that, if player connects on a new WS,
                //  the previous one is closed and only the new one is kept open. And player status is not
                //  changed to disconnected
                manager.disconnectPlayer(playerId)
            }
        }
    }
}

private fun resolveUser(call: ApplicationCall): User? {
    val token = call.parameters["token"] ?: return AppDependencies.userService.getGuestUser()
    return try {
        AppDependencies.userService.getAuthenticatedUser(token)
    } catch (e: InvalidTokenException) {
        null
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
    val boardColumns: Int? = null
)

@Serializable
class CreateGameResponse(val gameId: Int)
