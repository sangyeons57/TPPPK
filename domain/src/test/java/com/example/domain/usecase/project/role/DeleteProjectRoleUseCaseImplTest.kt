package com.example.domain.usecase.project.role

import com.example.domain.repository.base.ProjectRoleRepository
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
class DeleteProjectRoleUseCaseImplTest {

    @Mock
    private lateinit var projectRoleRepository: ProjectRoleRepository

    private lateinit var deleteProjectRoleUseCase: DeleteProjectRoleUseCaseImpl

    private val testProjectId = "project1"
    private val testRoleId = "role1"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        deleteProjectRoleUseCase = DeleteProjectRoleUseCaseImpl(projectRoleRepository)
    }

    @Test
    fun `invoke success should call repository and return success`() = runTest {
        // Arrange
        `when`(projectRoleRepository.deleteRole(testProjectId, testRoleId))
            .thenReturn(Result.success(Unit))

        // Act
        val result = deleteProjectRoleUseCase(testProjectId, testRoleId)

        // Assert
        assertTrue(result.isSuccess)
        verify(projectRoleRepository).deleteRole(testProjectId, testRoleId)
    }

    @Test
    fun `invoke failure should call repository and return failure`() = runTest {
        // Arrange
        val exception = RuntimeException("Delete role failed")
        `when`(projectRoleRepository.deleteRole(testProjectId, testRoleId))
            .thenReturn(Result.failure(exception))

        // Act
        val result = deleteProjectRoleUseCase(testProjectId, testRoleId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(projectRoleRepository).deleteRole(testProjectId, testRoleId)
    }
}
