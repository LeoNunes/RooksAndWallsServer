package me.leonunes.games.rooksandwalls.model

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.leonunes.games.common.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class GameUpdate

typealias GameId = Id<Game, Int>
interface Game {
    val id: GameId
    val config: GameConfig
    val gameStage: GameStage
    val currentTurn: Player?
    val players: List<Player>
    val remainingPlayers: List<Player>
    val pieces: List<Piece>
    val deadPieces: List<Piece>
    val walls: List<Wall>
    suspend fun start(players: List<Player>)
    suspend fun processAction(action: GameAction)
    fun createUpdatesChannel(): ReceiveChannel<GameUpdate>
}

class GameImp private constructor(override val id: GameId, override val config: GameConfig) : Game {
    override var gameStage: GameStage = GameStage.WaitingForPlayers
    override var currentTurn: Player? = null

    private val _players = mutableListOf<Player>()
    override val players: List<Player>
        get() = _players.toList()

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

    override var remainingPlayers = listOf<Player>()

    private var nextPieceId = 0

    private val updateChannels: MutableList<SendChannel<GameUpdate>> = mutableListOf()
    private val gameMutex: Mutex = Mutex()

    private fun getPlayerById(id: PlayerId) : Player = players.find { it.id == id } ?: throw IllegalArgumentException("Player doesn't exist on this game")
    private fun getPieceById(id: PieceId) : Piece = board.pieces.find { it.id == id } ?: throw IllegalArgumentException("Piece is dead or doesn't exist on this game")
    private fun getPieceByPosition(position: SquareCoordinate) : Piece? = board.pieces.find { it.position == position }
    private fun getWallByPosition(position: EdgeCoordinate) : Wall? = board.walls.find { it.position == position }
    private fun assertGameStage(gameStage: GameStage) = if (this.gameStage != gameStage) throw InvalidStageException() else Unit
    private fun assertPlayersTurn(player: Player) = if (currentTurn != player) throw NotPlayersTurnException() else Unit

    override suspend fun start(players: List<Player>) {
        gameMutex.withLock {
            _players.addAll(players)
            remainingPlayers = _players.toList()
            startPiecePlacementStage()
            notifyUpdates()
        }
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
        var nextPlayer = players[turnOrder.next()]
        while (nextPlayer !in remainingPlayers) {
            nextPlayer = players[turnOrder.next()]
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
        if (board.pieces.size == 0) {
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

        val player = getPlayerById(action.playerId)
        assertPlayersTurn(player)

        if (!board.isInsideBoard(action.position)) {
            throw InvalidActionException("Position is not inside board")
        }

        if (getPieceByPosition(action.position) != null) {
            throw InvalidActionException("Position is already occupied")
        }

        board.pieces.add(Piece(nextPieceId++.asId(), player, action.position, board))
        endTurn()
    }

    private fun processMoveAction(action: MoveAction) {
        assertGameStage(GameStage.Moves)

        val player = getPlayerById(action.playerId)
        assertPlayersTurn(player)

        if (getWallByPosition(action.wallPlacement.wallPosition) != null) {
            throw InvalidActionException("Wall position is already occupied")
        }
        if (!board.isInsideBoard(action.wallPlacement.wallPosition)) {
            throw InvalidActionException("Wall position is outside the board")
        }

        if (action.pieceMovement != null) {
            val piece = getPieceById(action.pieceMovement.pieceId)
            if (piece.owner != player) {
                throw InvalidActionException("Piece not owned by player")
            }
            if (!piece.movement.canMoveTo(action.pieceMovement.position)) {
                throw InvalidActionException("Piece can't move to this position")
            }
            piece.position = action.pieceMovement.position
        } else {
            val hasLegalMoves = board.pieces
                .filter { it.owner == player }
                .any { it.movement.getPossibleDestinations().isNotEmpty() }
            if (hasLegalMoves) {
                throw InvalidActionException("Player has legal moves and must move a piece")
            }
        }

        board.walls.add(Wall(action.wallPlacement.wallPosition))
        endTurn()
    }

    override suspend fun processAction(action: GameAction) {
        gameMutex.withLock {
            when (action) {
                is AddPieceAction -> processAddPieceAction(action)
                is MoveAction -> processMoveAction(action)
            }
            notifyUpdates()
        }
    }

    override fun createUpdatesChannel() : ReceiveChannel<GameUpdate> {
        val channel = Channel<GameUpdate>(CONFLATED)
        updateChannels.add(channel)
        return channel
    }

    private suspend fun notifyUpdates() {
        updateChannels.forEach { it.send(GameUpdate()) }
        if (gameStage == GameStage.Completed) {
            updateChannels.forEach { it.close() }
        }
    }

    object Factory {
        private var nextId = AtomicInteger(0)
        private val games = ConcurrentHashMap<GameId, Game>()
        fun getGameById(id: GameId) : Game? = games[id]

        fun createGame(config: GameConfig) : Game {
            val id: GameId = nextId.getAndIncrement().asId()
            return GameImp(id, config).also { games[id] = it }
        }

        fun createGame() : Game {
            return createGame(GameConfigDefaultValues)
        }
    }
}

@Serializable
enum class GameStage {
    // TODO: Rename to "not_started"
    @SerialName("waiting_for_players")
    WaitingForPlayers,
    @SerialName("piece_placement")
    PiecePlacement,
    @SerialName("moves")
    Moves,
    @SerialName("completed")
    Completed;
}

typealias GameFactory = GameImp.Factory
