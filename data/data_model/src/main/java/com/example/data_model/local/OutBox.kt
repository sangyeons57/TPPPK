package com.example.data_model.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.domain.enum.OutBoxStatus
import com.example.domain.model.sync.OutBoxRecord

@Entity(tableName = "outboxRecord")
data class OutboxRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "stream")
    val stream: String,

    @ColumnInfo(name = "aggregateId")
    val aggregateId: String,

    @ColumnInfo(name = "op")
    val op: String,

    @ColumnInfo(name = "payload")
    val payload: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long,

    @ColumnInfo(name = "attempt")
    val attempt: Int,

    @ColumnInfo(name = "status")
    val status: String, // OutBoxStatus.value
)


@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun enqueue(outboxRecordEntity: OutboxRecordEntity)

    @Query(
        """
        SELECT * FROM outboxRecord
        WHERE stream = :stream AND status = 'PENDING'
        ORDER BY createdAt ASC
        LIMIT :limit
    """
    )
    suspend fun peek(stream: String, limit: Int): List<OutboxRecordEntity>

    @Query("UPDATE outboxRecord SET status='DISPATCHED' WHERE id IN (:ids)")
    suspend fun markDispatched(ids: List<String>)

    @Query("UPDATE outboxRecord SET attempt = attempt + 1, status='FAILED' WHERE id IN (:ids)")
    suspend fun markFailed(ids: List<String>): Int

    // ================================
    // Message 전용 ACK 처리
    // ================================

    /**
     * Message ACK 수신 시 상태를 DISPATCHED로 변경
     */
    @Query("UPDATE outboxRecord SET status='DISPATCHED' WHERE aggregateId = :messageId AND stream = 'messages'")
    suspend fun markMessageDispatched(messageId: String): Int

    /**
     * 특정 메시지의 OutBox 상태 조회
     */
    @Query("SELECT * FROM outboxRecord WHERE aggregateId = :messageId AND stream = 'messages' LIMIT 1")
    suspend fun findByMessageId(messageId: String): OutboxRecordEntity?

    /**
     * 특정 채널의 PENDING 상태 메시지들 조회
     */
    @Query(
        """
        SELECT * FROM outboxRecord 
        WHERE stream = 'messages' AND status = 'PENDING' 
        AND payload LIKE '%' || :channelId || '%'
        ORDER BY createdAt ASC
    """
    )
    suspend fun getPendingMessagesByChannel(channelId: String): List<OutboxRecordEntity>

    /**
     * 특정 채널의 DISPATCHED 상태 메시지들 조회
     */
    @Query(
        """
        SELECT * FROM outboxRecord 
        WHERE stream = 'messages' AND status = 'DISPATCHED' 
        AND payload LIKE '%' || :channelId || '%'
        ORDER BY createdAt ASC
    """
    )
    suspend fun getDispatchedMessagesByChannel(channelId: String): List<OutboxRecordEntity>

    // ================================
    // 캐시 관리 관련 쿼리
    // ================================

    /**
     * 특정 스트림의 모든 OutBox 레코드 삭제
     * @param stream 삭제할 스트림명 (예: "messages")
     * @return 삭제된 레코드 수
     */
    @Query("DELETE FROM outboxRecord WHERE stream = :stream")
    suspend fun deleteByStream(stream: String): Int

    /**
     * 특정 채널과 관련된 모든 OutBox 레코드 삭제
     * messages 스트림에서 특정 채널ID를 포함하는 payload를 가진 레코드들을 삭제
     * @param channelId 삭제할 채널 ID
     * @return 삭제된 레코드 수
     */
    @Query("DELETE FROM outboxRecord WHERE stream = 'messages' AND payload LIKE '%' || :channelId || '%'")
    suspend fun deleteByChannelId(channelId: String): Int

    /**
     * 모든 OutBox 레코드 삭제 (전체 캐시 클리어용)
     * @return 삭제된 레코드 수
     */
    @Query("DELETE FROM outboxRecord")
    suspend fun deleteAll(): Int

    // ================================
    // 디버그 및 통계 쿼리
    // ================================

    /**
     * 특정 채널의 OutBox 상태별 개수 조회
     */
    @Query(
        """
        SELECT status, COUNT(*) as count 
        FROM outboxRecord 
        WHERE stream = 'messages' AND payload LIKE '%' || :channelId || '%'
        GROUP BY status
    """
    )
    suspend fun getOutBoxStatusCountsByChannel(channelId: String): List<OutBoxStatusCount>
}

/**
 * OutBox 상태별 개수 통계
 */
data class OutBoxStatusCount(
    val status: String,
    val count: Int
)

fun OutboxRecordEntity.toModel(): OutBoxRecord {
    return OutBoxRecord(
        id = id,
        stream = stream,
        aggregateId = aggregateId,
        op = OutBoxRecord.Op.valueOf(op),
        payload = payload,
        createdAt = createdAt,
        attempt = attempt,
    )
}

fun OutBoxRecord.toEntity(status: OutBoxStatus = OutBoxStatus.PENDING): OutboxRecordEntity {
    return OutboxRecordEntity(
        id = id,
        stream = stream,
        aggregateId = aggregateId,
        op = op.name,
        payload = payload,
        createdAt = createdAt,
        attempt = attempt,
        status = status.value
    )
}