package com.example.data_model.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.base.Message
import com.example.domain.model.enum.SyncStatus
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.message.MentionInfo
import com.example.domain.model.vo.message.MessageContent
import com.example.domain.model.vo.message.MessageIsDeleted
import java.time.Instant

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "sender_id")
    val senderId: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "reply_to_message_id")
    val replyToMessageId: String? = null,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "mentions")
    val mentions: String = "[]", // JSON string of mentions

    @ColumnInfo(name = "created_at")
    val createdAt: Long, // Epoch milliseconds

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long, // Epoch milliseconds

    // Sync metadata fields
    @ColumnInfo(name = "server_version")
    val serverVersion: Long? = null,

    @ColumnInfo(name = "server_updated_at")
    val serverUpdatedAt: Long? = null, // Epoch milliseconds, server time

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = SyncStatus.DEFAULT.name,

    @ColumnInfo(name = "deleted")
    val deleted: Boolean = false
) {

    @Deprecated(
        message = "Use MessageMapper.entityToDomain() instead. " +
                "Direct conversion in Entity violates Clean Architecture.",
        replaceWith = ReplaceWith(
            "messageMapper.entityToDomain(this)",
            "com.example.mapper.message.MessageMapper"
        )
    )
    fun toDomainModel(): Message {
        val mentionsList = emptyList<MentionInfo>()

        return Message.fromDataSource(
            id = DocumentId(this.id),
            senderId = UserId(this.senderId),
            content = MessageContent(this.content),
            replyToMessageId = this.replyToMessageId?.let { DocumentId(it) },
            createdAt = Instant.ofEpochMilli(this.createdAt),
            updatedAt = Instant.ofEpochMilli(this.updatedAt),
            isDeleted = if (this.isDeleted) MessageIsDeleted.TRUE else MessageIsDeleted.FALSE,
            mentions = mentionsList
        )
    }

    companion object {
        @Deprecated(
            message = "Use MessageMapper.domainToEntity() or domainToEntityWithSync() instead. " +
                    "Direct conversion in Entity violates Clean Architecture.",
            replaceWith = ReplaceWith(
                "messageMapper.domainToEntityWithSync(message, serverVersion, serverUpdatedAt, syncStatus, deleted)",
                "com.example.mapper.message.MessageMapper"
            )
        )
        fun fromDomainModel(
            message: Message,
            serverVersion: Long? = null,
            serverUpdatedAt: Long? = null,
            syncStatus: SyncStatus = SyncStatus.DEFAULT,
            deleted: Boolean = false
        ): MessageEntity {
            val mentionsJson = "[]"

            return MessageEntity(
                id = message.id.value,
                senderId = message.senderId.value,
                content = message.content.value,
                replyToMessageId = message.replyToMessageId?.value,
                isDeleted = message.isDeleted.value,
                mentions = mentionsJson,
                createdAt = message.createdAt.toEpochMilli(),
                updatedAt = message.updatedAt.toEpochMilli(),
                serverVersion = serverVersion,
                serverUpdatedAt = serverUpdatedAt,
                syncStatus = syncStatus.name,
                deleted = deleted
            )
        }
    }

    fun needsSync(): Boolean = SyncStatus.valueOf(syncStatus).needsSync()
    fun isInSync(): Boolean = SyncStatus.valueOf(syncStatus).isCompleted()
    fun getSyncStatusEnum(): SyncStatus = SyncStatus.valueOf(syncStatus)
}