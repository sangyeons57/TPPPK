package com.example.data_model.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.domain.model.sync.SyncCursorStore

@Entity(tableName = "syncMetadata")
data class SyncMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "stream")
    val stream: String,

    @ColumnInfo(name = "cursor")
    val cursor: String?,
)

@Dao
interface SyncMetadataDao {
    @Query("SELECT cursor FROM syncMetadata WHERE stream = :stream")
    suspend fun getCursor(stream: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(syncMetadataEntity: SyncMetadataEntity)

    // ================================
    // 캐시 관리 관련 쿼리
    // ================================

    /**
     * 특정 스트림의 동기화 메타데이터 삭제
     * @param stream 삭제할 스트림명 (예: "messages")
     * @return 삭제된 레코드 수
     */
    @Query("DELETE FROM syncMetadata WHERE stream = :stream")
    suspend fun deleteByStream(stream: String): Int

    /**
     * 특정 스트림의 동기화 커서를 null로 리셋 (처음부터 다시 동기화)
     * @param stream 리셋할 스트림명
     */
    @Query("UPDATE syncMetadata SET cursor = NULL WHERE stream = :stream")
    suspend fun resetCursor(stream: String): Int

    /**
     * 모든 동기화 메타데이터 삭제 (전체 캐시 클리어용)
     * @return 삭제된 레코드 수
     */
    @Query("DELETE FROM syncMetadata")
    suspend fun deleteAll(): Int

}

class RoomSyncCursorStore(
    private val dao: SyncMetadataDao
) : SyncCursorStore {
    override suspend fun getCursor(stream: String): String? = dao.getCursor(stream)

    override suspend fun saveCursor(stream: String, cursor: String?) =
        dao.upsert(SyncMetadataEntity(stream, cursor))
}


