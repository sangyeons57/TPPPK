package com.example.domain.usecase.project.role

import com.example.domain.model.Role
import com.example.domain.model.RolePermission
import com.example.domain.model.project.RoleSortOption
import com.example.domain.repository.base.ProjectRoleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class GetProjectRolesUseCaseImplTest {

    @Mock
    private lateinit var projectRoleRepository: ProjectRoleRepository

    private lateinit var getProjectRolesUseCase: GetProjectRolesUseCaseImpl

    private val testProjectId = "project1"

    private val role1 = Role("role1", testProjectId, "Alpha Role", mapOf(RolePermission.MANAGE_MEMBERS to true), false, 3)
    private val role2 = Role("role2", testProjectId, "Beta Role", mapOf(RolePermission.MANAGE_TASKS to true), true, 5)
    private val role3 = Role("role3", testProjectId, "gamma Role", mapOf(RolePermission.VIEW_PROJECT to true), false, 1) // Lowercase for sorting
    private val role4 = Role("role4", testProjectId, "Delta Role", mapOf(), true, 10)


    private val allRoles = listOf(role1, role2, role3, role4)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        getProjectRolesUseCase = GetProjectRolesUseCaseImpl(projectRoleRepository)
        `when`(projectRoleRepository.getRolesStream(testProjectId)).thenReturn(flowOf(allRoles))
    }

    @Test
    fun `invoke with no filter and no sort should return all roles`() = runTest {
        // Act
        val result = getProjectRolesUseCase(testProjectId, null, null).first()

        // Assert
        assertEquals(allRoles.size, result.size)
        assertTrue(result.containsAll(allRoles))
        verify(projectRoleRepository).getRolesStream(testProjectId)
    }

    @Test
    fun `invoke with filterIsDefault true should return only default roles`() = runTest {
        // Act
        val result = getProjectRolesUseCase(testProjectId, true, null).first()
        val expectedRoles = allRoles.filter { it.isDefault }

        // Assert
        assertEquals(expectedRoles.size, result.size)
        assertTrue(result.containsAll(expectedRoles))
    }

    @Test
    fun `invoke with filterIsDefault false should return only non-default roles`() = runTest {
        // Act
        val result = getProjectRolesUseCase(testProjectId, false, null).first()
        val expectedRoles = allRoles.filter { !it.isDefault }

        // Assert
        assertEquals(expectedRoles.size, result.size)
        assertTrue(result.containsAll(expectedRoles))
    }

    @Test
    fun `invoke with sortBy NAME_ASC should return roles sorted by name ascending`() = runTest {
        // Act
        val result = getProjectRolesUseCase(testProjectId, null, RoleSortOption.NAME_ASC).first()

        // Assert
        assertEquals(allRoles.sortedBy { it.name.lowercase() }, result)
    }

    @Test
    fun `invoke with sortBy NAME_DESC should return roles sorted by name descending`() = runTest {
        // Act
        val result = getProjectRolesUseCase(testProjectId, null, RoleSortOption.NAME_DESC).first()

        // Assert
        assertEquals(allRoles.sortedByDescending { it.name.lowercase() }, result)
    }
    
    @Test
    fun `invoke with filterIsDefault true and sortBy NAME_ASC should return filtered and sorted roles`() = runTest {
        // Act
        val result = getProjectRolesUseCase(testProjectId, true, RoleSortOption.NAME_ASC).first()
        val expectedRoles = allRoles.filter { it.isDefault }.sortedBy { it.name.lowercase() }

        // Assert
        assertEquals(expectedRoles, result)
    }
}
