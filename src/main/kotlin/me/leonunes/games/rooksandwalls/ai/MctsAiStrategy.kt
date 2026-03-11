package me.leonunes.games.rooksandwalls.ai

import kotlinx.serialization.Serializable
import me.leonunes.games.rooksandwalls.model.*
import kotlin.math.ln
import kotlin.math.sqrt

private const val MAX_CHILDREN = 30
private const val SQRT_2 = 1.4142135623730951  // UCB1 exploration constant C = sqrt(2)

class MctsNode(
    val state: SimGame,
    val move: SimMove?,
    val parent: MctsNode?
) {
    val children = mutableListOf<MctsNode>()
    var visits = 0
    val wins = IntArray(state.playerCount)

    val isFullyExpanded: Boolean
        get() = state.isTerminal() || children.size >= MAX_CHILDREN

    fun addChild(move: SimMove): MctsNode {
        val newState = state.clone()
        newState.applyMove(move)
        return MctsNode(newState, move, this).also { children.add(it) }
    }

    /**
     * Selects the best child from the perspective of [state.currentPlayerIndex] (the player
     * who will choose among these children). Unvisited children always get priority.
     */
    fun bestChild(): MctsNode {
        val parentPlayerIndex = state.currentPlayerIndex
        val logParentVisits = ln(visits.toDouble())
        return children.maxBy { child ->
            if (child.visits == 0) Double.MAX_VALUE
            else {
                val exploitation = child.wins[parentPlayerIndex].toDouble() / child.visits
                val exploration = SQRT_2 * sqrt(logParentVisits / child.visits)
                exploitation + exploration
            }
        }
    }
}

data class MctsConfig(
    val simulationCount: Int,
    val timeoutMs: Long?,
    val playoutPolicy: PlayoutPolicy
) {
    companion object {
        val EASY    = MctsConfig(50,             null,   RandomPlayoutPolicy())
        val MEDIUM  = MctsConfig(500,            null,   RandomPlayoutPolicy())
        val HARD    = MctsConfig(3000,           null,   HeuristicPlayoutPolicy())
        val MAXIMUM = MctsConfig(Int.MAX_VALUE,  20000L,  HeuristicPlayoutPolicy())
    }
}

@Serializable
enum class AiDifficulty {
    RANDOM, EASY, MEDIUM, HARD, MAXIMUM;

    fun toAiStrategy(): AiStrategy = when (this) {
        RANDOM  -> RandomAiStrategy()
        EASY    -> MctsAiStrategy(MctsConfig.EASY)
        MEDIUM  -> MctsAiStrategy(MctsConfig.MEDIUM)
        HARD    -> MctsAiStrategy(MctsConfig.HARD)
        MAXIMUM -> MctsAiStrategy(MctsConfig.MAXIMUM)
    }
}

class MctsAiStrategy(private val config: MctsConfig) : AiStrategy {
    private val placement = RandomAiStrategy()

    override fun chooseAction(game: Game, playerId: PlayerId): GameAction =
        when (game.gameStage) {
            GameStage.PiecePlacement -> placement.chooseAction(game, playerId)
            GameStage.Moves -> chooseMoveAction(game, playerId)
            else -> throw IllegalStateException("AI asked to act in stage ${game.gameStage}")
        }

    private fun chooseMoveAction(game: Game, playerId: PlayerId): MoveAction {
        val simGame = SimGame.from(game)
        val root = MctsNode(simGame, move = null, parent = null)

        val startTime = System.currentTimeMillis()
        var simCount = 0

        while (simCount < config.simulationCount &&
            (config.timeoutMs == null || System.currentTimeMillis() - startTime < config.timeoutMs)) {
            runSimulation(root, config.playoutPolicy)
            simCount++
        }

        val bestMove = root.children.maxBy { it.visits }.move!!
        return bestMove.toMoveAction(game, playerId)
    }

    private fun runSimulation(root: MctsNode, policy: PlayoutPolicy) {
        // Phase 1: Selection
        var node = root
        while (node.isFullyExpanded && !node.state.isTerminal()) {
            node = node.bestChild()
        }

        // Phase 2: Expansion
        // Note: we add ONE policy-sampled child per expansion visit rather than enumerating
        // all legal moves. The branching factor (piece destinations × wall positions) can be
        // thousands, making full enumeration impractical. Each visit that reaches an
        // unexpanded node adds one new child until MAX_CHILDREN is reached. This is a
        // standard MCTS adaptation for games with large action spaces.
        if (!node.state.isTerminal()) {
            val move = policy.sampleMove(node.state)
            node = node.addChild(move)
        }

        // Phase 3: Simulation (playout)
        val simState = node.state.clone()
        while (!simState.isTerminal()) {
            simState.applyMove(policy.sampleMove(simState))
        }

        // Phase 4: Backpropagation
        val winner = simState.winner()
        var current: MctsNode? = node
        while (current != null) {
            current.visits++
            if (winner != null) current.wins[winner]++
            current = current.parent
        }
    }
}

// SimMove.pieceIndex is an index into the root SimGame.pieces list, which is built from
// game.pieces in order (no eliminations applied). The root SimGame.state is never mutated
// during MCTS (only clones are), so this index remains aligned with game.pieces at call time.
// Only use this function with moves produced from the root node's children.
private fun SimMove.toMoveAction(game: Game, playerId: PlayerId): MoveAction {
    val pieceMovement = if (pieceIndex != null && destination != null) {
        PieceMovement(game.pieces[pieceIndex].id, destination)
    } else null
    return MoveAction(playerId, pieceMovement, WallPlacement(wallPosition))
}
