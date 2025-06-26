
package com.example.domain.model

import com.example.domain.model.base.User
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserName
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class UserTest {

    @Test
    fun `create user with active status`() {
        // Given
        val userId = "user123"
        val userEmail = "user@example.com"
        val userName = "Test User"

        // When
        val user = User.create(
            id = DocumentId(userId),
            email = UserEmail(userEmail),
            name = UserName(userName),
            consentTimeStamp = Instant.now()
        )

        // Then
        assertEquals(userId, user.id.value)
        assertEquals(userEmail, user.email.value)
        assertEquals(userName, user.name.value)
        assertEquals(UserAccountStatus.ACTIVE, user.accountStatus)
    }
}
