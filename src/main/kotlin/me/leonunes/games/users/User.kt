package me.leonunes.games.users

sealed interface User {
    val id: String
    val displayName: String
}

interface GuestUser : User
interface RegisteredUser : User
interface AuthenticatedUser : RegisteredUser

data class GuestUserImpl(
    override val id: String,
    override val displayName: String = "Guest"
) : GuestUser

data class RegisteredUserImpl(
    override val id: String,
    override val displayName: String
) : RegisteredUser

data class AuthenticatedUserImpl(
    override val id: String,
    override val displayName: String
) : AuthenticatedUser
