package me.leonunes.rooksandwalls.model

val GameConfigDefaultValues = GameConfig(3, 3, 8, 8)

class GameConfig(numberOfPlayers: Int? = null, piecesPerPlayer: Int? = null, boardRows: Int? = null, boardColumns: Int? = null) {
    val numberOfPlayers: Int
    val piecesPerPlayer: Int
    val boardRows: Int
    val boardColumns: Int

    init {
        this.numberOfPlayers = numberOfPlayers ?: GameConfigDefaultValues.numberOfPlayers
        this.piecesPerPlayer = piecesPerPlayer ?: GameConfigDefaultValues.piecesPerPlayer
        this.boardRows = boardRows ?: GameConfigDefaultValues.boardRows
        this.boardColumns = boardColumns ?: GameConfigDefaultValues.boardColumns
    }

    init {
        if (this.numberOfPlayers < 2) throw InvalidConfigurationException("Must have at least 2 players")
        if (this.piecesPerPlayer < 1) throw InvalidConfigurationException("Must have at least 1 piece per player")
        if (this.boardRows < 1 || this.boardColumns < 1) throw InvalidConfigurationException("Board must have a positive size")
    }

    override fun equals(other: Any?): Boolean {
        if (other is GameConfig) {
            return numberOfPlayers == other.numberOfPlayers &&
                   piecesPerPlayer == other.piecesPerPlayer &&
                   boardRows == other.boardRows &&
                   boardColumns == other.boardColumns
        }
        return false
    }

    override fun hashCode(): Int {
        var result = numberOfPlayers
        result = 31 * result + piecesPerPlayer
        result = 31 * result + boardRows
        result = 31 * result + boardColumns
        return result
    }
}
