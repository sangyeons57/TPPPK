package com.example.domain.usecase.project.member

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
class RemoveProjectMemberUseCaseImplTest {

    @Mock
    private lateinit var projectMemberRepository: ProjectMemberRepository

    private lateinit var removeProjectMemberUseCase: RemoveProjectMemberUseCaseImpl

    private val testProjectId = "project1"
    private val testUserId = "user1"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        removeProjectMemberUseCase = RemoveProjectMemberUseCaseImpl(projectMemberRepository)
    }

    @Test
    fun `invoke success should call repository and return success`() = runTest {
        // Arrange
        `when`(projectMemberRepository.removeMemberFromProject(testProjectId, testUserId))
            .thenReturn(Result.success(Unit))

        // Act
        val result = removeProjectMemberUseCase(testProjectId, testUserId)

        // Assert
        assertTrue(result.isSuccess)
        verify(projectMemberRepository).removeMemberFromProject(testProjectId, testUserId)
    }

    @Test
    fun `invoke failure should call repository and return failure`() = runTest {
        // Arrange
        val exception = RuntimeException("Remove member failed")
        `when`(projectMemberRepository.removeMemberFromProject(testProjectId, testUserId))
            .thenReturn(Result.failure(exception))

        // Act
        val result = removeProjectMemberUseCase(testProjectId, testUserId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(projectMemberRepository).removeMemberFromProject(testProjectId, testUserId)
    }
}
