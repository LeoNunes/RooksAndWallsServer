package me.leonunes.games.users

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.leonunes.games.users.auth.CognitoJwtValidator
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class UserServiceTest {
    private val mockValidator = mockk<CognitoJwtValidator>()
    private val mockRepository = mockk<UserRepository>()
    private val service = UserService(mockRepository, mockValidator)

    @Test
    fun `getGuestUser returns GuestUser with unique UUID and default display name`() {
        val user1 = service.getGuestUser()
        val user2 = service.getGuestUser()

        assertIs<GuestUser>(user1)
        assertEquals("Guest", user1.displayName)
        assertNotEquals(user1.id, user2.id)
    }

    @Test
    fun `getAuthenticatedUser returns AuthenticatedUser for valid token`() {
        every { mockValidator.validate("valid-token") } returns "cognito-sub-123"
        every { mockRepository.getUserData("cognito-sub-123") } returns object : UserData {
            override val displayName = "Alice"
        }

        val user = service.getAuthenticatedUser("valid-token")

        assertIs<AuthenticatedUser>(user)
        assertEquals("cognito-sub-123", user.id)
        assertEquals("Alice", user.displayName)
    }

    @Test
    fun `getAuthenticatedUser is an instance of RegisteredUser`() {
        every { mockValidator.validate("valid-token") } returns "cognito-sub-123"
        every { mockRepository.getUserData("cognito-sub-123") } returns object : UserData {
            override val displayName = "Alice"
        }

        val user = service.getAuthenticatedUser("valid-token")

        assertIs<RegisteredUser>(user)
    }

    @Test
    fun `getAuthenticatedUser throws InvalidTokenException for invalid token`() {
        every { mockValidator.validate("bad-token") } returns null

        assertFailsWith<InvalidTokenException> {
            service.getAuthenticatedUser("bad-token")
        }
    }

    @Test
    fun `getAuthenticatedUser throws InvalidTokenException when validator is absent`() {
        val serviceWithoutValidator = UserService(mockRepository, null)

        assertFailsWith<InvalidTokenException> {
            serviceWithoutValidator.getAuthenticatedUser("any-token")
        }
    }

    @Test
    fun `getRegisteredUser returns RegisteredUser from repository`() {
        every { mockRepository.getUserData("user-456") } returns object : UserData {
            override val displayName = "Bob"
        }

        val user = service.getRegisteredUser("user-456")

        assertIs<RegisteredUser>(user)
        assertEquals("user-456", user.id)
        assertEquals("Bob", user.displayName)
    }

    @Test
    fun `getRegisteredUser calls repository with correct userId`() {
        every { mockRepository.getUserData("user-789") } returns object : UserData {
            override val displayName = "Carol"
        }

        service.getRegisteredUser("user-789")

        verify { mockRepository.getUserData("user-789") }
    }

    @Test
    fun `registerUser throws InvalidTokenException for invalid token`() {
        every { mockValidator.validate("bad-token") } returns null

        assertFailsWith<InvalidTokenException> {
            service.registerUser("bad-token", "Alice")
        }
    }

    @Test
    fun `registerUser returns InvalidDisplayName for blank display name`() {
        every { mockValidator.validate("valid-token") } returns "sub-123"

        val result = service.registerUser("valid-token", "   ")

        assertIs<RegisterUserResult.InvalidDisplayName>(result)
    }

    @Test
    fun `registerUser returns InvalidDisplayName for display name shorter than 4 characters`() {
        every { mockValidator.validate("valid-token") } returns "sub-123"

        val result = service.registerUser("valid-token", "Al")

        assertIs<RegisterUserResult.InvalidDisplayName>(result)
    }

    @Test
    fun `registerUser returns InvalidDisplayName for display name longer than 30 characters`() {
        every { mockValidator.validate("valid-token") } returns "sub-123"

        val result = service.registerUser("valid-token", "A".repeat(31))

        assertIs<RegisterUserResult.InvalidDisplayName>(result)
    }

    @Test
    fun `registerUser returns AlreadyRegistered if user exists in repository`() {
        every { mockValidator.validate("valid-token") } returns "sub-123"
        every { mockRepository.getUserData("sub-123") } returns object : UserData {
            override val displayName = "Alice"
        }

        val result = service.registerUser("valid-token", "Alice")

        assertIs<RegisterUserResult.AlreadyRegistered>(result)
    }

    @Test
    fun `registerUser returns Success on new registration`() {
        every { mockValidator.validate("valid-token") } returns "sub-123"
        every { mockRepository.getUserData("sub-123") } throws Exception("not found")
        every { mockRepository.createUser("sub-123", "Alice", any()) } returns CreateUserResult.Success

        val result = service.registerUser("valid-token", "Alice")

        assertIs<RegisterUserResult.Success>(result)
        assertEquals("sub-123", result.user.id)
        assertEquals("Alice", result.user.displayName)
    }

    @Test
    fun `registerUser returns DisplayNameTaken when display name is not available`() {
        every { mockValidator.validate("valid-token") } returns "sub-123"
        every { mockRepository.getUserData("sub-123") } throws Exception("not found")
        every { mockRepository.createUser("sub-123", "Alice", any()) } returns CreateUserResult.DisplayNameTaken

        val result = service.registerUser("valid-token", "Alice")

        assertIs<RegisterUserResult.DisplayNameTaken>(result)
    }
}
