package com.example.domain.usecase.project.member

import com.example.domain.model.ProjectMember
import com.example.domain.repository.ProjectMemberRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import kotlin.Result
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class GetProjectMemberUseCaseImplTest {

    @Mock
    private lateinit var projectMemberRepository: ProjectMemberRepository

    private lateinit var getProjectMemberUseCase: GetProjectMemberUseCaseImpl

    private val testProjectId = "project1"
    private val testUserId = "user1"
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
        getProjectMemberUseCase = GetProjectMemberUseCaseImpl(projectMemberRepository)
    }

    @Test
    fun `invoke success should call repository and return member`() = runTest {
        // Arrange
        `when`(projectMemberRepository.getProjectMember(testProjectId, testUserId))
            .thenReturn(Result.success(testMember))

        // Act
        val result = getProjectMemberUseCase(testProjectId, testUserId)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(testMember, result.getOrNull())
        verify(projectMemberRepository).getProjectMember(testProjectId, testUserId)
    }

    @Test
    fun `invoke member not found should call repository and return success with null`() = runTest {
        // Arrange
        `when`(projectMemberRepository.getProjectMember(testProjectId, testUserId))
            .thenReturn(Result.success(null))

        // Act
        val result = getProjectMemberUseCase(testProjectId, testUserId)

        // Assert
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        verify(projectMemberRepository).getProjectMember(testProjectId, testUserId)
    }

    @Test
    fun `invoke failure should call repository and return failure`() = runTest {
        // Arrange
        val exception = RuntimeException("Get member failed")
        `when`(projectMemberRepository.getProjectMember(testProjectId, testUserId))
            .thenReturn(Result.failure(exception))

        // Act
        val result = getProjectMemberUseCase(testProjectId, testUserId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(projectMemberRepository).getProjectMember(testProjectId, testUserId)
    }
}
