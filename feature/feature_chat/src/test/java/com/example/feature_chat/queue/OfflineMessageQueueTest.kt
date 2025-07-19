package com.example.feature_chat.queue

import com.example.core_common.websocket.WebSocketConnectionState
import com.example.domain.model.base.Message
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MessageContent
import com.example.feature_chat.websocket.ChatWebSocketClient
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class OfflineMessageQueueTest {

    private lateinit var mockChatWebSocketClient: ChatWebSocketClient
    private lateinit var offlineMessageQueue: OfflineMessageQueue
    private lateinit var connectionStateFlow: MutableStateFlow<WebSocketConnectionState>

    @Before
    fun setup() {
        mockChatWebSocketClient = mockk()
        connectionStateFlow = MutableStateFlow(WebSocketConnectionState.Disconnected)
        
        every { mockChatWebSocketClient.connectionState } returns connectionStateFlow
        
        offlineMessageQueue = OfflineMessageQueue(mockChatWebSocketClient)
    }

    @Test
    fun `test queue message increases queue size`() {
        // Given
        val message = Message.create(
            id = DocumentId("test"),
            senderId = UserId("user1"),
            content = MessageContent("test message"),
            replyToMessageId = null
        )
        val action = QueuedMessageAction.Send(message, "room1")

        // When
        offlineMessageQueue.queueMessage(action)

        // Then
        assertEquals(1, offlineMessageQueue.getQueueSize())
    }

    @Test
    fun `test multiple queued messages`() {
        // Given
        val message1 = Message.create(
            id = DocumentId("test1"),
            senderId = UserId("user1"),
            content = MessageContent("test message 1"),
            replyToMessageId = null
        )
        val message2 = Message.create(
            id = DocumentId("test2"),
            senderId = UserId("user1"),
            content = MessageContent("test message 2"),
            replyToMessageId = null
        )
        
        val action1 = QueuedMessageAction.Send(message1, "room1")
        val action2 = QueuedMessageAction.Edit("msg1", "edited content", "room1")

        // When
        offlineMessageQueue.queueMessage(action1)
        offlineMessageQueue.queueMessage(action2)

        // Then
        assertEquals(2, offlineMessageQueue.getQueueSize())
    }

    @Test
    fun `test clear queue resets size to zero`() {
        // Given
        val message = Message.create(
            id = DocumentId("test"),
            senderId = UserId("user1"),
            content = MessageContent("test message"),
            replyToMessageId = null
        )
        val action = QueuedMessageAction.Send(message, "room1")
        offlineMessageQueue.queueMessage(action)

        // When
        offlineMessageQueue.clearQueue()

        // Then
        assertEquals(0, offlineMessageQueue.getQueueSize())
    }

    @Test
    fun `test queue processes when connection becomes available`() = runTest {
        // Given
        val message = Message.create(
            id = DocumentId("test"),
            senderId = UserId("user1"),
            content = MessageContent("test message"),
            replyToMessageId = null
        )
        val action = QueuedMessageAction.Send(message, "room1")
        
        coEvery { 
            mockChatWebSocketClient.sendMessage(any(), any(), any(), any(), any()) 
        } returns Result.success(Unit)
        
        offlineMessageQueue.queueMessage(action)
        assertEquals(1, offlineMessageQueue.getQueueSize())

        // When - Connection becomes available
        connectionStateFlow.value = WebSocketConnectionState.Connected

        // Give some time for the queue to process
        kotlinx.coroutines.delay(200)

        // Then
        coVerify {
            mockChatWebSocketClient.sendMessage(
                roomId = "room1",
                senderId = UserId("user1"),
                content = "test message",
                messageId = DocumentId("test"),
                replyToMessageId = null
            )
        }
    }
}