package me.leonunes.games.di

import me.leonunes.games.rooksandwalls.model.GameManagerFactory
import me.leonunes.games.users.UserRepository
import me.leonunes.games.users.UserService
import me.leonunes.games.users.auth.CognitoJwtValidator
import org.koin.dsl.module
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

val appModule = module {
    single {
        val region = System.getenv("GAMES_COGNITO_REGION") ?: error("GAMES_COGNITO_REGION not set")
        DynamoDbClient.builder().region(Region.of(region)).build()
    }

    single {
        val tableName = System.getenv("GAMES_USERS_TABLE_NAME") ?: error("GAMES_USERS_TABLE_NAME not set")
        UserRepository(get(), tableName)
    }

    single {
        val region     = System.getenv("GAMES_COGNITO_REGION")       ?: error("GAMES_COGNITO_REGION not set")
        val userPoolId = System.getenv("GAMES_COGNITO_USER_POOL_ID") ?: error("GAMES_COGNITO_USER_POOL_ID not set")
        CognitoJwtValidator(region, userPoolId)
    }

    single { UserService(get(), get()) }

    single { GameManagerFactory() }
}
