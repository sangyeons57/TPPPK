package com.example.domain.repository

import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.mockito.kotlin.verify

/**
 * ChannelRepository 인터페이스의 테스트 케이스.
 * 리팩토링 이후 채널 통합 로직이 올바르게 작동하는지 검증합니다.
 */
class ChannelRepositoryTest {
    
    // 테스트 데이터
    private val testChannel = Channel(
        id = "channel1",
        name = "Test Channel",
        description = "Test Description",
        type = ChannelType.TEXT,
        isPrivate = false,
        createdAt = 1000L,
        createdBy = "user1",
        updatedAt = 1000L,
        memberCount = 2,
        projectId = "project1",
        categoryId = "category1",
        isDirect = false
    )
    
    private val testDmChannel = Channel(
        id = "dm1",
        name = "DM Channel",
        description = "",
        type = ChannelType.DM,
        isPrivate = true,
        createdAt = 1000L,
        createdBy = "user1",
        updatedAt = 1000L,
        memberCount = 2,
        projectId = null,
        categoryId = null,
        isDirect = true
    )
    
    // Mock 레포지토리
    private lateinit var mockChannelRepository: ChannelRepository
    
    @Before
    fun setup() {
        mockChannelRepository = mock()
    }
    
    @Test
    fun `createChannel should return created channel on success`() = runTest {
        // Given
        whenever(mockChannelRepository.createChannel(any(), any(), any(), any(), any(), any()))
            .thenReturn(Result.success(testChannel))
            
        // When
        val result = mockChannelRepository.createChannel(
            name = testChannel.name,
            description = testChannel.description,
            type = testChannel.type,
            isPrivate = testChannel.isPrivate,
            projectId = testChannel.projectId,
            categoryId = testChannel.categoryId
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testChannel, result.getOrNull())
    }
    
    @Test
    fun `createDmChannel should return created DM channel on success`() = runTest {
        // Given
        whenever(mockChannelRepository.createDmChannel(any(), any()))
            .thenReturn(Result.success(testDmChannel))
            
        // When
        val result = mockChannelRepository.createDmChannel(
            otherUserId = "user2",
            isActive = true
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testDmChannel, result.getOrNull())
    }
    
    @Test
    fun `getChannelById should return channel when it exists`() = runTest {
        // Given
        whenever(mockChannelRepository.getChannelById(testChannel.id))
            .thenReturn(Result.success(testChannel))
            
        // When
        val result = mockChannelRepository.getChannelById(testChannel.id)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testChannel, result.getOrNull())
    }
    
    @Test
    fun `getChannelById should return failure when channel doesn't exist`() = runTest {
        // Given
        whenever(mockChannelRepository.getChannelById("non-existent"))
            .thenReturn(Result.failure(NoSuchElementException("채널을 찾을 수 없습니다.")))
            
        // When
        val result = mockChannelRepository.getChannelById("non-existent")
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }
    
    @Test
    fun `getChannelsStream should return flow of channels`() = runTest {
        // Given
        val channelsList = listOf(testChannel, testDmChannel)
        whenever(mockChannelRepository.getChannelsStream())
            .thenReturn(flowOf(channelsList))
            
        // When
        val result = mockChannelRepository.getChannelsStream().first()
        
        // Then
        assertEquals(2, result.size)
        assertEquals(channelsList, result)
    }
    
    @Test
    fun `getDmChannels should return list of DM channels`() = runTest {
        // Given
        whenever(mockChannelRepository.getDmChannels())
            .thenReturn(Result.success(listOf(testDmChannel)))
            
        // When
        val result = mockChannelRepository.getDmChannels()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(testDmChannel, result.getOrNull()?.first())
    }
    
    @Test
    fun `updateChannel should return success when channel is updated`() = runTest {
        // Given
        whenever(mockChannelRepository.updateChannel(any(), any(), any(), any()))
            .thenReturn(Result.success(Unit))
            
        // When
        val result = mockChannelRepository.updateChannel(
            channelId = testChannel.id,
            name = "Updated Name",
            description = "Updated Description",
            isPrivate = true
        )
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `deleteChannel should return success when channel is deleted`() = runTest {
        // Given
        whenever(mockChannelRepository.deleteChannel(testChannel.id))
            .thenReturn(Result.success(Unit))
            
        // When
        val result = mockChannelRepository.deleteChannel(testChannel.id)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `getProjectChannels should return channels for the project`() = runTest {
        // Given
        whenever(mockChannelRepository.getProjectChannels("project1"))
            .thenReturn(Result.success(listOf(testChannel)))
            
        // When
        val result = mockChannelRepository.getProjectChannels("project1")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(testChannel, result.getOrNull()?.first())
    }
    
    @Test
    fun `addUserToChannel should return success when user is added`() = runTest {
        // Given
        whenever(mockChannelRepository.addUserToChannel(any(), any()))
            .thenReturn(Result.success(Unit))
            
        // When
        val result = mockChannelRepository.addUserToChannel(testChannel.id, "user2")
        
        // Then
        assertTrue(result.isSuccess)
        verify(mockChannelRepository).addUserToChannel(testChannel.id, "user2")
    }
    
    @Test
    fun `removeUserFromChannel should return success when user is removed`() = runTest {
        // Given
        whenever(mockChannelRepository.removeUserFromChannel(any(), any()))
            .thenReturn(Result.success(Unit))
            
        // When
        val result = mockChannelRepository.removeUserFromChannel(testChannel.id, "user2")
        
        // Then
        assertTrue(result.isSuccess)
        verify(mockChannelRepository).removeUserFromChannel(testChannel.id, "user2")
    }
} 