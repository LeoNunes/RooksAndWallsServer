package me.leonunes.games.rooksandwalls.ai

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import me.leonunes.games.rooksandwalls.model.*
import me.leonunes.games.users.AiUser
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AiPlayerRunnerTest {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    @AfterTest
    fun tearDown() { scope.cancel() }

    @Test
    fun `two ai players drive game to completion`() = runBlocking {
        val config = GameConfig(numberOfPlayers = 2, piecesPerPlayer = 1, boardRows = 4, boardColumns = 4)
        val game = GameFactory.createGame(config)
        val strategy = RandomAiStrategy()

        val player1 = Player(AiUser("ai-0-0"))
        val player2 = Player(AiUser("ai-0-1"))
        game.start(listOf(player1, player2))

        AiPlayerRunner(game, player1.id, strategy).start(scope)
        AiPlayerRunner(game, player2.id, strategy).start(scope)

        val channel = game.createUpdatesChannel()
        for (update in channel) { /* drain until channel closes */ }

        assertEquals(GameStage.Completed, game.gameStage)
    }
}
