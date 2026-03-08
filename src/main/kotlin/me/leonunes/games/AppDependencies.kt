package me.leonunes.games

import me.leonunes.games.auth.CognitoJwtValidator
import me.leonunes.games.users.UserRepository
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

object AppDependencies {
    val userRepository: UserRepository by lazy {
        val tableName = System.getenv("GAMES_USERS_TABLE_NAME")
            ?: error("GAMES_USERS_TABLE_NAME not set")
        val region = System.getenv("GAMES_COGNITO_REGION")
            ?: error("GAMES_COGNITO_REGION not set")
        val dynamoDb = DynamoDbClient.builder()
            .region(Region.of(region))
            .build()
        UserRepository(dynamoDb, tableName)
    }

    val cognitoJwtValidator: CognitoJwtValidator? by lazy {
        val userPoolId = System.getenv("GAMES_COGNITO_USER_POOL_ID") ?: return@lazy null
        val region = System.getenv("GAMES_COGNITO_REGION")
            ?: error("GAMES_COGNITO_REGION not set")
        CognitoJwtValidator(region, userPoolId)
    }
}
