package me.leonunes

import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import me.leonunes.plugins.configureGame
import me.leonunes.plugins.configureHealthCheck
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toIntOrNull() ?: 5000, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    installPlugins()

    configureHealthCheck()
    configureGame()
}

fun Application.installPlugins() {
    install(ContentNegotiation) {
        json()
    }
    install(Resources)
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    install(IgnoreTrailingSlash)
    install(CORS) {
        if (this@installPlugins.developmentMode) {
            allowHost("localhost:5173", schemes = listOf("http"))
            allowHost("127.0.0.1:5173", schemes = listOf("http"))
        }
        allowHost("leonunes.me", subDomains = listOf("rw"), schemes = listOf("https"))
        allowHeader(HttpHeaders.ContentType)
    }
}
