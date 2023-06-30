package me.leonunes.model

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import me.leonunes.common.*

class GameUpdate

typealias PlayerId = Id<Player, Int>
data class Player(val id: PlayerId)

typealias GameId = Id<Game, Int>
interface Game {
    val id: GameId
    val gameStage: GameStage
    val currentTurn: Player?
    val players : List<Player>
    val pieces : List<Piece>
    val walls : List<Wall>
    suspend fun joinGame() : PlayerId
    suspend fun processAction(action: GameAction)
    fun createUpdatesChannel() : ReceiveChannel<GameUpdate>
}

class GameImp private constructor(override val id: GameId) : Game {
    override var gameStage: GameStage = GameStage.WaitingForPlayers
    override var currentTurn: Player? = null

    private val _players = mutableListOf<Player>()
    override val players: List<Player>
        get() = _players.toList()

    private val board = Board(boardRows, boardColumns)
    override val pieces: List<Piece>
        get() = board.pieces.toList()
    override val walls: List<Wall>
        get() = board.walls.toList()

    private val piecePlacementTurnOrder = alternatingSequencePlayerTurnOrder(numberOfPlayers, piecesPerPlayer * numberOfPlayers)
    private val movesTurnOrder = sequentialPlayerTurnOrder(numberOfPlayers)

    private var nextPlayerId = 0
    private var nextPieceId = 0

    private val updateChannels: MutableList<SendChannel<GameUpdate>> = mutableListOf()
    private val gameMutex: Mutex = Mutex()

    private fun getPlayerById(id: PlayerId) : Player = players.find { it.id == id } ?: throw IllegalArgumentException("Player doesn't exist on this game")
    private fun getPieceById(id: PieceId) : Piece = board.pieces.find { it.id == id } ?: throw IllegalArgumentException("Piece doesn't exist on this game")
    private fun getPieceByPosition(position: SquareCoordinate) : Piece? = board.pieces.find { it.position == position }
    private fun getWallByPosition(position: EdgeCoordinate) : Wall? = board.walls.find { it.position == position }
    private fun assertGameStage(gameStage: GameStage) = if (this.gameStage != gameStage) throw InvalidStageException() else Unit
    private fun assertPlayersTurn(player: Player) = if (currentTurn != player) throw NotPlayersTurnException() else Unit

    override suspend fun joinGame(): PlayerId {
        gameMutex.withLock {
            val player = Player(nextPlayerId++.asId())
            _players.add(player)
            if (players.size == numberOfPlayers) {
                startGame()
            }
            notifyUpdates()
            return player.id
        }
    }

    private fun startGame() {
        startPiecePlacementStage()
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
        currentTurn = players[turnOrder.next()]
    }

    private fun endTurn() {
        // Check if a Piece died and update accordingly
        // Check if game is over

        if (gameStage == GameStage.PiecePlacement && !piecePlacementTurnOrder.hasNext()) {
            startMovesStage()
        } else {
            moveToNextPlayerTurn()
        }
    }

    fun addPiece(playerId: PlayerId, position: SquareCoordinate) {
        assertGameStage(GameStage.PiecePlacement)

        val player = getPlayerById(playerId)
        assertPlayersTurn(player)

        if (getPieceByPosition(position) != null) {
            throw InvalidActionException("Position is already occupied")
        }

        board.pieces.add(Piece(nextPieceId++.asId(), player, position, board))
        endTurn()
    }

    fun move(playerId: PlayerId, pieceId: PieceId, pieceDestination: SquareCoordinate, wallPosition: EdgeCoordinate) {
        assertGameStage(GameStage.Moves)

        val player = getPlayerById(playerId)
        assertPlayersTurn(player)

        val piece = getPieceById(pieceId)
        if (piece.owner != player) {
            throw InvalidActionException("Piece not owned by player")
        }

        if (getWallByPosition(wallPosition) != null) {
            throw InvalidActionException("Wall position is already occupied")
        }

        if (!piece.movement.canMoveTo(pieceDestination)) {
            throw InvalidActionException("Piece can't move to this position")
        }

        if (!board.isInsideBoard(wallPosition)) {
            throw InvalidActionException("Wall position is outside the board")
        }

        piece.position = pieceDestination
        board.walls.add(Wall(wallPosition))
        endTurn()
    }

    override suspend fun processAction(action: GameAction) {
        gameMutex.withLock {
            action.process(this)
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
    }

    private companion object Config {
        const val numberOfPlayers = 2
        const val piecesPerPlayer = 3
        const val boardRows = 8
        const val boardColumns = 8
    }

    object Factory {
        private var nextId = AtomicInteger(0)
        private val games = ConcurrentHashMap<GameId, Game>()
        fun getGameById(id: GameId) : Game? = games[id]

        fun createGame() : Game {
            val id: GameId = nextId.getAndIncrement().asId()
            return GameImp(id).also { games[id] = it }
        }
    }
}

@Serializable
enum class GameStage {
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
