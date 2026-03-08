package me.leonunes.games.users

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*

sealed interface CreateUserResult {
    object Success : CreateUserResult
    object DisplayNameTaken : CreateUserResult
}

interface UserData {
    val displayName: String
}

class UserRepository(
    private val dynamoDb: DynamoDbClient,
    private val tableName: String
) {
    fun createUser(userId: String, displayName: String, createdAt: String): CreateUserResult {
        return try {
            dynamoDb.transactWriteItems(TransactWriteItemsRequest.builder()
                .transactItems(
                    // Reserve the display name atomically; fails if already taken
                    TransactWriteItem.builder()
                        .put(Put.builder()
                            .tableName(tableName)
                            .item(mapOf(
                                "userId"      to av("DISPLAYNAME#$displayName"),
                                "displayName" to av(displayName),
                            ))
                            .conditionExpression("attribute_not_exists(userId)")
                            .build())
                        .build(),
                    // Write the user record; fails if userId already exists
                    TransactWriteItem.builder()
                        .put(Put.builder()
                            .tableName(tableName)
                            .item(mapOf(
                                "userId"      to av(userId),
                                "displayName" to av(displayName),
                                "createdAt"   to av(createdAt),
                            ))
                            .conditionExpression("attribute_not_exists(userId)")
                            .build())
                        .build()
                )
                .build())
            CreateUserResult.Success
        } catch (e: TransactionCanceledException) {
            CreateUserResult.DisplayNameTaken
        }
    }

    fun getUserData(userId: String): UserData {
        val response = dynamoDb.getItem(GetItemRequest.builder()
            .tableName(tableName)
            .key(mapOf("userId" to av(userId)))
            .projectionExpression("displayName")
            .build())

        val displayName = response.item()["displayName"] ?: throw Exception("Invalid user data: Missing displayName")

        return object : UserData {
            override val displayName = displayName.s()
        }
    }

    private fun av(s: String) = AttributeValue.builder().s(s).build()
}
