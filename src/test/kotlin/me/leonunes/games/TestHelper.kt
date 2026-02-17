package me.leonunes.games

import kotlin.test.assertTrue

fun <T> assertEach(items: Collection<T>, condition: (T) -> Boolean) {
    items.forEach {
        assertTrue(condition(it))
    }
}
