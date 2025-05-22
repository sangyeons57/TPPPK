package com.example.domain.usecase.project.member

import com.example.domain.repository.ProjectMemberRepository
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
class UpdateProjectMemberUseCaseImplTest {

    @Mock
    private lateinit var projectMemberRepository: ProjectMemberRepository

    private lateinit var updateProjectMemberUseCase: UpdateProjectMemberUseCaseImpl

    private val testProjectId = "project1"
    private val testUserId = "user1"
    private val testRoleIds = listOf("roleNew1", "roleNew2")
    private val testChannelsToAdd = listOf("channelAdd1", "channelAdd2")
    private val testChannelsToRemove = listOf("channelRemove1")

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        updateProjectMemberUseCase = UpdateProjectMemberUseCaseImpl(projectMemberRepository)
    }

    @Test
    fun `invoke with all parameters null should succeed without calling repository methods`() = runTest {
        // Act
        val result = updateProjectMemberUseCase(testProjectId, testUserId, null, null, null)

        // Assert
        assertTrue(result.isSuccess)
        verifyNoInteractions(projectMemberRepository)
    }

    @Test
    fun `invoke with only roleIds should call updateMemberRoles and succeed`() = runTest {
        // Arrange
        `when`(projectMemberRepository.updateMemberRoles(testProjectId, testUserId, testRoleIds))
            .thenReturn(Result.success(Unit))

        // Act
        val result = updateProjectMemberUseCase(testProjectId, testUserId, testRoleIds, null, null)

        // Assert
        assertTrue(result.isSuccess)
        verify(projectMemberRepository).updateMemberRoles(testProjectId, testUserId, testRoleIds)
        verify(projectMemberRepository, never()).addChannelAccessToMember(anyString(), anyString(), anyString())
        verify(projectMemberRepository, never()).removeChannelAccessFromMember(anyString(), anyString(), anyString())
    }

    @Test
    fun `invoke with only channelIdsToAdd should call addChannelAccessToMember and succeed`() = runTest {
        // Arrange
        testChannelsToAdd.forEach { channelId ->
            `when`(projectMemberRepository.addChannelAccessToMember(testProjectId, testUserId, channelId))
                .thenReturn(Result.success(Unit))
        }

        // Act
        val result = updateProjectMemberUseCase(testProjectId, testUserId, null, testChannelsToAdd, null)

        // Assert
        assertTrue(result.isSuccess)
        verify(projectMemberRepository, never()).updateMemberRoles(anyString(), anyString(), anyList())
        testChannelsToAdd.forEach { channelId ->
            verify(projectMemberRepository).addChannelAccessToMember(testProjectId, testUserId, channelId)
        }
        verify(projectMemberRepository, never()).removeChannelAccessFromMember(anyString(), anyString(), anyString())
    }

    @Test
    fun `invoke with only channelIdsToRemove should call removeChannelAccessFromMember and succeed`() = runTest {
        // Arrange
        testChannelsToRemove.forEach { channelId ->
            `when`(projectMemberRepository.removeChannelAccessFromMember(testProjectId, testUserId, channelId))
                .thenReturn(Result.success(Unit))
        }

        // Act
        val result = updateProjectMemberUseCase(testProjectId, testUserId, null, null, testChannelsToRemove)

        // Assert
        assertTrue(result.isSuccess)
        verify(projectMemberRepository, never()).updateMemberRoles(anyString(), anyString(), anyList())
        verify(projectMemberRepository, never()).addChannelAccessToMember(anyString(), anyString(), anyString())
        testChannelsToRemove.forEach { channelId ->
            verify(projectMemberRepository).removeChannelAccessFromMember(testProjectId, testUserId, channelId)
        }
    }

    @Test
    fun `invoke with all parameters should call all relevant repository methods and succeed`() = runTest {
        // Arrange
        `when`(projectMemberRepository.updateMemberRoles(testProjectId, testUserId, testRoleIds))
            .thenReturn(Result.success(Unit))
        testChannelsToAdd.forEach { channelId ->
            `when`(projectMemberRepository.addChannelAccessToMember(testProjectId, testUserId, channelId))
                .thenReturn(Result.success(Unit))
        }
        testChannelsToRemove.forEach { channelId ->
            `when`(projectMemberRepository.removeChannelAccessFromMember(testProjectId, testUserId, channelId))
                .thenReturn(Result.success(Unit))
        }

        // Act
        val result = updateProjectMemberUseCase(testProjectId, testUserId, testRoleIds, testChannelsToAdd, testChannelsToRemove)

        // Assert
        assertTrue(result.isSuccess)
        verify(projectMemberRepository).updateMemberRoles(testProjectId, testUserId, testRoleIds)
        testChannelsToAdd.forEach { channelId ->
            verify(projectMemberRepository).addChannelAccessToMember(testProjectId, testUserId, channelId)
        }
        testChannelsToRemove.forEach { channelId ->
            verify(projectMemberRepository).removeChannelAccessFromMember(testProjectId, testUserId, channelId)
        }
    }

    @Test
    fun `invoke updateMemberRoles fails should return failure and not call other methods`() = runTest {
        // Arrange
        val exception = RuntimeException("Roles update failed")
        `when`(projectMemberRepository.updateMemberRoles(testProjectId, testUserId, testRoleIds))
            .thenReturn(Result.failure(exception))

        // Act
        val result = updateProjectMemberUseCase(testProjectId, testUserId, testRoleIds, testChannelsToAdd, testChannelsToRemove)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(projectMemberRepository).updateMemberRoles(testProjectId, testUserId, testRoleIds)
        verify(projectMemberRepository, never()).addChannelAccessToMember(anyString(), anyString(), anyString())
        verify(projectMemberRepository, never()).removeChannelAccessFromMember(anyString(), anyString(), anyString())
    }

    @Test
    fun `invoke addChannelAccessToMember fails should return failure and not call subsequent methods`() = runTest {
        // Arrange
        val exception = RuntimeException("Add channel access failed")
        `when`(projectMemberRepository.updateMemberRoles(testProjectId, testUserId, testRoleIds))
            .thenReturn(Result.success(Unit)) // Roles update succeeds
        `when`(projectMemberRepository.addChannelAccessToMember(testProjectId, testUserId, testChannelsToAdd[0]))
            .thenReturn(Result.failure(exception)) // First add fails

        // Act
        val result = updateProjectMemberUseCase(testProjectId, testUserId, testRoleIds, testChannelsToAdd, testChannelsToRemove)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(projectMemberRepository).updateMemberRoles(testProjectId, testUserId, testRoleIds)
        verify(projectMemberRepository).addChannelAccessToMember(testProjectId, testUserId, testChannelsToAdd[0])
        // Ensure subsequent add calls and remove calls are not made
        if (testChannelsToAdd.size > 1) {
            verify(projectMemberRepository, never()).addChannelAccessToMember(testProjectId, testUserId, testChannelsToAdd[1])
        }
        verify(projectMemberRepository, never()).removeChannelAccessFromMember(anyString(), anyString(), anyString())
    }

    @Test
    fun `invoke removeChannelAccessFromMember fails should return failure`() = runTest {
        // Arrange
        val exception = RuntimeException("Remove channel access failed")
        `when`(projectMemberRepository.updateMemberRoles(testProjectId, testUserId, testRoleIds))
            .thenReturn(Result.success(Unit))
        testChannelsToAdd.forEach { channelId ->
            `when`(projectMemberRepository.addChannelAccessToMember(testProjectId, testUserId, channelId))
                .thenReturn(Result.success(Unit))
        }
        `when`(projectMemberRepository.removeChannelAccessFromMember(testProjectId, testUserId, testChannelsToRemove[0]))
            .thenReturn(Result.failure(exception)) // First remove fails

        // Act
        val result = updateProjectMemberUseCase(testProjectId, testUserId, testRoleIds, testChannelsToAdd, testChannelsToRemove)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(projectMemberRepository).updateMemberRoles(testProjectId, testUserId, testRoleIds)
        testChannelsToAdd.forEach { channelId ->
            verify(projectMemberRepository).addChannelAccessToMember(testProjectId, testUserId, channelId)
        }
        verify(projectMemberRepository).removeChannelAccessFromMember(testProjectId, testUserId, testChannelsToRemove[0])
        // Ensure subsequent remove calls are not made
        if (testChannelsToRemove.size > 1) {
             verify(projectMemberRepository, never()).removeChannelAccessFromMember(testProjectId, testUserId, testChannelsToRemove[1])
        }
    }
}
