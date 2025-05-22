package com.example.domain.usecase.project.member

import com.example.domain.model.ProjectMember
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
class GetMembersInRoleUseCaseImplTest {

    @Mock
    private lateinit var projectMemberRepository: ProjectMemberRepository

    private lateinit var getMembersInRoleUseCase: GetMembersInRoleUseCaseImpl

    private val testProjectId = "project1"
    private val testRoleId = "targetRole"

    private val member1 = ProjectMember("user1", "Alice", null, listOf("targetRole", "otherRole"), 1000L)
    private val member2 = ProjectMember("user2", "Bob", null, listOf("otherRole"), 2000L)
    private val member3 = ProjectMember("user3", "Charlie", null, listOf("targetRole"), 500L)
    private val member4 = ProjectMember("user4", "David", null, listOf(), 1500L)


    private val allMembers = listOf(member1, member2, member3, member4)
    private val membersInTargetRole = listOf(member1, member3)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        getMembersInRoleUseCase = GetMembersInRoleUseCaseImpl(projectMemberRepository)
    }

    @Test
    fun `invoke should return only members with the specified roleId`() = runTest {
        // Arrange
        `when`(projectMemberRepository.getProjectMembersStream(testProjectId)).thenReturn(flowOf(allMembers))

        // Act
        val result = getMembersInRoleUseCase(testProjectId, testRoleId).first()

        // Assert
        assertEquals(membersInTargetRole.size, result.size)
        assertTrue(result.containsAll(membersInTargetRole))
        assertFalse(result.contains(member2)) // Bob should not be there
        assertFalse(result.contains(member4)) // David should not be there
        verify(projectMemberRepository).getProjectMembersStream(testProjectId)
    }

    @Test
    fun `invoke when no members have the roleId should return empty list`() = runTest {
        // Arrange
        val roleWithNoMembers = "nonExistentRole"
        `when`(projectMemberRepository.getProjectMembersStream(testProjectId)).thenReturn(flowOf(allMembers))

        // Act
        val result = getMembersInRoleUseCase(testProjectId, roleWithNoMembers).first()

        // Assert
        assertTrue(result.isEmpty())
        verify(projectMemberRepository).getProjectMembersStream(testProjectId)
    }

    @Test
    fun `invoke when member list is empty should return empty list`() = runTest {
        // Arrange
        `when`(projectMemberRepository.getProjectMembersStream(testProjectId)).thenReturn(flowOf(emptyList()))

        // Act
        val result = getMembersInRoleUseCase(testProjectId, testRoleId).first()

        // Assert
        assertTrue(result.isEmpty())
        verify(projectMemberRepository).getProjectMembersStream(testProjectId)
    }
}
