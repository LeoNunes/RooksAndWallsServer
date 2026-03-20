package me.leonunes.games.rooksandwalls.ai

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.leonunes.games.rooksandwalls.model.GameStage
import me.leonunes.games.rooksandwalls.model.GameView

class AiPlayerRunner(
    private val gameView: GameView,
    private val strategy: AiStrategy
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            val channel = gameView.updatesChannel
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

    private val playerNumber get() = gameView.player.playerNumber

    private suspend fun maybeAct() {
        if (gameView.gameStage == GameStage.Completed) return
        if (gameView.gameStage == GameStage.NotStarted) return
        if (gameView.currentTurn != playerNumber) return
        val action = withContext(Dispatchers.Default) { strategy.chooseAction(gameView.game, playerNumber) }
        gameView.processAction(action)
    }
}
