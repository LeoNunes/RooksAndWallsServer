package me.leonunes.games.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.leonunes.games.AppDependencies
import me.leonunes.games.auth.CognitoJwtValidator
import me.leonunes.games.users.CreateUserResult
import java.time.Instant

fun Application.configureUsers() {
    routing {
        post("/rw/users") {
            val userId = extractUserIdFromBearer(call, AppDependencies.cognitoJwtValidator)
                ?: run { call.respond(HttpStatusCode.Unauthorized); return@post }

            val body = call.receive<CreateUserRequest>()
            if (body.displayName.isBlank() || body.displayName.length > 30) {
                call.respond(HttpStatusCode.BadRequest, "Invalid display name")
                return@post
            }

            when (AppDependencies.userRepository.createUser(userId, body.displayName, Instant.now().toString())) {
                is CreateUserResult.Success -> call.respond(HttpStatusCode.Created)
                is CreateUserResult.DisplayNameTaken -> call.respond(HttpStatusCode.Conflict, "Display name taken")
            }
        }

        get("/rw/users/me") {
            val userId = extractUserIdFromBearer(call, AppDependencies.cognitoJwtValidator)
                ?: run { call.respond(HttpStatusCode.Unauthorized); return@get }

            val userData = try {
                AppDependencies.userRepository.getUserData(userId)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.NotFound); return@get
            }

            call.respond(UserProfileResponse(userId, userData.displayName))
        }
    }
}

private suspend fun extractUserIdFromBearer(call: ApplicationCall, validator: CognitoJwtValidator?): String? {
    val authHeader = call.request.header("Authorization") ?: return null
    if (!authHeader.startsWith("Bearer ")) return null
    val token = authHeader.removePrefix("Bearer ")
    return validator?.validate(token)
}

@Serializable data class CreateUserRequest(val displayName: String)
@Serializable data class UserProfileResponse(val userId: String, val displayName: String)
