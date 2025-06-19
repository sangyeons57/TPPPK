package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.PermissionRemoteDataSource
import com.example.data.datasource.remote.RoleRemoteDataSource
import com.example.data.model.remote.RoleDTO
import com.example.data.model.remote.toDomain
import com.example.domain.model.base.Role
import com.example.domain.model.vo.DocumentId
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.time.Instant

@ExperimentalCoroutinesApi
class RoleRepositoryImplTest {

    @Mock
    private lateinit var roleRemoteDataSource: RoleRemoteDataSource

    @Mock
    private lateinit var permissionRemoteDataSource: PermissionRemoteDataSource

    private lateinit var roleRepository: RoleRepositoryImpl

    private val testProjectId = "testProject1"
    private val testRoleId = "testRole1"
    private val testRoleName = "Test Role"
    private val testIsDefault = false

    private lateinit var testRole: Role
    private lateinit var testRoleDto: RoleDTO

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        roleRepository = RoleRepositoryImpl(roleRemoteDataSource, permissionRemoteDataSource)

        val now = Instant.now()
        testRole = Role.fromDataSource(
            id = DocumentId(testRoleId),
            name = testRoleName,
            isDefault = testIsDefault,
            createdAt = now,
            updatedAt = now
        )

        testRoleDto = RoleDTO(
            id = testRoleId,
            name = testRoleName,
            isDefault = testIsDefault,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
    }

    @Test
    fun `getRoleDetails success should return role from remote data source`() = runTest {
        // Arrange
        `when`(roleRemoteDataSource.observeRole(testProjectId, testRoleId))
            .thenReturn(flowOf(CustomResult.Success(testRoleDto)))

        // Act
        val result = roleRepository.getRoleDetails(testProjectId, testRoleId)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(testRole.id, result.getOrNull()?.id)
        assertEquals(testRole.name, result.getOrNull()?.name)
        verify(roleRemoteDataSource).observeRole(testProjectId, testRoleId)
    }

    @Test
    fun `createRole success should return role ID from remote data source`() = runTest {
        // Arrange
        `when`(roleRemoteDataSource.addRole(any(String::class.java), any(RoleDTO::class.java)))
            .thenReturn(CustomResult.Success(testRoleId))

        // Act
        val result = roleRepository.createRole(testProjectId, testRoleName, testIsDefault)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(testRoleId, result.getOrNull())
    }

    @Test
    fun `updateRole success should return success from remote data source`() = runTest {
        // Arrange
        val updatedName = "Updated Role Name"
        val updatedIsDefault = true
        val updates = mapOf(
            "name" to updatedName,
            "isDefault" to updatedIsDefault
        )
        `when`(roleRemoteDataSource.updateRole(testProjectId, testRoleId, updates))
            .thenReturn(CustomResult.Success(Unit))

        // Act
        val result = roleRepository.updateRole(testProjectId, testRoleId, updatedName, updatedIsDefault)

        // Assert
        assertTrue(result.isSuccess)
        verify(roleRemoteDataSource).updateRole(testProjectId, testRoleId, updates)
    }

    @Test
    fun `deleteRole success should return success from remote data source`() = runTest {
        // Arrange
        `when`(roleRemoteDataSource.deleteRole(testProjectId, testRoleId))
            .thenReturn(CustomResult.Success(Unit))

        // Act
        val result = roleRepository.deleteRole(testProjectId, testRoleId)

        // Assert
        assertTrue(result.isSuccess)
        verify(roleRemoteDataSource).deleteRole(testProjectId, testRoleId)
    }

    @Test
    fun `deleteRole failure should return failure from remote data source`() = runTest {
        // Arrange
        val exception = RuntimeException("Delete error")
        `when`(roleRemoteDataSource.deleteRole(testProjectId, testRoleId))
            .thenReturn(CustomResult.Failure(exception))

        // Act
        val result = roleRepository.deleteRole(testProjectId, testRoleId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
