package me.leonunes.games.users

import io.mockk.*
import org.junit.Test
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserRepositoryTest {
    private val dynamoDb = mockk<DynamoDbClient>()
    private val tableName = "test-users-table"
    private val repo = UserRepository(dynamoDb, tableName)

    @Test
    fun `createUser returns success when displayName is unique`() {
        every { dynamoDb.query(any<QueryRequest>()) } returns QueryResponse.builder()
            .items(emptyList())
            .count(0)
            .build()
        every { dynamoDb.putItem(any<PutItemRequest>()) } returns PutItemResponse.builder().build()

        val result = repo.createUser("user-123", "Alice", "2026-01-01T00:00:00Z")

        assertTrue(result is CreateUserResult.Success)
        verify { dynamoDb.putItem(match<PutItemRequest> {
            it.item()["userId"]?.s() == "user-123" &&
            it.item()["displayName"]?.s() == "Alice"
        }) }
    }

    @Test
    fun `createUser returns DisplayNameTaken when displayName is already taken`() {
        every { dynamoDb.query(any<QueryRequest>()) } returns QueryResponse.builder()
            .items(listOf(mapOf("displayName" to AttributeValue.builder().s("Alice").build())))
            .count(1)
            .build()

        val result = repo.createUser("user-456", "Alice", "2026-01-01T00:00:00Z")

        assertTrue(result is CreateUserResult.DisplayNameTaken)
        verify(exactly = 0) { dynamoDb.putItem(any<PutItemRequest>()) }
    }

    @Test
    fun `getUserData returns userData for existing user`() {
        every { dynamoDb.getItem(any<GetItemRequest>()) } returns GetItemResponse.builder()
            .item(mapOf(
                "userId" to AttributeValue.builder().s("user-123").build(),
                "displayName" to AttributeValue.builder().s("Alice").build()
            )).build()

        assertEquals("Alice", repo.getUserData("user-123").displayName)
    }

    @Test
    fun `getUserData throws for non-existent user`() {
        every { dynamoDb.getItem(any<GetItemRequest>()) } returns GetItemResponse.builder()
            .item(emptyMap()).build()

        assertFailsWith<Exception> { repo.getUserData("user-404") }
    }
}
