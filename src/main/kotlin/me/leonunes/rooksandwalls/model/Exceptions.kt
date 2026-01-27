package me.leonunes.rooksandwalls.model

class InvalidConfigurationException(message: String? = null, cause: Throwable? = null): Exception(message, cause)
class GameFullException(message: String? = null, cause: Throwable? = null): Exception(message, cause)
class NotPlayersTurnException(message: String? = null, cause: Throwable? = null): Exception(message, cause)
class InvalidStageException(message: String? = null, cause: Throwable? = null): Exception(message, cause)
class InvalidActionException(message: String? = null, cause: Throwable? = null): Exception(message, cause)
