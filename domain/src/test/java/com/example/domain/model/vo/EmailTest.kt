package com.example.domain.model.vo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EmailTest {

    @Test
    fun `create Email with valid email address`() {
        // Given
        val validEmail = "user@example.com"

        // When
        val email = Email(validEmail)

        // Then
        assertEquals(validEmail, email.value)
    }

    @Test
    fun `create Email from factory method`() {
        // Given
        val validEmail = "user@example.com"

        // When
        val email = Email.from(validEmail)

        // Then
        assertEquals(validEmail, email.value)
    }

    @Test
    fun `Email equality works correctly`() {
        // Given
        val email1 = Email("user@example.com")
        val email2 = Email("user@example.com")
        val email3 = Email("other@example.com")

        // Then
        assertEquals(email1, email2)
        assertNotEquals(email1, email3)
        assertEquals(email1.hashCode(), email2.hashCode())
        assertNotEquals(email1.hashCode(), email3.hashCode())
    }

    @Test
    fun `create Email with blank value throws exception`() {
        // Given
        val blankEmail = ""

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            Email(blankEmail)
        }
    }

    @Test
    fun `create Email with whitespace only value throws exception`() {
        // Given
        val whitespaceEmail = "   "

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            Email(whitespaceEmail)
        }
    }

    @Test
    fun `Email toString returns value`() {
        // Given
        val validEmail = "user@example.com"
        val email = Email(validEmail)

        // When
        val result = email.toString()

        // Then
        assertEquals(validEmail, result)
    }

    @Test
    fun `Email with various valid formats`() {
        // Test various valid email formats
        val validEmails = listOf(
            "user@example.com",
            "user.name@example.com",
            "user+tag@example.com",
            "user_name@example.com",
            "user-name@example.com",
            "123@example.com",
            "user@example-domain.com",
            "user@subdomain.example.com",
            "a@b.co",
            "very.long.email.address@very.long.domain.name.com"
        )

        validEmails.forEach { emailValue ->
            // Should not throw exception
            val email = Email(emailValue)
            assertEquals(emailValue, email.value)
        }
    }

    @Test
    fun `Email with invalid format should not throw exception in constructor`() {
        // Note: This test documents current behavior - Email VO only checks for blank,
        // not email format validation. Format validation should be done at use case level.
        val invalidEmails = listOf(
            "plaintext",
            "@example.com",
            "user@",
            "user@@example.com",
            "user@example.",
            "user name@example.com" // Space in local part
        )

        invalidEmails.forEach { emailValue ->
            // Current implementation only checks for blank, not format
            val email = Email(emailValue)
            assertEquals(emailValue, email.value)
        }
    }

    @Test
    fun `Email comparison with different types`() {
        // Given
        val email = Email("user@example.com")

        // Then
        assertNotEquals(email, "user@example.com")
        assertNotEquals(email, null)
    }

    @Test
    fun `Email factory method with blank value throws exception`() {
        // Given
        val blankEmail = ""

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            Email.from(blankEmail)
        }
    }

    @Test
    fun `Email case sensitivity`() {
        // Given
        val lowerEmail = Email("user@example.com")
        val upperEmail = Email("USER@EXAMPLE.COM")
        val mixedEmail = Email("User@Example.Com")

        // Then - emails are case-sensitive in this implementation
        assertNotEquals(lowerEmail, upperEmail)
        assertNotEquals(lowerEmail, mixedEmail)
        assertNotEquals(upperEmail, mixedEmail)
    }
}