package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data.model.remote.UserDTO
import com.example.data.model.remote.toDto
import com.example.domain.model.base.User
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserEmail
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryImplTest {

    private lateinit var userRemoteDataSource: UserRemoteDataSource
    private lateinit var functionsRemoteDataSource: FunctionsRemoteDataSource
    private lateinit var repository: UserRepositoryImpl
    private lateinit var context: UserRepositoryFactoryContext

    @Before
    fun setUp() {
        userRemoteDataSource = mockk(relaxed = true)
        functionsRemoteDataSource = mockk(relaxed = true)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        context = UserRepositoryFactoryContext(CollectionPath.users)
        repository = UserRepositoryImpl(userRemoteDataSource, functionsRemoteDataSource, context)
    }

    @Test
    fun `save new user delegates to create`() = runTest {
        val user = User.create(
            id = DocumentId("") ,
            email = UserEmail("test@example.com"),
            name = UserName("John"),
            consentTimeStamp = Instant.EPOCH
        )
        val expectedId = DocumentId("123")
        coEvery { userRemoteDataSource.create(user.toDto()) } returns CustomResult.Success(expectedId)

        val result = repository.save(user)

        coVerify { userRemoteDataSource.setCollection(context.collectionPath) }
        coVerify { userRemoteDataSource.create(user.toDto()) }
        assertEquals(expectedId, (result as CustomResult.Success).data)
    }

    @Test
    fun `save existing user delegates to update`() = runTest {
        val user = User.fromDataSource(
            id = DocumentId("1"),
            email = UserEmail("test@example.com"),
            name = UserName("John"),
            consentTimeStamp = Instant.EPOCH,
            memo = null,
            userStatus = UserStatus.OFFLINE,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            fcmToken = null,
            accountStatus = UserAccountStatus.ACTIVE
        )
        user.changeName(UserName("New"))
        val changedFields = user.getChangedFields()
        coEvery { userRemoteDataSource.update(user.id, changedFields) } returns CustomResult.Success(user.id)

        val result = repository.save(user)

        coVerify { userRemoteDataSource.setCollection(context.collectionPath) }
        coVerify { userRemoteDataSource.update(user.id, changedFields) }
        assertEquals(user.id, (result as CustomResult.Success).data)
    }

    @Test
    fun `observeByName returns mapped user`() = runTest {
        val dto = UserDTO(
            id = "1",
            email = "test@example.com",
            name = "John",
            consentTimeStamp = Date.from(Instant.EPOCH),
            createdAt = Date.from(Instant.EPOCH),
            updatedAt = Date.from(Instant.EPOCH)
        )
        every { userRemoteDataSource.findByNameStream("John") } returns flowOf(CustomResult.Success(dto))

        val result = repository.observeByName(UserName("John")).first()

        coVerify { userRemoteDataSource.setCollection(context.collectionPath) }
        assertTrue(result is CustomResult.Success)
        assertEquals("1", (result as CustomResult.Success).data.id.value)
    }
}

