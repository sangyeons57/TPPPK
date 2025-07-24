package com.example.data.dao

import androidx.room.*
import com.example.data.model.local.ChannelSyncEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 채널 동기화 상태 관리를 위한 DAO
 * 증분 동기화와 페이지네이션 메타데이터를 효율적으로 관리
 */
@Dao
interface ChannelSyncDao {

    /**
     * 채널의 동기화 정보 조회
     * @param channelId 채널 ID
     * @return 동기화 정보 (없으면 null)
     */
    @Query("SELECT * FROM channel_sync_info WHERE channelId = :channelId")
    suspend fun getSyncInfo(channelId: String): ChannelSyncEntity?

    /**
     * 채널의 동기화 정보를 실시간으로 관찰
     * @param channelId 채널 ID
     * @return 동기화 정보 Flow
     */
    @Query("SELECT * FROM channel_sync_info WHERE channelId = :channelId")
    fun observeSyncInfo(channelId: String): Flow<ChannelSyncEntity?>

    /**
     * 동기화 정보 삽입/업데이트
     * @param syncInfo 동기화 정보
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncInfo(syncInfo: ChannelSyncEntity)

    /**
     * 동기화 정보 업데이트
     * @param syncInfo 업데이트할 동기화 정보
     */
    @Update
    suspend fun updateSyncInfo(syncInfo: ChannelSyncEntity)

    /**
     * 마지막 동기화 시간만 업데이트 (빠른 업데이트용)
     * @param channelId 채널 ID
     * @param timestamp 새로운 동기화 시간
     * @param syncAt 동기화 실행 시간
     */
    @Query(
        """
        UPDATE channel_sync_info 
        SET lastSyncTimestamp = :timestamp, 
            lastSyncAt = :syncAt,
            syncFailureCount = 0
        WHERE channelId = :channelId
    """
    )
    suspend fun updateLastSyncTimestamp(channelId: String, timestamp: Instant, syncAt: Instant)

    /**
     * 메시지 개수와 타임스탬프 정보 업데이트
     * @param channelId 채널 ID
     * @param messageCount 새로운 메시지 개수
     * @param oldestTimestamp 가장 오래된 메시지 시간
     * @param newestTimestamp 가장 최신 메시지 시간
     */
    @Query(
        """
        UPDATE channel_sync_info 
        SET messageCount = :messageCount,
            oldestMessageTimestamp = :oldestTimestamp,
            newestMessageTimestamp = :newestTimestamp,
            lastSyncAt = :syncAt
        WHERE channelId = :channelId
    """
    )
    suspend fun updateMessageStats(
        channelId: String,
        messageCount: Int,
        oldestTimestamp: Instant?,
        newestTimestamp: Instant?,
        syncAt: Instant
    )

    /**
     * 페이지네이션 상태 업데이트
     * @param channelId 채널 ID
     * @param hasMoreOlderMessages 더 오래된 메시지 존재 여부
     */
    @Query(
        """
        UPDATE channel_sync_info 
        SET hasMoreOlderMessages = :hasMoreOlderMessages 
        WHERE channelId = :channelId
    """
    )
    suspend fun updatePaginationState(channelId: String, hasMoreOlderMessages: Boolean)

    /**
     * 동기화 실패 횟수 증가
     * @param channelId 채널 ID
     */
    @Query(
        """
        UPDATE channel_sync_info 
        SET syncFailureCount = syncFailureCount + 1,
            lastSyncAt = :syncAt
        WHERE channelId = :channelId
    """
    )
    suspend fun incrementSyncFailureCount(channelId: String, syncAt: Instant)

    /**
     * 동기화 실패 횟수 초기화
     * @param channelId 채널 ID
     */
    @Query(
        """
        UPDATE channel_sync_info 
        SET syncFailureCount = 0 
        WHERE channelId = :channelId
    """
    )
    suspend fun resetSyncFailureCount(channelId: String)

    /**
     * 모든 채널의 동기화 정보 조회 (전체 동기화용)
     * @return 모든 동기화 정보 목록
     */
    @Query("SELECT * FROM channel_sync_info ORDER BY lastSyncAt DESC")
    suspend fun getAllSyncInfo(): List<ChannelSyncEntity>

    /**
     * 동기화가 필요한 채널들 조회 (배경 동기화용)
     * @param staleThreshold 오래된 동기화 기준 시간
     * @return 동기화가 필요한 채널 목록
     */
    @Query(
        """
        SELECT * FROM channel_sync_info 
        WHERE lastSyncAt < :staleThreshold 
        OR syncFailureCount > 0
        ORDER BY syncFailureCount DESC, lastSyncAt ASC
    """
    )
    suspend fun getStaleChannels(staleThreshold: Instant): List<ChannelSyncEntity>

    /**
     * 특정 채널의 동기화 정보 삭제
     * @param channelId 채널 ID
     */
    @Query("DELETE FROM channel_sync_info WHERE channelId = :channelId")
    suspend fun deleteSyncInfo(channelId: String)

    /**
     * 모든 동기화 정보 삭제 (앱 데이터 초기화용)
     */
    @Query("DELETE FROM channel_sync_info")
    suspend fun deleteAllSyncInfo()

    /**
     * 채널의 동기화 정보 존재 여부 확인
     * @param channelId 채널 ID
     * @return 존재 여부
     */
    @Query("SELECT EXISTS(SELECT 1 FROM channel_sync_info WHERE channelId = :channelId)")
    suspend fun syncInfoExists(channelId: String): Boolean
}