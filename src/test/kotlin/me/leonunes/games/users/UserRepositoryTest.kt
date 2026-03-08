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
        every { dynamoDb.transactWriteItems(any<TransactWriteItemsRequest>()) } returns
            TransactWriteItemsResponse.builder().build()

        val result = repo.createUser("user-123", "Alice", "2026-01-01T00:00:00Z")

        assertTrue(result is CreateUserResult.Success)
        verify { dynamoDb.transactWriteItems(match<TransactWriteItemsRequest> { req ->
            val items = req.transactItems()
            items.size == 2 &&
            items[0].put().item()["userId"]?.s() == "DISPLAYNAME#Alice" &&
            items[1].put().item()["userId"]?.s() == "user-123" &&
            items[1].put().item()["displayName"]?.s() == "Alice"
        }) }
    }

    @Test
    fun `createUser returns DisplayNameTaken when transaction is cancelled`() {
        every { dynamoDb.transactWriteItems(any<TransactWriteItemsRequest>()) } throws
            TransactionCanceledException.builder()
                .cancellationReasons(
                    CancellationReason.builder().code("ConditionalCheckFailed").build()
                )
                .build()

        val result = repo.createUser("user-456", "Alice", "2026-01-01T00:00:00Z")

        assertTrue(result is CreateUserResult.DisplayNameTaken)
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
