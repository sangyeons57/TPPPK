package com.example.domain.usecase.project.role

import com.example.domain.model.RolePermission
import com.example.domain.repository.ProjectRoleRepository
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
class CreateProjectRoleUseCaseImplTest {

    @Mock
    private lateinit var projectRoleRepository: ProjectRoleRepository

    private lateinit var createProjectRoleUseCase: CreateProjectRoleUseCaseImpl

    private val testProjectId = "project1"
    private val testRoleName = "New Role"
    private val testPermissions = mapOf(RolePermission.MANAGE_TASKS to true)
    private val testIsDefault = false
    private val expectedRoleId = "newRoleId"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        createProjectRoleUseCase = CreateProjectRoleUseCaseImpl(projectRoleRepository)
    }

    @Test
    fun `invoke success should call repository and return role ID`() = runTest {
        // Arrange
        `when`(projectRoleRepository.createRole(testProjectId, testRoleName, testPermissions, testIsDefault))
            .thenReturn(Result.success(expectedRoleId))

        // Act
        val result = createProjectRoleUseCase(testProjectId, testRoleName, testPermissions, testIsDefault)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(expectedRoleId, result.getOrNull())
        verify(projectRoleRepository).createRole(testProjectId, testRoleName, testPermissions, testIsDefault)
    }

    @Test
    fun `invoke failure should call repository and return failure`() = runTest {
        // Arrange
        val exception = RuntimeException("Create role failed")
        `when`(projectRoleRepository.createRole(testProjectId, testRoleName, testPermissions, testIsDefault))
            .thenReturn(Result.failure(exception))

        // Act
        val result = createProjectRoleUseCase(testProjectId, testRoleName, testPermissions, testIsDefault)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(projectRoleRepository).createRole(testProjectId, testRoleName, testPermissions, testIsDefault)
    }

    @Test
    fun `invoke with isDefault true should call repository with isDefault true`() = runTest {
        // Arrange
        val isDefaultTrue = true
        `when`(projectRoleRepository.createRole(testProjectId, testRoleName, testPermissions, isDefaultTrue))
            .thenReturn(Result.success(expectedRoleId))

        // Act
        val result = createProjectRoleUseCase(testProjectId, testRoleName, testPermissions, isDefaultTrue)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(expectedRoleId, result.getOrNull())
        verify(projectRoleRepository).createRole(testProjectId, testRoleName, testPermissions, isDefaultTrue)
    }
}
