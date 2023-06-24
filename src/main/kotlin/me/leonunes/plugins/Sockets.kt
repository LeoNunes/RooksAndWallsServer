package me.leonunes.plugins

import io.ktor.serialization.kotlinx.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import kotlinx.serialization.json.Json
import me.leonunes.common.SquareCoordinate
import me.leonunes.dto.*
import me.leonunes.model.Game
import java.nio.charset.Charset

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    routing {
        webSocket("/rw") {
            val game = Game()

            sendSerialized(game.getStateDto())

            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val dto = converter?.deserialize(Charset.defaultCharset(), typeInfo<ActionDTO>(), frame) as ActionDTO
                    println("Received DTO: $dto")
                    dto.getAction().process(game)
                }
            }
        }

        webSocket("/echo") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    outgoing.send(Frame.Text("YOU SAID: $text"))
                    if (text.equals("bye", ignoreCase = true)) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                    }
                }
            }
        }
    }
}
