package me.leonunes.common

import kotlin.test.*

class IdTest {

    class Person
    class Car

    @Test
    fun `Id creation works properly`() {
        val intId = 5.asId<Person>()
        assertEquals(5, intId.get())

        val stringId = "id".asId<Person>()
        assertEquals("id", stringId.get())

        val genericId = SquareCoordinate(0, 5).asId<Person, _>()
        assertEquals(SquareCoordinate(0, 5), genericId.get())
    }

    @Test
    fun `Id comparison takes type into account`() {
        assertNotEquals<Id<*, *>>(
            5.asId<Person>(),
            5.asId<Car>()
        )
    }
}