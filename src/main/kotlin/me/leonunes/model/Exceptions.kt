package me.leonunes.model

class NotPlayersTurnException(message: String? = null, cause: Throwable? = null): Exception(message, cause)
class InvalidStageException(message: String? = null, cause: Throwable? = null): Exception(message, cause)
class InvalidActionException(message: String? = null, cause: Throwable? = null): Exception(message, cause)
