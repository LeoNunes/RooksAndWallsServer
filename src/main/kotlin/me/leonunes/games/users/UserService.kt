package me.leonunes.games.users

import me.leonunes.games.users.auth.CognitoJwtValidator
import java.time.Instant
import java.util.UUID

sealed interface RegisterUserResult {
    data class Success(val user: RegisteredUser) : RegisterUserResult
    object AlreadyRegistered : RegisterUserResult
    object DisplayNameTaken : RegisterUserResult
    object InvalidDisplayName : RegisterUserResult
}

class UserService(
    private val repository: UserRepository,
    private val jwtValidator: CognitoJwtValidator?
) {
    fun getGuestUser(): GuestUser {
        return GuestUserImpl(id = UUID.randomUUID().toString())
    }

    fun getAuthenticatedUser(token: String): AuthenticatedUser {
        val sub = jwtValidator?.validate(token) ?: throw InvalidTokenException()
        val userData = repository.getUserData(sub)
        return AuthenticatedUserImpl(id = sub, displayName = userData.displayName)
    }

    fun getRegisteredUser(userId: String): RegisteredUser {
        val userData = repository.getUserData(userId)
        return RegisteredUserImpl(id = userId, displayName = userData.displayName)
    }

    fun registerUser(token: String, displayName: String): RegisterUserResult {
        val sub = jwtValidator?.validate(token) ?: throw InvalidTokenException()

        if (displayName.isBlank() || displayName.length < 4 || displayName.length > 30) {
            return RegisterUserResult.InvalidDisplayName
        }

        try {
            repository.getUserData(sub)
            return RegisterUserResult.AlreadyRegistered
        } catch (_: Exception) {
            // User not registered yet — proceed to create
        }

        return when (repository.createUser(sub, displayName, Instant.now().toString())) {
            is CreateUserResult.Success ->
                RegisterUserResult.Success(RegisteredUserImpl(id = sub, displayName = displayName))
            is CreateUserResult.DisplayNameTaken ->
                RegisterUserResult.DisplayNameTaken
        }
    }
}
