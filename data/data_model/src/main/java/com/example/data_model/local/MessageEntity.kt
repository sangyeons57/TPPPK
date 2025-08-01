package com.example.data_model.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core_common.constant.MessageDeliveryStatus
import com.example.domain.model.enum.SyncStatus

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

    // 클라이언트 측 메시지 전송 상태 (SENDING/SENT/FAILED)
    @ColumnInfo(name = "delivery_status")
    val deliveryStatus: String = MessageDeliveryStatus.SENT // Default to SENT for existing messages
) {


    companion object {
        // Companion object methods removed - use MessageMapper instead
    }

    fun needsSync(): Boolean = SyncStatus.valueOf(syncStatus).needsSync()
    fun isInSync(): Boolean = SyncStatus.valueOf(syncStatus).isCompleted()
    fun getSyncStatusEnum(): SyncStatus = SyncStatus.valueOf(syncStatus)
}