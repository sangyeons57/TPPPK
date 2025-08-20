package com.example.mapper.message

import com.example.data_model.local.MessageEntity
import com.example.data_model.remote.MessageDTO
import com.example.domain.model.base.Message
import com.example.domain.vo.ChannelId
import com.example.domain.vo.DocumentId
import com.example.domain.vo.UserId
import com.example.domain.vo.message.MessagePayload
import com.example.domain.vo.message.MessageType
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

class MessageMapperTest {

    private lateinit var messageMapper: MessageMapper

    @Before
    fun setup() {
        messageMapper = MessageMapper()
    }

    @Test
    fun `entityToDomain should convert MessageEntity to Message with payload`() {
        // Given
        val entity = MessageEntity(
            id = "test-id",
            channelId = "channel-1",
            senderId = "user-1",
            messageType = "TEXT",
            payload = """{"content": "Hello World"}""",
            replyToMessageId = null,
            isDeleted = false,
            mentions = "[]",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // When
        val result = messageMapper.entityToDomain(entity)

        // Then
        assertEquals("test-id", result.id.value)
        assertEquals("channel-1", result.channelId.value)
        assertEquals("user-1", result.senderId.value)
        assertEquals(MessageType.TEXT, result.messageType)
        assertEquals("""{"content": "Hello World"}""", result.payload.value)
        assertEquals("Hello World", result.payload.getTextContent())
    }

    @Test
    fun `domainToEntity should convert Message to MessageEntity`() {
        // Given
        val message = Message.create(
            id = DocumentId("test-id"),
            senderId = UserId("user-1"),
            messageType = MessageType.TEXT,
            payload = MessagePayload.forText("Hello World"),
            replyToMessageId = null,
            mentions = emptyList(),
            channelId = ChannelId("channel-1")
        )

        // When
        val result = messageMapper.domainToEntity(message)

        // Then
        assertEquals("test-id", result.id)
        assertEquals("channel-1", result.channelId)
        assertEquals("user-1", result.senderId)
        assertEquals("TEXT", result.messageType)
        assertEquals("""{"content": "Hello World"}""", result.payload)
    }

    @Test
    fun `dtoToDomain should parse payload map`() {
        // Given - DTO with payload as Map
        val dto = MessageDTO(
            id = "test-id",
            channelId = "channel-1",
            senderId = "user-1",
            messageType = "TEXT",
            payload = mapOf("content" to "New payload content"),
            replyToMessageId = null,
            isDeleted = false,
            mentions = emptyList(),
            createdAt = null,
            updatedAt = null
        )

        // When
        val result = messageMapper.dtoToDomain(dto)

        // Then
        assertEquals("New payload content", result.payload.getTextContent())
    }

    @Test
    fun `domainToDto should create DTO with payload`() {
        // Given - 직접 JSON 구성으로 시스템 메시지 페이로드 생성
        val jsonString = """
            {
                "date": "2024-01-01",
                "displayText": "2024년 1월 1일"
            }
        """.trimIndent()
        val payload = MessagePayload(jsonString)
        
        val message = Message.create(
            id = DocumentId("test-id"),
            senderId = UserId("user-1"),
            messageType = MessageType.SYSTEM_DATE,
            payload = payload,
            replyToMessageId = null,
            mentions = emptyList(),
            channelId = ChannelId("channel-1")
        )

        // When
        val result = messageMapper.domainToDto(message)

        // Then
        assertEquals("test-id", result.id)
        assertEquals("SYSTEM_DATE", result.messageType)
        @Suppress("UNCHECKED_CAST")
        val payloadMap = result.payload as Map<String, Any?>
        assertEquals("2024-01-01", payloadMap["date"])
        assertEquals("2024년 1월 1일", payloadMap["displayText"])
        // content field removed; only payload is present
    }
}
