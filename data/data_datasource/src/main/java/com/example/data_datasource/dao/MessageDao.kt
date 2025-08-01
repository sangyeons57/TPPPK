package com.example.data_datasource.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    // ================================
    // 기본 CRUD 작업
    // ================================
    
    /**
     * Message 엔티티 삽입/업데이트
     * @param entity 저장할 Message 엔티티
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity)
    
    /**
     * 여러 Message 엔티티들을 배치로 삽입/업데이트
     * @param entities 저장할 Message 엔티티 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MessageEntity>)
    
    /**
     * Message 엔티티 업데이트
     * @param entity 업데이트할 Message 엔티티
     */
    @Update
    suspend fun update(entity: MessageEntity)
    
    /**
     * Message 엔티티 삭제
     * @param entity 삭제할 Message 엔티티
     */
    @Delete
    suspend fun delete(entity: MessageEntity)

    // ================================
    // 기본 조회 메서드
    // ================================
    
    /**
     * ID로 특정 Message 조회
     * @param id 조회할 메시지 ID
     * @return Message 엔티티 (없으면 null)
     */
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?
    
    /**
     * 모든 Message 조회
     * @return 모든 Message 엔티티 목록
     */
    @Query("SELECT * FROM messages ORDER BY created_at DESC")
    suspend fun getAll(): List<MessageEntity>
    
    /**
     * ID로 특정 Message 관찰
     * @param id 관찰할 메시지 ID
     * @return Message 엔티티 Flow
     */
    @Query("SELECT * FROM messages WHERE id = :id")
    fun observeById(id: String): Flow<MessageEntity?>
    
    /**
     * 모든 Message 관찰
     * @return 모든 Message 엔티티 Flow
     */
    @Query("SELECT * FROM messages ORDER BY created_at DESC")
    fun observeAll(): Flow<List<MessageEntity>>

    // ================================
    // 시간 범위별 조회 메서드 (페이징용)
    // ================================
    
    /**
     * 특정 시간 이후의 메시지들 조회 (페이징용)
     * @param afterTimestamp 기준 시간 (epoch milliseconds)
     * @param limit 조회할 개수 제한
     * @return 해당 시간 이후의 메시지 목록
     */
    @Query("""
        SELECT * FROM messages 
        WHERE created_at > :afterTimestamp
        AND is_deleted = 0
        ORDER BY created_at ASC 
        LIMIT :limit
    """)
    suspend fun getMessagesAfter(
        afterTimestamp: Long, 
        limit: Int = 50
    ): List<MessageEntity>
    
    /**
     * 특정 시간 이전의 메시지들 조회 (페이징용)
     * @param beforeTimestamp 기준 시간 (epoch milliseconds)
     * @param limit 조회할 개수 제한
     * @return 해당 시간 이전의 메시지 목록
     */
    @Query("""
        SELECT * FROM messages 
        WHERE created_at < :beforeTimestamp
        AND is_deleted = 0
        ORDER BY created_at DESC 
        LIMIT :limit
    """)
    suspend fun getMessagesBefore(
        beforeTimestamp: Long, 
        limit: Int = 50
    ): List<MessageEntity>
    
    /**
     * 특정 시간 범위의 메시지들 조회
     * @param startTimestamp 시작 시간 (epoch milliseconds)
     * @param endTimestamp 종료 시간 (epoch milliseconds)
     * @return 해당 시간 범위의 메시지 목록
     */
    @Query("""
        SELECT * FROM messages 
        WHERE created_at BETWEEN :startTimestamp AND :endTimestamp
        AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    suspend fun getMessagesBetween(startTimestamp: Long, endTimestamp: Long): List<MessageEntity>

    // ================================
    // 동기화 관련 메서드
    // ================================
    
    /**
     * 특정 동기화 상태의 메시지들 조회
     * @param syncStatus 동기화 상태
     * @return 해당 상태의 메시지 목록
     */
    @Query("SELECT * FROM messages WHERE sync_status = :syncStatus ORDER BY created_at DESC")
    suspend fun getMessagesBySyncStatus(syncStatus: String): List<MessageEntity>
    
    /**
     * 동기화가 필요한 메시지들 조회
     * @return 동기화가 필요한 메시지 목록
     */
    @Query("""
        SELECT * FROM messages 
        WHERE sync_status IN ('PENDING_CREATE', 'PENDING_UPDATE', 'PENDING_DELETE', 'ERROR')
        ORDER BY created_at ASC
    """)
    suspend fun getUnsyncedMessages(): List<MessageEntity>
    
    /**
     * 에러 상태의 메시지들 조회
     * @return 동기화 에러가 발생한 메시지 목록
     */
    @Query("SELECT * FROM messages WHERE sync_status = 'ERROR' ORDER BY updated_at DESC")
    suspend fun getErrorMessages(): List<MessageEntity>
    
    /**
     * 특정 서버 버전 이후의 메시지들 조회
     * @param version 기준 서버 버전
     * @return 해당 버전 이후의 메시지 목록
     */
    @Query("""
        SELECT * FROM messages 
        WHERE server_version > :version 
        ORDER BY server_version ASC
    """)
    suspend fun getMessagesAfterVersion(version: Long): List<MessageEntity>

    // ================================
    // 사용자별 조회 메서드
    // ================================
    
    /**
     * 특정 사용자가 보낸 메시지들 조회
     * @param senderId 발신자 ID
     * @return 해당 사용자의 메시지 목록
     */
    @Query("""
        SELECT * FROM messages 
        WHERE sender_id = :senderId 
        AND is_deleted = 0
        ORDER BY created_at DESC
    """)
    suspend fun getMessagesBySender(senderId: String): List<MessageEntity>
    
    /**
     * 답글 메시지들 조회
     * @param replyToMessageId 원본 메시지 ID
     * @return 해당 메시지에 대한 답글 목록
     */
    @Query("""
        SELECT * FROM messages 
        WHERE reply_to_message_id = :replyToMessageId 
        AND is_deleted = 0
        ORDER BY created_at ASC
    """)
    suspend fun getRepliesByMessageId(replyToMessageId: String): List<MessageEntity>

    // ================================
    // 삭제 및 정리 메서드
    // ================================
    
    /**
     * ID로 특정 Message 삭제
     * @param id 삭제할 메시지 ID
     * @return 삭제된 행의 개수
     */
    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String): Int
    
    
    /**
     * 오래된 메시지들 정리 (특정 시간 이전)
     * @param beforeTimestamp 기준 시간 이전 (epoch milliseconds)
     * @return 삭제된 행의 개수
     */
    @Query("DELETE FROM messages WHERE created_at < :beforeTimestamp")
    suspend fun deleteOldMessages(beforeTimestamp: Long): Int
    
    /**
     * 삭제 마크된 메시지들 완전 제거
     * @return 삭제된 행의 개수
     */
    @Query("DELETE FROM messages WHERE is_deleted = 1")
    suspend fun deleteMarkedMessages(): Int
    
    /**
     * 모든 Message 삭제 (테스트/초기화용)
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    // ================================
    // 통계 및 개수 조회 메서드
    // ================================
    
    /**
     * 전체 메시지 개수 조회
     * @return 전체 메시지 개수
     */
    @Query("SELECT COUNT(*) FROM messages WHERE is_deleted = 0")
    suspend fun getTotalCount(): Int
    
    
    /**
     * 동기화가 필요한 메시지 개수 조회
     * @return 미동기화 메시지 개수
     */
    @Query("""
        SELECT COUNT(*) FROM messages 
        WHERE sync_status IN ('PENDING_CREATE', 'PENDING_UPDATE', 'PENDING_DELETE', 'ERROR')
    """)
    suspend fun getUnsyncedCount(): Int

    // ================================
    // 메시지 존재 여부 확인
    // ================================
    
    /**
     * 메시지 ID 존재 여부 확인
     * @param id 확인할 메시지 ID
     * @return 존재 여부
     */
    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE id = :id)")
    suspend fun exists(id: String): Boolean

    // ================================
    // 양방향 페이징을 위한 추가 쿼리 메서드
    // ================================

    /**
     * 특정 메시지를 기준으로 이후(최신) 메시지들 조회
     * 중간 진입점에서 위쪽(최신) 방향으로 로딩할 때 사용
     * @param anchorTimestamp 기준 메시지의 타임스탬프
     * @param limit 조회할 개수 제한
     * @return 기준 이후의 메시지 목록 (시간 순 정렬)
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE created_at > :anchorTimestamp
        AND is_deleted = 0
        ORDER BY created_at ASC 
        LIMIT :limit
    """
    )
    suspend fun getMessagesAfterAnchor(
        anchorTimestamp: Long,
        limit: Int = 50
    ): List<MessageEntity>

    /**
     * 특정 메시지를 기준으로 이전(과거) 메시지들 조회
     * 중간 진입점에서 아래쪽(과거) 방향으로 로딩할 때 사용
     * @param anchorTimestamp 기준 메시지의 타임스탬프
     * @param limit 조회할 개수 제한
     * @return 기준 이전의 메시지 목록 (시간 역순 정렬)
     */
    @Query(
        """
        SELECT * FROM messages 
        WHERE created_at < :anchorTimestamp
        AND is_deleted = 0
        ORDER BY created_at DESC 
        LIMIT :limit
    """
    )
    suspend fun getMessagesBeforeAnchor(
        anchorTimestamp: Long,
        limit: Int = 50
    ): List<MessageEntity>

    /**
     * 특정 메시지 ID를 기준으로 주변 메시지들 조회
     * 중간 진입점 설정 시 사용 (기준점 + 앞뒤 컨텍스트)
     * @param anchorMessageId 기준 메시지 ID
     * @param beforeCount 기준 이전 메시지 개수
     * @param afterCount 기준 이후 메시지 개수
     * @return 기준 메시지 주변의 메시지 목록
     */
    @Query(
        """
        WITH anchor_message AS (
            SELECT created_at FROM messages WHERE id = :anchorMessageId
        ),
        before_messages AS (
            SELECT * FROM messages 
            WHERE created_at < (SELECT created_at FROM anchor_message)
            AND is_deleted = 0
            ORDER BY created_at DESC 
            LIMIT :beforeCount
        ),
        after_messages AS (
            SELECT * FROM messages 
            WHERE created_at >= (SELECT created_at FROM anchor_message)
            AND is_deleted = 0
            ORDER BY created_at ASC 
            LIMIT :afterCount
        )
        SELECT * FROM (
            SELECT * FROM before_messages
            UNION ALL
            SELECT * FROM after_messages
        ) ORDER BY created_at ASC
    """
    )
    suspend fun getMessagesAroundAnchor(
        anchorMessageId: String,
        beforeCount: Int = 25,
        afterCount: Int = 25
    ): List<MessageEntity>

    // ================================
    // 전송 상태 관리
    // ================================

    /**
     * 메시지의 전송 상태 업데이트
     * @param messageId 메시지 ID
     * @param deliveryStatus 새로운 전송 상태 (SENDING/SENT/FAILED)
     */
    @Query("UPDATE messages SET delivery_status = :deliveryStatus WHERE id = :messageId")
    suspend fun updateDeliveryStatus(messageId: String, deliveryStatus: String)
    
}