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
        // Check global uniqueness via the displayName-index GSI before writing.
        // Note: this query-then-write has a small race-condition window; see design-fix doc.
        val existing = dynamoDb.query(QueryRequest.builder()
            .tableName(tableName)
            .indexName("displayName-index")
            .keyConditionExpression("displayName = :dn")
            .expressionAttributeValues(mapOf(":dn" to av(displayName)))
            .limit(1)
            .build())
        if (existing.count() > 0) return CreateUserResult.DisplayNameTaken

        return try {
            dynamoDb.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(mapOf(
                    "userId"      to av(userId),
                    "displayName" to av(displayName),
                    "createdAt"   to av(createdAt),
                ))
                .conditionExpression("attribute_not_exists(userId)")
                .build())
            CreateUserResult.Success
        } catch (e: ConditionalCheckFailedException) {
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
