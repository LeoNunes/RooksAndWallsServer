package me.leonunes.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureHealthCheck() {
    routing {
        get("ping") {
            call.respond(HttpStatusCode.OK)
        }
    }
}
