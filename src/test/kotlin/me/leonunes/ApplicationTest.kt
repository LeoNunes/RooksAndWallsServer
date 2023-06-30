package me.leonunes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.*
import io.ktor.http.*
import me.leonunes.plugins.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            installPlugins()
            configureGame()
        }
        client.get("/").apply {
//            assertEquals(HttpStatusCode.OK, status)
//            assertEquals("Hello World!", bodyAsText())
        }
    }
}
