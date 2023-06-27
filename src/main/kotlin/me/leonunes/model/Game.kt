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

typealias GameId = Id<Game, Int>
interface Game {
    val id: GameId
    val gameStage: GameStage
    val currentTurn: Player?
    val players : List<Player>
    val pieces : List<Piece>
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
    private val _pieces = mutableListOf<Piece>()
    override val pieces: List<Piece>
        get() = _pieces.toList()

    private val piecePlacementTurnOrder = alternatingSequencePlayerTurnOrder(numberOfPlayers, piecesPerPlayer * numberOfPlayers)
    private val movesTurnOrder = sequentialPlayerTurnOrder(numberOfPlayers)

    private var nextPlayerId = 0
    private var nextPieceId = 0

    private val updateChannels: MutableList<SendChannel<GameUpdate>> = mutableListOf()
    private val gameMutex: Mutex = Mutex()

    fun getPlayerById(id: PlayerId) : Player? = players.find { it.id == id }

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

    fun addPiece(player: Player, position: SquareCoordinate) {
        if (player != currentTurn) {
            throw NotPlayersTurnException()
        }
        if (gameStage != GameStage.PiecePlacement) {
            throw InvalidActionForCurrentStage()
        }

        _pieces.add(Piece(nextPieceId++.asId(), player, position))
        endTurn()
    }

    override suspend fun processAction(action: GameAction) {
        gameMutex.withLock {
            action.process(this)
            notifyUpdates() // Can this be outside the lock?
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
    @SerialName("piece-placement")
    PiecePlacement,
    @SerialName("moves")
    Moves,
    @SerialName("completed")
    Completed;
}

typealias GameFactory = GameImp.Factory

typealias PieceId = Id<Piece, Int>
data class Piece(val id: PieceId, val owner: Player, val position: SquareCoordinate)

typealias PlayerId = Id<Player, Int>
data class Player(val id: PlayerId)

class NotPlayersTurnException : Exception()
class InvalidActionForCurrentStage : Exception()