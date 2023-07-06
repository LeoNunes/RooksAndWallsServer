package me.leonunes.common

/**
 * Reversed is used to get players in descending order (for example: 0, 3, 2, 1, 0, 3, 2, 1)
 */
fun sequentialPlayerTurnOrder(numberOfPlayers: Int, totalNumberOfTurns: Int? = null, startPlayer: Int = 0, reversed: Boolean = false) : Iterator<Int> = sequence {
    if (startPlayer >= numberOfPlayers) {
        throw IllegalArgumentException("startPlayer must be lesser than numberOfPlayers")
    }

    var currentTurn = 0
    while (totalNumberOfTurns == null || currentTurn < totalNumberOfTurns) {
        yield((currentTurn) % numberOfPlayers)
        currentTurn++
    }
}
    .map { if (reversed) numberOfPlayers - it else it } // Reverses the sequence. 0, 1, 2, 3, 0 becomes 4 (0 mod4), 3, 2, 1, 4
    .map { (it + startPlayer) % numberOfPlayers } // Sets the initial player for the sequence
    .iterator()

fun alternatingSequencePlayerTurnOrder(numberOfPlayers: Int, totalNumberOfTurns: Int? = null, startPlayer: Int = 0) : Iterator<Int> = sequence {
    if (startPlayer >= numberOfPlayers) {
        throw IllegalArgumentException("startPlayer must be lesser than numberOfPlayers")
    }

    var currentTurn = 0
    while (totalNumberOfTurns == null || currentTurn < totalNumberOfTurns) {
        val positionInRepeatingPattern = currentTurn % (2 * numberOfPlayers)
        if (positionInRepeatingPattern < numberOfPlayers) {
            yield(positionInRepeatingPattern)
        }
        else {
            yield(2 * numberOfPlayers - positionInRepeatingPattern - 1)
        }

        currentTurn++
    }
}.map { (it + startPlayer) % numberOfPlayers }.iterator()
