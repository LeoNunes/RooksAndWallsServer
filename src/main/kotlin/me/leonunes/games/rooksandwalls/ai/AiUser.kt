package me.leonunes.games.rooksandwalls.ai

import me.leonunes.games.users.GuestUser

class EasyBotUser : GuestUser {
    override val id = "easy_bot_user"
    override val displayName = "Easy Bot"
}

class MediumBotUser : GuestUser {
    override val id = "medium_bot_user"
    override val displayName = "Medium Bot"
}

class HardBotUser : GuestUser {
    override val id = "hard_bot_user"
    override val displayName = "Hard Bot"
}

class MaxBotUser : GuestUser {
    override val id = "max_bot_user"
    override val displayName = "Max Bot"
}

fun AiDifficulty.getUser() = when (this) {
    AiDifficulty.MEDIUM -> MediumBotUser()
    AiDifficulty.EASY -> EasyBotUser()
    AiDifficulty.HARD -> HardBotUser()
    AiDifficulty.MAXIMUM -> MaxBotUser()
}
