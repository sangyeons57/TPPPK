package com.example.data_model.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.sync.ScopeMetadata
import java.time.Instant

@Entity(tableName = ScopeMetadata.TABLE_NAME)
data class ScopeMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = ScopeMetadata.COLUMN_KEY)
    val key: String,

    @ColumnInfo(name = ScopeMetadata.COLUMN_LAST_CURSOR)
    val lastCursor: Long, // Epoch milliseconds

    @ColumnInfo(name = ScopeMetadata.COLUMN_LAST_SYNCED_AT)
    val lastSyncedAt: Long, // Epoch milliseconds

    @ColumnInfo(name = ScopeMetadata.COLUMN_SYNC_COUNT)
    val syncCount: Long = 0,

    @ColumnInfo(name = ScopeMetadata.COLUMN_ERROR_COUNT)
    val errorCount: Long = 0,

    @ColumnInfo(name = ScopeMetadata.COLUMN_LAST_ERROR_MESSAGE)
    val lastErrorMessage: String? = null
) {

    fun toDomainModel(): ScopeMetadata {
        return ScopeMetadata.fromDataSource(
            key = this.key,
            lastCursor = Instant.ofEpochMilli(this.lastCursor),
            lastSyncedAt = Instant.ofEpochMilli(this.lastSyncedAt),
            syncCount = this.syncCount,
            errorCount = this.errorCount,
            lastErrorMessage = this.lastErrorMessage
        )
    }

    companion object {
        fun fromDomainModel(scopeMetadata: ScopeMetadata): ScopeMetadataEntity {
            return ScopeMetadataEntity(
                key = scopeMetadata.key,
                lastCursor = scopeMetadata.getLastCursor().toEpochMilli(),
                lastSyncedAt = scopeMetadata.getLastSyncedAt().toEpochMilli(),
                syncCount = scopeMetadata.getSyncCount(),
                errorCount = scopeMetadata.getErrorCount(),
                lastErrorMessage = scopeMetadata.getLastErrorMessage()
            )
        }
    }
}