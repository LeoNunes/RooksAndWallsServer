package me.leonunes.model

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.leonunes.common.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class GameUpdate

typealias GameId = Id<Game, Int>
interface Game {
    val id: GameId
    suspend fun joinGame() : PlayerId
    fun getPlayers() : List<Player>
    fun getPieces() : List<Piece>
    suspend fun processAction(action: GameAction)
    fun createUpdatesChannel() : ReceiveChannel<GameUpdate>
}

class GameImp private constructor(override val id: GameId) : Game {
    private val players = mutableListOf<Player>()
    private val pieces = mutableListOf<Piece>()

    private var nextPlayerId = 0
    private var nextPieceId = 0

    private val updateChannels: MutableList<SendChannel<GameUpdate>> = mutableListOf()
    private val gameMutex: Mutex = Mutex()

    override suspend fun joinGame(): PlayerId {
        gameMutex.withLock {
            val player = Player(nextPlayerId.asId())
            nextPlayerId++
            players.add(player)
            notifyUpdates()
            return player.id
        }
    }
    override fun getPlayers() : List<Player> = players.toList()
    fun getPlayerById(id: PlayerId) : Player? = players.find { it.id == id }
    override fun getPieces() : List<Piece> = pieces.toList()

    fun addPiece(player: Player, position: SquareCoordinate) {
        val id = nextPieceId++
        pieces.add(Piece(id.asId(), player, position))
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

    companion object Factory {
        private var nextId = AtomicInteger(0)
        private val games = ConcurrentHashMap<GameId, Game>()
        fun getGameById(id: GameId) : Game? = games[id]

        fun createGame() : Game {
            val id: GameId = nextId.getAndIncrement().asId()
            return GameImp(id).also { games[id] = it }
        }
    }
}

typealias GameFactory = GameImp.Factory

typealias PieceId = Id<Piece, Int>
data class Piece(val id: PieceId, val owner: Player, val position: SquareCoordinate)

typealias PlayerId = Id<Player, Int>
data class Player(val id: PlayerId)
