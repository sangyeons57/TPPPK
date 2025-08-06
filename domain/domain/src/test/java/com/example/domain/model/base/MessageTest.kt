package com.example.domain.model.base

import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageTest {

    @Test
    fun `create should create new TEXT message with payload`() {
        // Given
        val id = DocumentId("test-id")
        val senderId = UserId("user-1")
        val payload = MessagePayload.forText("Hello World")
        val channelId = ChannelId("channel-1")

        // When
        val message = Message.create(
            id = id,
            senderId = senderId,
            messageType = MessageType.TEXT,
            payload = payload,
            replyToMessageId = null,
            mentions = emptyList(),
            channelId = channelId
        )

        // Then
        assertEquals("test-id", message.id.value)
        assertEquals("user-1", message.senderId.value)
        assertEquals(MessageType.TEXT, message.messageType)
        assertEquals("Hello World", message.payload.getTextContent())
        assertEquals("channel-1", message.channelId.value)
        assertTrue(message.isNew)
    }

    @Test
    fun `createTextMessage should create TEXT message with convenience method`() {
        // Given
        val textContent = "Simple text message"

        // When
        val message = Message.createTextMessage(
            id = DocumentId("test-id"),
            senderId = UserId("user-1"),
            textContent = textContent,
            channelId = ChannelId("channel-1")
        )

        // Then
        assertEquals(MessageType.TEXT, message.messageType)
        assertEquals(textContent, message.payload.getTextContent())
    }

    @Test
    fun `createSystemMessage should create system message`() {
        // Given
        val payload = MessagePayload.forDateSystem("2024-01-01", "2024년 1월 1일")

        // When
        val message = Message.createSystemMessage(
            id = DocumentId("test-id"),
            senderId = UserId("system"),
            messageType = MessageType.SYSTEM_DATE,
            payload = payload,
            channelId = ChannelId("channel-1")
        )

        // Then
        assertEquals(MessageType.SYSTEM_DATE, message.messageType)
        assertEquals("2024년 1월 1일", message.payload.getValue("displayText"))
        assertEquals("2024-01-01", message.payload.getValue("date"))
    }

    @Test
    fun `updatePayload should update message payload`() {
        // Given
        val message = Message.createTextMessage(
            id = DocumentId("test-id"),
            senderId = UserId("user-1"),
            textContent = "Original text",
            channelId = ChannelId("channel-1")
        )
        val newPayload = MessagePayload.forText("Updated text")

        // When
        message.updatePayload(newPayload)

        // Then
        assertEquals("Updated text", message.payload.getTextContent())
    }

    @Test
    fun `delete should mark message as deleted`() {
        // Given
        val message = Message.createTextMessage(
            id = DocumentId("test-id"),
            senderId = UserId("user-1"),
            textContent = "To be deleted",
            channelId = ChannelId("channel-1")
        )

        // When
        message.delete()

        // Then
        assertTrue(message.isDeleted.value)
    }

    @Test
    fun `MessagePayload factory methods should create valid JSON`() {
        // Given & When
        val textPayload = MessagePayload.forText("Hello")
        val datePayload = MessagePayload.forDateSystem("2024-01-01", "Today")
        val projectPayload = MessagePayload.forProjectJoin("proj-1", "My Project")
        val chatStartPayload = MessagePayload.forChatStart("General")

        // Then
        assertEquals("Hello", textPayload.getTextContent())
        assertEquals("Today", datePayload.getValue("displayText"))
        assertEquals("My Project", projectPayload.getValue("projectName"))
        assertEquals("General", chatStartPayload.getValue("channelName"))
    }
}