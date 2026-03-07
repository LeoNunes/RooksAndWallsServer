package me.leonunes.games.auth

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.RSAKeyProvider
import java.net.URL
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

class JwtValidator(region: String, userPoolId: String) {
    private val issuer = "https://cognito-idp.$region.amazonaws.com/$userPoolId"
    private val jwkProvider = JwkProviderBuilder(URL("$issuer/.well-known/jwks.json"))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    /** Returns the Cognito sub (user ID) if the token is valid, null otherwise. */
    fun validate(token: String): String? {
        return try {
            val decoded = JWT.decode(token)
            val jwk = jwkProvider.get(decoded.keyId)
            val keyProvider = object : RSAKeyProvider {
                override fun getPublicKeyById(keyId: String): RSAPublicKey = jwk.publicKey as RSAPublicKey
                override fun getPrivateKey(): RSAPrivateKey? = null
                override fun getPrivateKeyId(): String? = null
            }
            val verifier = JWT.require(Algorithm.RSA256(keyProvider))
                .withIssuer(issuer)
                .withClaim("token_use", "id")
                .build()
            verifier.verify(token).subject
        } catch (e: Exception) {
            null
        }
    }
}
