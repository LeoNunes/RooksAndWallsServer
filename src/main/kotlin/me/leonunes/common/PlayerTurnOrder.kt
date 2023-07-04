package me.leonunes.common

fun sequentialPlayerTurnOrder(numberOfPlayers: Int, totalNumberOfTurns: Int? = null, startPlayer: Int = 0) : Iterator<Int> = sequence {
    if (startPlayer >= numberOfPlayers) {
        throw IllegalArgumentException("startPlayer must be lesser than numberOfPlayers")
    }

    var currentTurn = 0
    while (totalNumberOfTurns == null || currentTurn < totalNumberOfTurns) {
        yield((currentTurn + startPlayer) % numberOfPlayers)
        currentTurn++
    }
}.iterator()

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
