package me.leonunes.games.users

class InvalidTokenException : Exception("Invalid or missing authentication token")
class UserNotFoundException(userId: String) : Exception("User not found: $userId")
