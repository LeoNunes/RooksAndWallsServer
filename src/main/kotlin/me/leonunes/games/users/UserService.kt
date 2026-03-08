package me.leonunes.games.users

import me.leonunes.games.users.auth.CognitoJwtValidator
import java.util.UUID

class UserService(
    private val repository: UserRepository,
    private val jwtValidator: CognitoJwtValidator?
) {
    fun getGuestUser(): GuestUser {
        return GuestUser(id = UUID.randomUUID().toString())
    }

    fun getAuthenticatedUser(token: String): AuthenticatedUser {
        val sub = jwtValidator?.validate(token) ?: throw InvalidTokenException()
        val userData = repository.getUserData(sub)
        return AuthenticatedUser(id = sub, displayName = userData.displayName)
    }

    fun getRegisteredUser(userId: String): RegisteredUser {
        val userData = repository.getUserData(userId)
        return AuthenticatedUser(id = userId, displayName = userData.displayName)
    }
}
