package me.leonunes.games.rooksandwalls.model

/**
 * Represent the view of the game from the perspective of a Player
 */
class GameView(private val gameManager: GameManager, val player: Player) {
    val id: GameId
        get() = game().id
    val config: GameConfig
        get() = game().config
    val gameStage: GameStage
        get() = game().gameStage
    val currentTurn: Player?
        get() = game().currentTurn
    val players: List<Player>
        get() = gameManager.players
    val remainingPlayers: List<Player>
        get() = game().remainingPlayers
    val pieces: List<Piece>
        get() = game().pieces
    val deadPieces: List<Piece>
        get() = game().deadPieces
    val walls: List<Wall>
        get() = game().walls

    val updatesChannel = game().createUpdatesChannel()

    private fun game(): Game = gameManager.game
}
