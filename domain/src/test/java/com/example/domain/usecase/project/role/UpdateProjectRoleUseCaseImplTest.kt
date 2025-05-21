package com.example.domain.usecase.project.role

import com.example.domain.model.Role
import com.example.domain.model.RolePermission
import com.example.domain.repository.ProjectRoleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlin.Result
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class UpdateProjectRoleUseCaseImplTest {

    @Mock
    private lateinit var projectRoleRepository: ProjectRoleRepository

    private lateinit var updateProjectRoleUseCase: UpdateProjectRoleUseCaseImpl

    private val testProjectId = "project1"
    private val testRoleId = "role1"

    private val originalRole = Role(
        id = testRoleId,
        projectId = testProjectId,
        name = "Original Name",
        permissions = mapOf(RolePermission.MANAGE_MEMBERS to true),
        isDefault = false,
        memberCount = 1
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        updateProjectRoleUseCase = UpdateProjectRoleUseCaseImpl(projectRoleRepository)
    }

    @Test
    fun `invoke with all parameters null should fetch role and update with original values for name and permissions`() = runTest {
        // Arrange
        `when`(projectRoleRepository.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.success(originalRole))
        `when`(projectRoleRepository.updateRole(testProjectId, testRoleId, originalRole.name, originalRole.permissions, null))
            .thenReturn(Result.success(Unit))

        // Act
        val result = updateProjectRoleUseCase(testProjectId, testRoleId, null, null, null)

        // Assert
        assertTrue(result.isSuccess)
        verify(projectRoleRepository).getRoleDetails(testProjectId, testRoleId)
        verify(projectRoleRepository).updateRole(testProjectId, testRoleId, originalRole.name, originalRole.permissions, null)
    }

    @Test
    fun `invoke with new name should fetch role and update only name`() = runTest {
        // Arrange
        val newName = "New Role Name"
        `when`(projectRoleRepository.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.success(originalRole))
        `when`(projectRoleRepository.updateRole(testProjectId, testRoleId, newName, originalRole.permissions, null))
            .thenReturn(Result.success(Unit))

        // Act
        val result = updateProjectRoleUseCase(testProjectId, testRoleId, newName, null, null)

        // Assert
        assertTrue(result.isSuccess)
        verify(projectRoleRepository).getRoleDetails(testProjectId, testRoleId)
        verify(projectRoleRepository).updateRole(testProjectId, testRoleId, newName, originalRole.permissions, null)
    }

    @Test
    fun `invoke with new permissions should fetch role and update only permissions`() = runTest {
        // Arrange
        val newPermissions = mapOf(RolePermission.MANAGE_TASKS to true)
        `when`(projectRoleRepository.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.success(originalRole))
        `when`(projectRoleRepository.updateRole(testProjectId, testRoleId, originalRole.name, newPermissions, null))
            .thenReturn(Result.success(Unit))

        // Act
        val result = updateProjectRoleUseCase(testProjectId, testRoleId, null, newPermissions, null)

        // Assert
        assertTrue(result.isSuccess)
        verify(projectRoleRepository).getRoleDetails(testProjectId, testRoleId)
        verify(projectRoleRepository).updateRole(testProjectId, testRoleId, originalRole.name, newPermissions, null)
    }

    @Test
    fun `invoke with new isDefault should fetch role and update only isDefault`() = runTest {
        // Arrange
        val newIsDefault = true
        `when`(projectRoleRepository.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.success(originalRole))
        `when`(projectRoleRepository.updateRole(testProjectId, testRoleId, originalRole.name, originalRole.permissions, newIsDefault))
            .thenReturn(Result.success(Unit))

        // Act
        val result = updateProjectRoleUseCase(testProjectId, testRoleId, null, null, newIsDefault)

        // Assert
        assertTrue(result.isSuccess)
        verify(projectRoleRepository).getRoleDetails(testProjectId, testRoleId)
        verify(projectRoleRepository).updateRole(testProjectId, testRoleId, originalRole.name, originalRole.permissions, newIsDefault)
    }

    @Test
    fun `invoke with all new values should fetch role and update all values`() = runTest {
        // Arrange
        val newName = "Completely New Name"
        val newPermissions = mapOf(RolePermission.VIEW_PROJECT to true, RolePermission.MANAGE_ROLES to false)
        val newIsDefault = true
        `when`(projectRoleRepository.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.success(originalRole)) // Still need to mock this for the initial fetch
        `when`(projectRoleRepository.updateRole(testProjectId, testRoleId, newName, newPermissions, newIsDefault))
            .thenReturn(Result.success(Unit))

        // Act
        val result = updateProjectRoleUseCase(testProjectId, testRoleId, newName, newPermissions, newIsDefault)

        // Assert
        assertTrue(result.isSuccess)
        verify(projectRoleRepository).getRoleDetails(testProjectId, testRoleId) // Verifies it was called
        verify(projectRoleRepository).updateRole(testProjectId, testRoleId, newName, newPermissions, newIsDefault)
    }
    
    @Test
    fun `invoke when getRoleDetails fails should return failure`() = runTest {
        // Arrange
        val exception = RuntimeException("Fetch role failed")
        `when`(projectRoleRepository.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.failure(exception))

        // Act
        val result = updateProjectRoleUseCase(testProjectId, testRoleId, "New Name", null, null)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(projectRoleRepository).getRoleDetails(testProjectId, testRoleId)
        verify(projectRoleRepository, never()).updateRole(anyString(), anyString(), anyString(), anyMap(), anyBoolean())
    }

    @Test
    fun `invoke when getRoleDetails returns null (role not found) should return failure`() = runTest {
        // Arrange
        `when`(projectRoleRepository.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.success(null))

        // Act
        val result = updateProjectRoleUseCase(testProjectId, testRoleId, "New Name", null, null)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Role with ID $testRoleId not found") ?: false)
        verify(projectRoleRepository).getRoleDetails(testProjectId, testRoleId)
        verify(projectRoleRepository, never()).updateRole(anyString(), anyString(), anyString(), anyMap(), anyBoolean())
    }
    
    @Test
    fun `invoke when updateRole fails should return failure`() = runTest {
        // Arrange
        val newName = "New Name"
        val exception = RuntimeException("Update role failed")
        `when`(projectRoleRepository.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.success(originalRole))
        `when`(projectRoleRepository.updateRole(testProjectId, testRoleId, newName, originalRole.permissions, null))
            .thenReturn(Result.failure(exception))

        // Act
        val result = updateProjectRoleUseCase(testProjectId, testRoleId, newName, null, null)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(projectRoleRepository).getRoleDetails(testProjectId, testRoleId)
        verify(projectRoleRepository).updateRole(testProjectId, testRoleId, newName, originalRole.permissions, null)
    }
}
