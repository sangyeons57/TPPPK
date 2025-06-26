package com.example.domain.usecase.auth.account

import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.repository.FakeAuthRepository
import com.example.domain.repository.FakeUserRepository
import com.example.domain.util.TestDataBuilder
import com.example.domain.util.shouldBeActive
import com.example.domain.util.shouldBeWithdrawn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ReactivateAccountUseCaseTest {

    private lateinit var useCase: ReactivateAccountUseCase
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeUserRepository: FakeUserRepository

    @Before
    fun setUp() {
        fakeAuthRepository = FakeAuthRepository()
        fakeUserRepository = FakeUserRepository()
        useCase = ReactivateAccountUseCase(fakeAuthRepository, fakeUserRepository)
    }

    @Test
    fun `reactivate withdrawn account success`() = runTest {
        // Given – a withdrawn user present in repository
        val withdrawnUser = TestDataBuilder.createTestUser(
            id = "withdrawn_user_id",
            email = "withdrawn@example.com",
            name = "Old Name",
            accountStatus = UserAccountStatus.WITHDRAWN
        )
        withdrawnUser.shouldBeWithdrawn()
        fakeUserRepository.addUser(withdrawnUser)

        // When – we invoke reactivation
        val newNickname = "ReactivatedUser"
        val result = useCase(
            email = withdrawnUser.email.value,
            nickname = newNickname,
            consentTimeStamp = Instant.now()
        )

        // Then – result success and user status ACTIVE with updated name
        assertTrue(result is CustomResult.Success)
        val reactivatedUser = (result as CustomResult.Success).data
        reactivatedUser.shouldBeActive()
        assertTrue("Nickname should be updated", reactivatedUser.name.value == newNickname)
    }
} 