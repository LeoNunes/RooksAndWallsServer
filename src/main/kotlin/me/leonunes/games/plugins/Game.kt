package me.leonunes.games.plugins

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
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
import me.leonunes.games.rooksandwalls.model.GameAlreadyStartedException
import me.leonunes.games.rooksandwalls.model.GameConfig
import me.leonunes.games.rooksandwalls.model.GameFullException
import me.leonunes.games.rooksandwalls.model.GameId
import me.leonunes.games.users.InvalidTokenException
import me.leonunes.games.users.User

private val logger = KotlinLogging.logger {}

const val apiPathPrefix = "/rw"

fun Application.configureGame() {
    routing {
        post<CreateGameRequest> {
            val body = runCatching { call.receive<CreateGameRequestBody>() }.getOrNull()
            val config = body?.let {
                GameConfig(it.numberOfPlayers, it.piecesPerPlayer, it.boardRows, it.boardColumns)
            }
            val manager = AppDependencies.gameManagerFactory.createGame(config)
            logger.info { "game created: ${manager.game.id}" }
            call.respond(CreateGameResponse(manager.game.id.get()))
        }

        post<AddAiPlayerRequest> { request ->
            val gameId: GameId = request.gameId.asId()
            val manager = AppDependencies.gameManagerFactory.getManager(gameId)
            if (manager == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }
            try {
                val gameView = manager.addAiPlayer(AppDependencies.coroutineScope)
                logger.info { "Bot added successfully to game $gameId" }
                call.respond(AddAiResponse(playerId = gameView.player.id.get(), displayName = gameView.player.displayName))
            } catch (e: GameFullException) {
                call.respond(HttpStatusCode.Conflict, "Game is full")
            } catch (e: GameAlreadyStartedException) {
                call.respond(HttpStatusCode.Conflict, "Game has already started")
            }
        }

        webSocket("$apiPathPrefix/game/{gameId}") {
            val gameId: GameId? = call.parameters["gameId"]?.asId()

            logger.info { "Attempting to connect to $gameId" }

            val user: User = resolveUser(call) ?: run {
                logger.info { "Invalid token" }
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid token"))
                return@webSocket
            }

            val manager = gameId?.let { AppDependencies.gameManagerFactory.getManager(it) }
            if (manager == null) {
                logger.info { "Game not found" }
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Websocket closed due to nonexistent game"))
                return@webSocket
            }

            // TODO: Handle exceptions
            val gameView = manager.joinGame(user)

            // TODO: In the future, make `manager.joinGame` automatically call `createUpdatesChannel` in the game
            //  and return a `GameView` object (which is a view of the game from the perspective of a player)
            sendSerialized(gameView.getStateDto())

            launch {
                val channel = gameView.updatesChannel
                try {
                    for (update in channel) {
                        sendSerialized(gameView.getStateDto())
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
                            // TODO: Process action through GameView: gameView.processAction.
                            manager.game.processAction(dto.getAction(gameView.player.id))
                        } catch (e: Exception) {
                            send("Error while executing action: ${e.javaClass.name} ${e.message}")
                        }
                    }
                }.join()
            } finally {
                // TODO: Find a way to keep track of the connections so that, if player connects on a new WS,
                //  the previous one is closed and only the new one is kept open. And player status is not
                //  changed to disconnected
                manager.disconnectPlayer(gameView.player.id)
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
class CreateGameResponse(val gameId: String)

@Serializable
@Resource("$apiPathPrefix/game/{gameId}/ai")
class AddAiPlayerRequest(val gameId: String)

@Serializable
class AddAiResponse(val playerId: String, val displayName: String)
