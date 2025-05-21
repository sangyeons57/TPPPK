package com.example.data.repository

import com.example.data.datasource.remote.projectmember.ProjectMemberRemoteDataSource
import com.example.domain.model.ProjectMember
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import kotlin.Result
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class ProjectMemberRepositoryImplTest {

    @Mock
    private lateinit var remoteDataSource: ProjectMemberRemoteDataSource

    private lateinit var projectMemberRepository: ProjectMemberRepositoryImpl

    private val testProjectId = "testProject1"
    private val testUserId = "testUser1"
    private val testMember = ProjectMember(
        userId = testUserId,
        userName = "Test User",
        profileImageUrl = null,
        roleIds = listOf("role1"),
        joinedAt = System.currentTimeMillis()
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        projectMemberRepository = ProjectMemberRepositoryImpl(remoteDataSource)
    }

    @Test
    fun `getProjectMember success should return member from remote data source`() = runTest {
        // Arrange
        `when`(remoteDataSource.getProjectMember(testProjectId, testUserId))
            .thenReturn(Result.success(testMember))

        // Act
        val result = projectMemberRepository.getProjectMember(testProjectId, testUserId)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(testMember, result.getOrNull())
    }

    @Test
    fun `getProjectMember when remote data source returns null should return success with null`() = runTest {
        // Arrange
        `when`(remoteDataSource.getProjectMember(testProjectId, testUserId))
            .thenReturn(Result.success(null))

        // Act
        val result = projectMemberRepository.getProjectMember(testProjectId, testUserId)

        // Assert
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getProjectMember failure should return failure from remote data source`() = runTest {
        // Arrange
        val exception = RuntimeException("Network error")
        `when`(remoteDataSource.getProjectMember(testProjectId, testUserId))
            .thenReturn(Result.failure(exception))

        // Act
        val result = projectMemberRepository.getProjectMember(testProjectId, testUserId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
