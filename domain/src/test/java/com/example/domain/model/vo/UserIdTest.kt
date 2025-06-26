package com.example.domain.model.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UserIdTest {

    @Test
    fun `create UserId with valid value`() {
        // Given
        val validId = "user123"

        // When
        val userId = UserId(validId)

        // Then
        assertEquals(validId, userId.value)
    }

    @Test
    fun `create UserId from factory method`() {
        // Given
        val validId = "user123"

        // When
        val userId = UserId.from(validId)

        // Then
        assertEquals(validId, userId.value)
    }

    @Test
    fun `UserId equality works correctly`() {
        // Given
        val id1 = UserId("user123")
        val id2 = UserId("user123")
        val id3 = UserId("user456")

        // Then
        assertEquals(id1, id2)
        assertNotEquals(id1, id3)
        assertEquals(id1.hashCode(), id2.hashCode())
        assertNotEquals(id1.hashCode(), id3.hashCode())
    }

    @Test
    fun `create UserId with blank value throws exception`() {
        // Given
        val blankId = ""

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            UserId(blankId)
        }
    }

    @Test
    fun `create UserId with whitespace only value throws exception`() {
        // Given
        val whitespaceId = "   "

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            UserId(whitespaceId)
        }
    }

    @Test
    fun `UserId toString returns value`() {
        // Given
        val validId = "user123"
        val userId = UserId(validId)

        // When
        val result = userId.toString()

        // Then
        assertEquals(validId, result)
    }

    @Test
    fun `UserId with valid edge case values`() {
        // Test various valid ID formats
        val validIds = listOf(
            "1",
            "a",
            "user-123",
            "user_123",
            "user.123",
            "123456789012345678901234567890", // Long ID
            "FirebaseUserId_ABC123",
            "email@domain.com" // Email-like format
        )

        validIds.forEach { id ->
            // Should not throw exception
            val userId = UserId(id)
            assertEquals(id, userId.value)
        }
    }

    @Test
    fun `UserId comparison with different types`() {
        // Given
        val userId = UserId("user123")
        val documentId = DocumentId("user123")

        // Then
        assertNotEquals(userId, documentId)
        assertNotEquals(userId, "user123")
        assertNotEquals(userId, null)
    }

    @Test
    fun `UserId factory method with blank value throws exception`() {
        // Given
        val blankId = ""

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            UserId.from(blankId)
        }
    }
}