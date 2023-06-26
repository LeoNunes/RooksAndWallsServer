package me.leonunes.model

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.leonunes.common.SquareCoordinate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class GameUpdate

interface Game {
    val id: Int
    suspend fun joinGame() : Id<Player>
    fun getPlayers() : List<Player>
    fun getPieces() : List<Piece>
    suspend fun processAction(action: GameAction)
    fun createUpdatesChannel() : ReceiveChannel<GameUpdate>
}

class GameImp private constructor(override val id: Int) : Game {
    private val players = mutableListOf<Player>()
    private val pieces = mutableListOf<Piece>()
    private val updateChannels: MutableList<SendChannel<GameUpdate>> = mutableListOf()
    private val gameMutex: Mutex = Mutex()

    private var nextPlayerId = 0
    override suspend fun joinGame(): Id<Player> {
        gameMutex.withLock {
            val player = Player(nextPlayerId.asId())
            nextPlayerId++
            players.add(player)
            notifyUpdates()
            return player.id
        }
    }
    override fun getPlayers() : List<Player> = players.toList()
    fun getPlayerById(id: Id<Player>) : Player? = players.find { it.id.isSame(id) }
    override fun getPieces() : List<Piece> = pieces.toList()

    fun addPiece(player: Player, position: SquareCoordinate) {
        pieces.add(Piece(player, position))
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
        private val games = ConcurrentHashMap<Int, Game>()
        fun getGameById(id: Int) : Game? = games[id]

        fun createGame() : Game {
            val id = nextId.getAndIncrement()
            return GameImp(id).also { games[id] = it }
        }
    }
}

typealias GameFactory = GameImp.Factory

data class Piece(val owner: Player, val position: SquareCoordinate)

data class Player(val id: Id<Player>)

abstract class Id<T> {
    abstract fun get(): Int

    fun isSame(other: Id<T>) : Boolean {
        return this.get() == other.get()
    }
}
class IdImp<T>(private val value: Int) : Id<T>() {
    override fun get(): Int {
        return value
    }
}
fun <T> Int.asId() : Id<T> {
    return IdImp(this)
}
