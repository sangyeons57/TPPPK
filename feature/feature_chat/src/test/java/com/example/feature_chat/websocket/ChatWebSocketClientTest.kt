package com.example.feature_chat.websocket

import com.example.core_common.websocket.WebSocketConnectionState
import com.example.core_common.websocket.WebSocketManager
import com.example.core_common.websocket.WebSocketMessage
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ChatWebSocketClientTest {

    private lateinit var mockWebSocketManager: WebSocketManager
    private lateinit var chatWebSocketClient: ChatWebSocketClient

    @Before
    fun setup() {
        mockWebSocketManager = mockk()
        chatWebSocketClient = ChatWebSocketClient(mockWebSocketManager)
    }

    @Test
    fun `test connection state is properly exposed`() = runTest {
        // Given
        val connectionState = MutableStateFlow<WebSocketConnectionState>(WebSocketConnectionState.Disconnected)
        every { mockWebSocketManager.connectionState } returns connectionState

        // When
        val result = chatWebSocketClient.connectionState.value

        // Then
        assertEquals(WebSocketConnectionState.Disconnected, result)
    }

    @Test
    fun `test send message creates correct WebSocket message`() = runTest {
        // Given
        val roomId = "test_room"
        val senderId = UserId("user123")
        val content = "Hello, World!"
        val messageId = DocumentId("msg456")
        
        coEvery { mockWebSocketManager.sendMessage(any()) } returns Result.success(Unit)

        // When
        val result = chatWebSocketClient.sendMessage(roomId, senderId, content, messageId)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockWebSocketManager.sendMessage(
                match { message ->
                    message.type == WebSocketMessage.TYPE_MESSAGE &&
                    message.roomId == roomId &&
                    message.senderId == senderId.value &&
                    message.content == content &&
                    message.messageId == messageId.value
                }
            )
        }
    }

    @Test
    fun `test edit message creates correct WebSocket message`() = runTest {
        // Given
        val roomId = "test_room"
        val messageId = DocumentId("msg456")
        val newContent = "Edited message"
        
        coEvery { mockWebSocketManager.sendMessage(any()) } returns Result.success(Unit)

        // When
        val result = chatWebSocketClient.editMessage(roomId, messageId, newContent)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockWebSocketManager.sendMessage(
                match { message ->
                    message.type == WebSocketMessage.TYPE_EDIT_MESSAGE &&
                    message.roomId == roomId &&
                    message.messageId == messageId.value &&
                    message.content == newContent
                }
            )
        }
    }

    @Test
    fun `test delete message creates correct WebSocket message`() = runTest {
        // Given
        val roomId = "test_room"
        val messageId = DocumentId("msg456")
        
        coEvery { mockWebSocketManager.sendMessage(any()) } returns Result.success(Unit)

        // When
        val result = chatWebSocketClient.deleteMessage(roomId, messageId)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            mockWebSocketManager.sendMessage(
                match { message ->
                    message.type == WebSocketMessage.TYPE_DELETE_MESSAGE &&
                    message.roomId == roomId &&
                    message.messageId == messageId.value
                }
            )
        }
    }

    @Test
    fun `test getChatMessages filters by room ID`() = runTest {
        // Given
        val roomId = "test_room"
        val messages = flowOf(
            WebSocketMessage(
                type = WebSocketMessage.TYPE_MESSAGE,
                roomId = roomId,
                messageId = "msg1",
                senderId = "user1",
                content = "Message for correct room"
            ),
            WebSocketMessage(
                type = WebSocketMessage.TYPE_MESSAGE,
                roomId = "other_room",
                messageId = "msg2",
                senderId = "user2",
                content = "Message for other room"
            )
        )
        
        every { mockWebSocketManager.incomingMessages } returns messages

        // When
        val events = mutableListOf<ChatWebSocketEvent>()
        chatWebSocketClient.getChatMessages(roomId).collect { event ->
            events.add(event)
        }

        // Then
        assertEquals(1, events.size)
        assertTrue(events[0] is ChatWebSocketEvent.MessageReceived)
        val messageEvent = events[0] as ChatWebSocketEvent.MessageReceived
        assertEquals("msg1", messageEvent.messageId)
        assertEquals("Message for correct room", messageEvent.content)
    }

    @Test
    fun `test join room sends correct message`() = runTest {
        // Given
        val roomId = "test_room"
        coEvery { mockWebSocketManager.joinRoom(roomId) } returns Result.success(Unit)

        // When
        val result = chatWebSocketClient.joinRoom(roomId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockWebSocketManager.joinRoom(roomId) }
    }

    @Test
    fun `test leave room sends correct message`() = runTest {
        // Given
        val roomId = "test_room"
        coEvery { mockWebSocketManager.leaveRoom(roomId) } returns Result.success(Unit)

        // When
        val result = chatWebSocketClient.leaveRoom(roomId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockWebSocketManager.leaveRoom(roomId) }
    }
}