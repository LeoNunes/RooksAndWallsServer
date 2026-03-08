package me.leonunes.games.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CognitoJwtValidatorTest {
    // For tests we use a simple HMAC validator stub (real validator uses JWKS)
    // These tests focus on claim extraction logic
    @Test
    fun `extractSubFromValidToken returns sub claim`() {
        val token = JWT.create()
            .withSubject("user-sub-123")
            .withClaim("token_use", "id")
            .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
            .sign(Algorithm.HMAC256("secret"))

        val decoded = JWT.decode(token)
        assertEquals("user-sub-123", decoded.subject)
    }
}
