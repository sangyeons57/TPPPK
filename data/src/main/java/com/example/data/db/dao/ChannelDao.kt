package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.local.ChannelEntity
import com.example.domain.model.ChannelType
import kotlinx.coroutines.flow.Flow

/**
 * 채널 데이터 접근 객체 인터페이스
 * Room 데이터베이스의 channels 테이블에 접근하기 위한 메서드들을 정의합니다.
 */
@Dao
interface ChannelDao {
    
    /**
     * 채널 목록을 가져옵니다.
     */
    @Query("SELECT * FROM channels")
    fun getAllChannels(): Flow<List<ChannelEntity>>
    
    /**
     * 특정 채널을 ID로 조회합니다.
     */
    @Query("SELECT * FROM channels WHERE id = :channelId")
    suspend fun getChannelById(channelId: String): ChannelEntity?
    
    /**
     * 특정 채널을 ID로 구독합니다.
     */
    @Query("SELECT * FROM channels WHERE id = :channelId")
    fun observeChannelById(channelId: String): Flow<ChannelEntity?>
    
    /**
     * 특정 프로젝트의 모든 채널을 조회합니다.
     */
    @Query("SELECT * FROM channels WHERE projectId = :projectId ORDER BY channelOrder ASC")
    suspend fun getChannelsByProject(projectId: String): List<ChannelEntity>
    
    /**
     * 특정 프로젝트의 모든 채널을 구독합니다.
     */
    @Query("SELECT * FROM channels WHERE projectId = :projectId ORDER BY channelOrder ASC")
    fun observeChannelsByProject(projectId: String): Flow<List<ChannelEntity>>
    
    /**
     * 특정 유형의 채널을 조회합니다.
     */
    @Query("SELECT * FROM channels WHERE type = :type")
    suspend fun getChannelsByType(type: String): List<ChannelEntity>
    
    /**
     * 특정 유형의 채널을 구독합니다.
     */
    @Query("SELECT * FROM channels WHERE type = :type")
    fun observeChannelsByType(type: String): Flow<List<ChannelEntity>>
    
    /**
     * DM 채널 중 특정 사용자가 참여한 채널을 조회합니다.
     * SQL로 직접 필터링할 수 없으므로, 타입만 필터링하고 결과를 클라이언트에서 추가 필터링해야 합니다.
     */
    @Query("SELECT * FROM channels WHERE type = 'DM'")
    suspend fun getDmChannels(): List<ChannelEntity>
    
    /**
     * DM 채널 중 특정 사용자가 참여한 채널을 구독합니다.
     * SQL로 직접 필터링할 수 없으므로, 타입만 필터링하고 결과를 클라이언트에서 추가 필터링해야 합니다.
     */
    @Query("SELECT * FROM channels WHERE type = 'DM'")
    fun observeDmChannels(): Flow<List<ChannelEntity>>
    
    /**
     * 채널을 저장합니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)
    
    /**
     * 여러 채널을 한 번에 저장합니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)
    
    /**
     * 채널을 업데이트합니다.
     */
    @Update
    suspend fun updateChannel(channel: ChannelEntity)
    
    /**
     * 채널을 삭제합니다.
     */
    @Query("DELETE FROM channels WHERE id = :channelId")
    suspend fun deleteChannel(channelId: String)
    
    /**
     * 특정 프로젝트의 모든 채널을 삭제합니다.
     */
    @Query("DELETE FROM channels WHERE projectId = :projectId")
    suspend fun deleteChannelsByProject(projectId: String)
    
    /**
     * 모든 채널을 삭제합니다.
     */
    @Query("DELETE FROM channels")
    suspend fun deleteAllChannels()
}