package me.leonunes.games

import io.github.oshai.kotlinlogging.KotlinLogging
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
import me.leonunes.games.di.appModule
import me.leonunes.games.plugins.configureGame
import me.leonunes.games.plugins.configureHealthCheck
import me.leonunes.games.plugins.configureUsers
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

fun main() {
    val port = System.getenv("GAMES_PORT")?.toIntOrNull() ?: 5000
    logger.info { "Starting server on port $port" }
    embeddedServer(Netty, port = port, host = "127.0.0.1", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    installPlugins()

    configureHealthCheck()
    configureGame()
    configureUsers()
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
        allowHost("*.games.leonunes.me", schemes = listOf("https"))
        allowHeader(HttpHeaders.ContentType)
    }
}
