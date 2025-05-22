package com.example.domain.usecase.project.role

import com.example.domain.model.Role
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
class GetProjectRoleUseCaseImplTest {

    @Mock
    private lateinit var projectRoleRepository: ProjectRoleRepository

    private lateinit var getProjectRoleUseCase: GetProjectRoleUseCaseImpl

    private val testProjectId = "project1"
    private val testRoleId = "role1"
    private val testRole = Role(
        id = testRoleId,
        projectId = testProjectId,
        name = "Test Role",
        permissions = mapOf(RolePermission.MANAGE_MEMBERS to true),
        isDefault = false,
        memberCount = 5
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        getProjectRoleUseCase = GetProjectRoleUseCaseImpl(projectRoleRepository)
    }

    @Test
    fun `invoke success should call repository and return role`() = runTest {
        // Arrange
        `when`(projectRoleRepository.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.success(testRole))

        // Act
        val result = getProjectRoleUseCase(testProjectId, testRoleId)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(testRole, result.getOrNull())
        verify(projectRoleRepository).getRoleDetails(testProjectId, testRoleId)
    }

    @Test
    fun `invoke role not found should call repository and return success with null`() = runTest {
        // Arrange
        `when`(projectRoleRepository.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.success(null))

        // Act
        val result = getProjectRoleUseCase(testProjectId, testRoleId)

        // Assert
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        verify(projectRoleRepository).getRoleDetails(testProjectId, testRoleId)
    }

    @Test
    fun `invoke failure should call repository and return failure`() = runTest {
        // Arrange
        val exception = RuntimeException("Get role failed")
        `when`(projectRoleRepository.getRoleDetails(testProjectId, testRoleId))
            .thenReturn(Result.failure(exception))

        // Act
        val result = getProjectRoleUseCase(testProjectId, testRoleId)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(projectRoleRepository).getRoleDetails(testProjectId, testRoleId)
    }
}
