package com.example.domain.usecase.project.member

import com.example.domain.model.ProjectMember
import com.example.domain.model.project.MemberSortOption
import com.example.domain.repository.ProjectMemberRepository
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
class GetProjectMembersUseCaseImplTest {

    @Mock
    private lateinit var projectMemberRepository: ProjectMemberRepository

    private lateinit var getProjectMembersUseCase: GetProjectMembersUseCaseImpl

    private val testProjectId = "project1"

    private val member1 = ProjectMember("user1", "Alice", null, listOf("role1", "role2"), 1000L)
    private val member2 = ProjectMember("user2", "Bob", null, listOf("role2"), 2000L)
    private val member3 = ProjectMember("user3", "charlie", null, listOf("role1"), 500L) // Lowercase for sorting test
    private val member4 = ProjectMember("user4", "David", null, listOf("role3"), 1500L)

    private val allMembers = listOf(member1, member2, member3, member4)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        getProjectMembersUseCase = GetProjectMembersUseCaseImpl(projectMemberRepository)
        `when`(projectMemberRepository.getProjectMembersStream(testProjectId)).thenReturn(flowOf(allMembers))
    }

    @Test
    fun `invoke with no filter and no sort should return all members`() = runTest {
        // Act
        val result = getProjectMembersUseCase(testProjectId, null, null).first()

        // Assert
        assertEquals(allMembers.size, result.size)
        assertTrue(result.containsAll(allMembers))
        verify(projectMemberRepository).getProjectMembersStream(testProjectId)
    }

    @Test
    fun `invoke with roleIdFilter should return filtered members`() = runTest {
        val roleIdToFilter = "role1"
        // Act
        val result = getProjectMembersUseCase(testProjectId, roleIdToFilter, null).first()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.all { it.roleIds.contains(roleIdToFilter) })
        assertTrue(result.contains(member1))
        assertTrue(result.contains(member3))
    }

    @Test
    fun `invoke with sortBy NAME_ASC should return members sorted by name ascending`() = runTest {
        // Act
        val result = getProjectMembersUseCase(testProjectId, null, MemberSortOption.NAME_ASC).first()

        // Assert
        assertEquals(listOf(member1, member2, member3, member4).sortedBy { it.userName.lowercase() }, result)
    }

    @Test
    fun `invoke with sortBy NAME_DESC should return members sorted by name descending`() = runTest {
        // Act
        val result = getProjectMembersUseCase(testProjectId, null, MemberSortOption.NAME_DESC).first()

        // Assert
        assertEquals(listOf(member1, member2, member3, member4).sortedByDescending { it.userName.lowercase() }, result)
    }
    
    @Test
    fun `invoke with sortBy JOINED_AT_ASC should return members sorted by joinedAt ascending`() = runTest {
        // Act
        val result = getProjectMembersUseCase(testProjectId, null, MemberSortOption.JOINED_AT_ASC).first()

        // Assert
        assertEquals(allMembers.sortedBy { it.joinedAt }, result)
    }

    @Test
    fun `invoke with sortBy JOINED_AT_DESC should return members sorted by joinedAt descending`() = runTest {
        // Act
        val result = getProjectMembersUseCase(testProjectId, null, MemberSortOption.JOINED_AT_DESC).first()

        // Assert
        assertEquals(allMembers.sortedByDescending { it.joinedAt }, result)
    }
    
    @Test
    fun `invoke with roleIdFilter and sortBy NAME_ASC should return filtered and sorted members`() = runTest {
        val roleIdToFilter = "role2"
        // Expected: member1 (Alice), member2 (Bob) -> sorted: member1, member2
        val expectedMembers = listOf(member1, member2).sortedBy { it.userName.lowercase() }
        
        // Act
        val result = getProjectMembersUseCase(testProjectId, roleIdToFilter, MemberSortOption.NAME_ASC).first()

        // Assert
        assertEquals(expectedMembers, result)
    }
}
