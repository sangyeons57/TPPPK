package com.example.domain.usecase

import com.example.domain.model.Channel
import com.example.domain.repository.FakeChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * GetChannelUseCase에 대한 단위 테스트입니다.
 */
@ExperimentalCoroutinesApi
class GetChannelUseCaseTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var channelRepository: FakeChannelRepository
    private lateinit var getChannelUseCase: GetChannelUseCase
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        channelRepository = FakeChannelRepository(testDispatcher)
        getChannelUseCase = GetChannelUseCase(channelRepository)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `when channel exists, returns success with channel`() = runTest {
        // Given
        val testChannel = createTestChannel("test_channel_id")
        channelRepository.saveChannel(testChannel)
        
        // When
        val result = getChannelUseCase("test_channel_id").first()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testChannel, result.getOrNull())
    }
    
    @Test
    fun `when channel does not exist, returns failure`() = runTest {
        // When
        val result = getChannelUseCase("non_existent_id").first()
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }
    
    private fun createTestChannel(
        id: String,
        name: String = "Test Channel",
        ownerId: String = "user1",
        participantIds: List<String> = listOf("user1", "user2"),
        createdAt: LocalDateTime = LocalDateTime.now()
    ): Channel {
        return Channel(
            id = id,
            name = name,
            description = "Test description",
            ownerId = ownerId,
            participantIds = participantIds,
            lastMessagePreview = null,
            lastMessageTimestamp = null,
            metadata = null,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }
} 