package me.leonunes.games.common.player

class GameFullException(message: String? = null, cause: Throwable? = null): Exception(message, cause)
class UserNotInGameException(message: String? = null, cause: Throwable? = null): Exception(message, cause)
