package me.leonunes.games.rooksandwalls.ai

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.leonunes.games.rooksandwalls.model.Game
import me.leonunes.games.rooksandwalls.model.GameStage
import me.leonunes.games.rooksandwalls.model.PlayerId

class AiPlayerRunner(
    private val game: Game,
    private val playerId: PlayerId,
    private val strategy: AiStrategy
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            val channel = game.createUpdatesChannel()
            try {
                // Act immediately in case it's already the AI's turn when the runner starts
                maybeAct()
                for (update in channel) {
                    maybeAct()
                }
            } finally {
                channel.cancel()
            }
        }
    }

    private suspend fun maybeAct() {
        if (game.gameStage == GameStage.Completed) return
        if (game.gameStage == GameStage.WaitingForPlayers) return
        if (game.currentTurn?.id != playerId) return
        val action = withContext(Dispatchers.Default) { strategy.chooseAction(game, playerId) }
        game.processAction(action)
    }
}
