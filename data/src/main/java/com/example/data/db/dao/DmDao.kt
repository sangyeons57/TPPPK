package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.local.DmConversationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DM 대화 관련 데이터베이스 작업을 위한 DAO 인터페이스
 */
@Dao
interface DmDao {
    /**
     * 모든 DM 대화 목록을 가져옵니다.
     * @return DM 대화 목록의 Flow
     */
    @Query("SELECT * FROM dm_conversations ORDER BY lastMessageTimestamp DESC")
    fun getAllDmConversationsStream(): Flow<List<DmConversationEntity>>
    
    /**
     * 특정 DM 대화 정보를 가져옵니다.
     * @param dmId DM 채널 ID
     * @return DM 대화 정보 또는 null
     */
    @Query("SELECT * FROM dm_conversations WHERE id = :dmId LIMIT 1")
    suspend fun getDmConversationById(dmId: String): DmConversationEntity?
    
    /**
     * DM 대화 정보를 추가하거나 업데이트합니다.
     * @param dmConversation 추가/업데이트할 DM 대화 정보
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDmConversation(dmConversation: DmConversationEntity)
    
    /**
     * DM 대화 목록을 추가하거나 업데이트합니다.
     * @param dmConversations 추가/업데이트할 DM 대화 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDmConversations(dmConversations: List<DmConversationEntity>)
    
    /**
     * DM 대화 정보를 삭제합니다.
     * @param dmId 삭제할 DM 채널 ID
     * @return 삭제된 행 수
     */
    @Query("DELETE FROM dm_conversations WHERE id = :dmId")
    suspend fun deleteDmConversation(dmId: String): Int
    
    /**
     * 모든 DM 대화 데이터를 삭제합니다.
     */
    @Query("DELETE FROM dm_conversations")
    suspend fun deleteAllDmConversations()
    
    /**
     * 마지막 메시지를 업데이트합니다.
     * @param dmId DM 채널 ID
     * @param message 메시지 내용
     * @param timestamp 메시지 타임스탬프
     * @return 업데이트된 행 수
     */
    @Query("UPDATE dm_conversations SET lastMessage = :message, lastMessageTimestamp = :timestamp WHERE id = :dmId")
    suspend fun updateLastMessage(dmId: String, message: String, timestamp: Long): Int
} 