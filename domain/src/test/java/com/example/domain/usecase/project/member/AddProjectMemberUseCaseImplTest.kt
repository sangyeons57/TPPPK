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
class AddProjectMemberUseCaseImplTest {

    @Mock
    private lateinit var projectMemberRepository: ProjectMemberRepository

    private lateinit var addProjectMemberUseCase: AddProjectMemberUseCaseImpl

    private val testProjectId = "project1"
    private val testUserId = "user1"
    private val testRoleIds = listOf("role1", "role2")

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        addProjectMemberUseCase = AddProjectMemberUseCaseImpl(projectMemberRepository)
    }

    @Test
    fun `invoke success should call repository and return success`() = runTest {
        // Arrange
        `when`(projectMemberRepository.addMemberToProject(testProjectId, testUserId, testRoleIds))
            .thenReturn(Result.success(Unit))

        // Act
        val result = addProjectMemberUseCase(testProjectId, testUserId, testRoleIds)

        // Assert
        assertTrue(result.isSuccess)
        verify(projectMemberRepository).addMemberToProject(testProjectId, testUserId, testRoleIds)
    }

    @Test
    fun `invoke failure should call repository and return failure`() = runTest {
        // Arrange
        val exception = RuntimeException("Add member failed")
        `when`(projectMemberRepository.addMemberToProject(testProjectId, testUserId, testRoleIds))
            .thenReturn(Result.failure(exception))

        // Act
        val result = addProjectMemberUseCase(testProjectId, testUserId, testRoleIds)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(projectMemberRepository).addMemberToProject(testProjectId, testUserId, testRoleIds)
    }
}
