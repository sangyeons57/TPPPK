package com.example.domain.model.base

import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

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
        // Given - 직접 JSON 구성으로 시스템 메시지 페이로드 생성
        val jsonString = """
            {
                "date": "2024-01-01",
                "displayText": "2024년 1월 1일"
            }
        """.trimIndent()
        val payload = MessagePayload(jsonString)

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
        // Given & When - 남은 factory 메서드와 직접 JSON 구성 테스트
        val textPayload = MessagePayload.forText("Hello")

        // 직접 JSON 구성으로 시스템 메시지 페이로드들 생성
        val datePayloadJson = """
            {
                "date": "2024-01-01",
                "displayText": "Today"
            }
        """.trimIndent()
        val datePayload = MessagePayload(datePayloadJson)

        val projectPayloadJson = """
            {
                "projectId": "proj-1",
                "projectName": "My Project",
                "actionText": "참여하기"
            }
        """.trimIndent()
        val projectPayload = MessagePayload(projectPayloadJson)

        val chatStartPayloadJson = """
            {
                "channelName": "General",
                "welcomeMessage": "채팅을 시작합니다"
            }
        """.trimIndent()
        val chatStartPayload = MessagePayload(chatStartPayloadJson)

        // Then
        assertEquals("Hello", textPayload.getTextContent())
        assertEquals("Today", datePayload.getValue("displayText"))
        assertEquals("My Project", projectPayload.getValue("projectName"))
        assertEquals("General", chatStartPayload.getValue("channelName"))
    }
}