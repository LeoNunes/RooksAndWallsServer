package me.leonunes.games.users

sealed interface User {
    val id: String
    val displayName: String
}

data class GuestUser(
    override val id: String,
    override val displayName: String = "Guest"
) : User

abstract class RegisteredUser : User

data class AuthenticatedUser(
    override val id: String,
    override val displayName: String
) : RegisteredUser()
