package com.example.domain.model.vo.user

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UserNameTest {

    @Test
    fun `create UserName with valid name`() {
        // Given
        val validName = "John Doe"

        // When
        val userName = UserName(validName)

        // Then
        assertEquals(validName, userName.value)
    }

    @Test
    fun `create UserName from factory method`() {
        // Given
        val validName = "John Doe"

        // When
        val userName = UserName.from(validName)

        // Then
        assertEquals(validName, userName.value)
    }

    @Test
    fun `UserName equality works correctly`() {
        // Given
        val name1 = UserName("John Doe")
        val name2 = UserName("John Doe")
        val name3 = UserName("Jane Doe")

        // Then
        assertEquals(name1, name2)
        assertNotEquals(name1, name3)
        assertEquals(name1.hashCode(), name2.hashCode())
        assertNotEquals(name1.hashCode(), name3.hashCode())
    }

    @Test
    fun `create UserName with blank value throws exception`() {
        // Given
        val blankName = ""

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            UserName(blankName)
        }
    }

    @Test
    fun `create UserName with whitespace only value throws exception`() {
        // Given
        val whitespaceName = "   "

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            UserName(whitespaceName)
        }
    }

    @Test
    fun `UserName toString returns value`() {
        // Given
        val validName = "John Doe"
        val userName = UserName(validName)

        // When
        val result = userName.toString()

        // Then
        assertEquals(validName, result)
    }

    @Test
    fun `UserName with various valid formats`() {
        // Test various valid name formats
        val validNames = listOf(
            "John",
            "John Doe",
            "John-Paul Smith",
            "Mary Jane Watson",
            "O'Connor",
            "李明",
            "José María",
            "Anna-Marie",
            "J",
            "Jean-Baptiste Emmanuel Zorg",
            "Dr. John Smith Jr.",
            "Smith, John",
            "John123", // Numbers allowed
            "User Name With Spaces"
        )

        validNames.forEach { nameValue ->
            // Should not throw exception
            val userName = UserName(nameValue)
            assertEquals(nameValue, userName.value)
        }
    }

    @Test
    fun `UserName with edge case valid values`() {
        // Test edge cases that should be valid
        val edgeCaseNames = listOf(
            "A", // Single character
            "a", // Lowercase single character
            "1", // Single digit
            "User.Name", // With period
            "User_Name", // With underscore
            "User Name!", // With exclamation
            "User@Name", // With at symbol
            "User#Name", // With hash
            "$pecial", // Starting with special character
            "Name with multiple    spaces", // Multiple spaces
            "\tTabbed\tName", // With tabs
            "Name\nWith\nNewlines" // With newlines (if allowed)
        )

        edgeCaseNames.forEach { nameValue ->
            // Should not throw exception for any non-blank string
            val userName = UserName(nameValue)
            assertEquals(nameValue, userName.value)
        }
    }

    @Test
    fun `UserName comparison with different types`() {
        // Given
        val userName = UserName("John Doe")

        // Then
        assertNotEquals(userName, "John Doe")
        assertNotEquals(userName, null)
    }

    @Test
    fun `UserName factory method with blank value throws exception`() {
        // Given
        val blankName = ""

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            UserName.from(blankName)
        }
    }

    @Test
    fun `UserName case sensitivity`() {
        // Given
        val lowerName = UserName("john doe")
        val upperName = UserName("JOHN DOE")
        val mixedName = UserName("John Doe")

        // Then - names are case-sensitive
        assertNotEquals(lowerName, upperName)
        assertNotEquals(lowerName, mixedName)
        assertNotEquals(upperName, mixedName)
    }

    @Test
    fun `UserName with whitespace preservation`() {
        // Given
        val nameWithSpaces = "  John  Doe  "
        
        // When
        val userName = UserName(nameWithSpaces)
        
        // Then - whitespace is preserved (no trimming)
        assertEquals(nameWithSpaces, userName.value)
    }

    @Test
    fun `UserName with null value throws exception`() {
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            UserName(null as String?)
        }
    }
}