package me.leonunes.games.users

data class AiUser(override val id: String, override val displayName: String = "AI") : User
