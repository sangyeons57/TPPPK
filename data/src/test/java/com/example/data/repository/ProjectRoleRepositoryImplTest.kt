package com.example.data.repository

import com.example.data.datasource.remote.projectrole.ProjectRoleRemoteDataSource
import com.example.domain.model.Role
import com.example.domain.model.RolePermission
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
class ProjectRoleRepositoryImplTest {

    @Mock
    private lateinit var remoteDataSource: ProjectRoleRemoteDataSource

    private lateinit var projectRoleRepository: ProjectRoleRepositoryImpl

    private val testProjectId = "testProject1"
    private val testRoleId = "testRole1"
    private val testRoleName = "Test Role"
    private val testPermissions = mapOf(RolePermission.MANAGE_ROLES to true)
    private val testIsDefault = false

    private val testRole = Role(
        id = testRoleId,
        projectId = testProjectId,
        name = testRoleName,
        permissions = testPermissions,
        isDefault = testIsDefault,
        memberCount = 0
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        projectRoleRepository = ProjectRoleRepositoryImpl(remoteDataSource)
    }

    // Test for getRoleDetails
    @Test
    fun `getRoleDetails success should return role from remote data source`() = runTest {
        // Arrange
        `when`(remoteDataSource.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.success(testRole))

        // Act
        val result = projectRoleRepository.getRoleDetails(testProjectId, testRoleId)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(testRole, result.getOrNull())
        verify(remoteDataSource).getRoleDetails(testProjectId, testRoleId)
    }

    @Test
    fun `getRoleDetails when remote data source returns null should return success with null`() = runTest {
        // Arrange
        `when`(remoteDataSource.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.success(null))

        // Act
        val result = projectRoleRepository.getRoleDetails(testProjectId, testRoleId)

        // Assert
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getRoleDetails failure should return failure from remote data source`() = runTest {
        // Arrange
        val exception = RuntimeException("Network error")
        `when`(remoteDataSource.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.failure(exception))

        // Act
        val result = projectRoleRepository.getRoleDetails(testProjectId, testRoleId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    // Test for createRole
    @Test
    fun `createRole success should return role ID from remote data source`() = runTest {
        // Arrange
        `when`(remoteDataSource.createRole(testProjectId, testRoleName, testPermissions, testIsDefault))
            .thenReturn(Result.success(testRoleId))

        // Act
        val result = projectRoleRepository.createRole(testProjectId, testRoleName, testPermissions, testIsDefault)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(testRoleId, result.getOrNull())
        verify(remoteDataSource).createRole(testProjectId, testRoleName, testPermissions, testIsDefault)
    }

    @Test
    fun `createRole failure should return failure from remote data source`() = runTest {
        // Arrange
        val exception = RuntimeException("Create error")
        `when`(remoteDataSource.createRole(testProjectId, testRoleName, testPermissions, testIsDefault))
            .thenReturn(Result.failure(exception))

        // Act
        val result = projectRoleRepository.createRole(testProjectId, testRoleName, testPermissions, testIsDefault)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    // Test for updateRole
    @Test
    fun `updateRole success should return success from remote data source`() = runTest {
        // Arrange
        val updatedName = "Updated Role Name"
        val updatedPermissions = mapOf(RolePermission.MANAGE_MEMBERS to true)
        val updatedIsDefault = true
        `when`(remoteDataSource.updateRole(testProjectId, testRoleId, updatedName, updatedPermissions, updatedIsDefault))
            .thenReturn(Result.success(Unit))

        // Act
        val result = projectRoleRepository.updateRole(testProjectId, testRoleId, updatedName, updatedPermissions, updatedIsDefault)

        // Assert
        assertTrue(result.isSuccess)
        verify(remoteDataSource).updateRole(testProjectId, testRoleId, updatedName, updatedPermissions, updatedIsDefault)
    }

    @Test
    fun `updateRole failure should return failure from remote data source`() = runTest {
        // Arrange
        val exception = RuntimeException("Update error")
        `when`(remoteDataSource.updateRole(testProjectId, testRoleId, testRoleName, testPermissions, testIsDefault))
            .thenReturn(Result.failure(exception))

        // Act
        val result = projectRoleRepository.updateRole(testProjectId, testRoleId, testRoleName, testPermissions, testIsDefault)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
    
    // Test for updateRole with nullable isDefault
    @Test
    fun `updateRole with null isDefault success should call remote with null isDefault`() = runTest {
        // Arrange
        `when`(remoteDataSource.updateRole(testProjectId, testRoleId, testRoleName, testPermissions, null))
            .thenReturn(Result.success(Unit))

        // Act
        val result = projectRoleRepository.updateRole(testProjectId, testRoleId, testRoleName, testPermissions, null)

        // Assert
        assertTrue(result.isSuccess)
        verify(remoteDataSource).updateRole(testProjectId, testRoleId, testRoleName, testPermissions, null)
    }


    // Test for deleteRole
    @Test
    fun `deleteRole success should return success from remote data source`() = runTest {
        // Arrange
        `when`(remoteDataSource.deleteRole(testProjectId, testRoleId))
            .thenReturn(Result.success(Unit))

        // Act
        val result = projectRoleRepository.deleteRole(testProjectId, testRoleId)

        // Assert
        assertTrue(result.isSuccess)
        verify(remoteDataSource).deleteRole(testProjectId, testRoleId)
    }

    @Test
    fun `deleteRole failure should return failure from remote data source`() = runTest {
        // Arrange
        val exception = RuntimeException("Delete error")
        `when`(remoteDataSource.deleteRole(testProjectId, testRoleId))
            .thenReturn(Result.failure(exception))

        // Act
        val result = projectRoleRepository.deleteRole(testProjectId, testRoleId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
