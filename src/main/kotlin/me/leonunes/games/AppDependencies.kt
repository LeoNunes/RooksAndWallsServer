package me.leonunes.games

import me.leonunes.games.auth.JwtValidator
import me.leonunes.games.users.UserRepository
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

object AppDependencies {
    val userRepository: UserRepository by lazy {
        val tableName = System.getenv("GAMES_USERS_TABLE_NAME")
            ?: error("GAMES_USERS_TABLE_NAME not set")
        val region = System.getenv("GAMES_COGNITO_REGION") ?: "us-west-2"
        val dynamoDb = DynamoDbClient.builder()
            .region(Region.of(region))
            .build()
        UserRepository(dynamoDb, tableName)
    }

    val jwtValidator: JwtValidator? by lazy {
        val userPoolId = System.getenv("GAMES_COGNITO_USER_POOL_ID") ?: return@lazy null
        val region = System.getenv("GAMES_COGNITO_REGION") ?: "us-west-2"
        JwtValidator(region, userPoolId)
    }
}
