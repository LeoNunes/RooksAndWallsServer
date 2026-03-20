package me.leonunes.games.rooksandwalls.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.leonunes.games.common.*

typealias GameId = Id<Game, String>

interface GameObserver {
    suspend fun onGameUpdated() = Unit
}

interface Game {
    val id: GameId
    val config: GameConfig
    val gameStage: GameStage
    val currentTurn: PlayerNumber?
    val remainingPlayers: List<PlayerNumber>
    val pieces: List<Piece>
    val deadPieces: List<Piece>
    val walls: List<Wall>
    suspend fun start()
    suspend fun processAction(action: GameAction)
    fun observe(observer: GameObserver)
}

class GameImp internal constructor(override val id: GameId, override val config: GameConfig) : Game {
    override var gameStage: GameStage = GameStage.NotStarted
    override var currentTurn: PlayerNumber? = null

    private val board = Board(config.boardRows, config.boardColumns)
    override val pieces: List<Piece>
        get() = board.pieces.toList()
    override val walls: List<Wall>
        get() = board.walls.toList()
    override val deadPieces: List<Piece>
        get() = board.deadPieces.toList()

    private val piecePlacementTurnOrder =
        alternatingSequencePlayerTurnOrder(config.numberOfPlayers, config.piecesPerPlayer * config.numberOfPlayers)
    private val movesTurnOrder = if (config.piecesPerPlayer % 2 == 1) sequentialPlayerTurnOrder(config.numberOfPlayers)
        else sequentialPlayerTurnOrder(config.numberOfPlayers, startPlayer = config.numberOfPlayers - 1, reversed = true)

    override var remainingPlayers = listOf<Int>()

    private var nextPieceId = 0

    private val observers: MutableList<GameObserver> = mutableListOf()

    override fun observe(observer: GameObserver) { observers.add(observer) }

    private fun getPieceById(id: PieceId) : Piece = board.pieces.find { it.id == id } ?: throw IllegalArgumentException("Piece is dead or doesn't exist on this game")
    private fun getPieceByPosition(position: SquareCoordinate) : Piece? = board.pieces.find { it.position == position }
    private fun getWallByPosition(position: EdgeCoordinate) : Wall? = board.walls.find { it.position == position }
    private fun assertGameStage(gameStage: GameStage) = if (this.gameStage != gameStage) throw InvalidStageException() else Unit
    private fun assertPlayersTurn(playerNumber: PlayerNumber) = if (currentTurn != playerNumber) throw NotPlayersTurnException() else Unit

    override suspend fun start() {
        assertGameStage(GameStage.NotStarted)
        remainingPlayers = (0 until config.numberOfPlayers).toList()
        startPiecePlacementStage()
        notifyUpdates()
    }

    private fun startPiecePlacementStage() {
        gameStage = GameStage.PiecePlacement
        moveToNextPlayerTurn()
    }

    private fun startMovesStage() {
        gameStage = GameStage.Moves
        moveToNextPlayerTurn()
    }

    private fun completeGame() {
        gameStage = GameStage.Completed
        currentTurn = null
    }

    private fun moveToNextPlayerTurn() {
        val turnOrder = if (gameStage == GameStage.PiecePlacement) piecePlacementTurnOrder else movesTurnOrder
        var nextPlayer = turnOrder.next()
        while (nextPlayer !in remainingPlayers) {
            nextPlayer = turnOrder.next()
        }
        currentTurn = nextPlayer
    }

    private fun checkDeadPieces() {
        val deadSquares = board.sliceIntoRegions()
            .filter { it.size <= 8 }
            .reduceOrNull { acc, curr -> acc.union(curr) }
            ?: return


        for (piece in board.pieces.toList()) {
            if (piece.position in deadSquares) {
                board.pieces.remove(piece)
                board.deadPieces.add(piece)
            }
        }
    }

    private fun updateRemainingPlayers() {
        remainingPlayers = pieces.map { it.owner }.distinct()
    }

    private fun checkGameIsOver() {
        if (board.pieces.isEmpty()) {
            // Draw
            completeGame()
        }
        if (board.pieces.all { it.owner == board.pieces[0].owner }) {
            // Win
            completeGame()
        }
    }

    private fun endTurn() {
        if (gameStage == GameStage.Moves) {
            checkDeadPieces()
            updateRemainingPlayers()
            checkGameIsOver()
        }

        if (gameStage == GameStage.Completed)
            return

        if (gameStage == GameStage.PiecePlacement && !piecePlacementTurnOrder.hasNext()) {
            startMovesStage()
        }
        else {
            moveToNextPlayerTurn()
        }
    }

    private fun processAddPieceAction(action: AddPieceAction) {
        assertGameStage(GameStage.PiecePlacement)

        val playerNumber = action.playerNumber
        assertPlayersTurn(playerNumber)

        if (!board.isInsideBoard(action.position)) {
            throw InvalidActionException("Position is not inside board")
        }

        if (getPieceByPosition(action.position) != null) {
            throw InvalidActionException("Position is already occupied")
        }

        board.pieces.add(Piece(nextPieceId++.asId(), playerNumber, action.position, board))
        endTurn()
    }

    private fun processMoveAction(action: MoveAction) {
        assertGameStage(GameStage.Moves)

        val playerNumber = action.playerNumber
        assertPlayersTurn(playerNumber)

        if (getWallByPosition(action.wallPlacement.wallPosition) != null) {
            throw InvalidActionException("Wall position is already occupied")
        }
        if (!board.isInsideBoard(action.wallPlacement.wallPosition)) {
            throw InvalidActionException("Wall position is outside the board")
        }

        if (action.pieceMovement != null) {
            val piece = getPieceById(action.pieceMovement.pieceId)
            if (piece.owner != playerNumber) {
                throw InvalidActionException("Piece not owned by player")
            }
            if (!piece.movement.canMoveTo(action.pieceMovement.position)) {
                throw InvalidActionException("Piece can't move to this position")
            }
            piece.position = action.pieceMovement.position
        } else {
            val hasLegalMoves = board.pieces
                .filter { it.owner == playerNumber }
                .any { it.movement.getPossibleDestinations().isNotEmpty() }
            if (hasLegalMoves) {
                throw InvalidActionException("Player has legal moves and must move a piece")
            }
        }

        board.walls.add(Wall(action.wallPlacement.wallPosition))
        endTurn()
    }

    override suspend fun processAction(action: GameAction) {
        when (action) {
            is AddPieceAction -> processAddPieceAction(action)
            is MoveAction -> processMoveAction(action)
        }
        notifyUpdates()
    }

    private suspend fun notifyUpdates() {
        observers.forEach { it.onGameUpdated() }
    }
}

@Serializable
enum class GameStage {
    @SerialName("not_started")
    NotStarted,
    @SerialName("piece_placement")
    PiecePlacement,
    @SerialName("moves")
    Moves,
    @SerialName("completed")
    Completed;
}
