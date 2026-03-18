package me.leonunes.games.rooksandwalls.model

import me.leonunes.games.common.BoardPlaceable
import me.leonunes.games.common.EdgeCoordinate

data class Wall(override val position: EdgeCoordinate) : BoardPlaceable<EdgeCoordinate>
