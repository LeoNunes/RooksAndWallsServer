package me.leonunes.games.plugins

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import me.leonunes.games.AppDependencies
import me.leonunes.games.users.CreateUserResult
import me.leonunes.games.users.InvalidTokenException
import java.time.Instant

fun Application.configureUsers() {
    routing {
        post<CreateUserRequest> {
            val authenticatedUser = try {
                val token = extractBearerToken(call) ?: run {
                    call.respond(HttpStatusCode.Unauthorized); return@post
                }
                AppDependencies.userService.getAuthenticatedUser(token)
            } catch (e: InvalidTokenException) {
                call.respond(HttpStatusCode.Unauthorized); return@post
            }

            val body = call.receive<CreateUserRequestBody>()
            if (body.displayName.isBlank() || body.displayName.length > 30) {
                call.respond(HttpStatusCode.BadRequest, "Invalid display name")
                return@post
            }

            when (AppDependencies.userRepository.createUser(
                authenticatedUser.id, body.displayName, Instant.now().toString()
            )) {
                is CreateUserResult.Success ->
                    call.respond(HttpStatusCode.Created, UserProfileResponse(authenticatedUser.id, body.displayName))
                is CreateUserResult.DisplayNameTaken ->
                    call.respond(HttpStatusCode.Conflict, "Display name taken")
            }
        }

        get<GetUserMeRequest> {
            val authenticatedUser = try {
                val token = extractBearerToken(call) ?: run {
                    call.respond(HttpStatusCode.Unauthorized); return@get
                }
                AppDependencies.userService.getAuthenticatedUser(token)
            } catch (e: InvalidTokenException) {
                call.respond(HttpStatusCode.Unauthorized); return@get
            }

            val userData = try {
                AppDependencies.userRepository.getUserData(authenticatedUser.id)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.NotFound); return@get
            }

            call.respond(UserProfileResponse(authenticatedUser.id, userData.displayName))
        }
    }
}

private fun extractBearerToken(call: ApplicationCall): String? {
    val authHeader = call.request.header("Authorization") ?: return null
    if (!authHeader.startsWith("Bearer ")) return null
    return authHeader.removePrefix("Bearer ")
}

@Serializable @Resource("/rw/users") class CreateUserRequest
@Serializable @Resource("/rw/users/me") class GetUserMeRequest
@Serializable data class CreateUserRequestBody(val displayName: String)
@Serializable data class UserProfileResponse(val userId: String, val displayName: String)
