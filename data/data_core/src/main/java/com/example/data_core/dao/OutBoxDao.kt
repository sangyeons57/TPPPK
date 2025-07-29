package com.example.data_core.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.OutBoxEntity
import com.example.domain.model.sync.OutBox

@Dao
interface OutBoxDao {

    // ================================
    // 기본 CRUD 작업
    // ================================
    
    /**
     * OutBox 엔티티 삽입/업데이트
     * @param outBoxEntity 저장할 OutBox 엔티티
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outBoxEntity: OutBoxEntity)
    
    /**
     * 여러 OutBox 엔티티들을 배치로 삽입/업데이트
     * @param outBoxEntities 저장할 OutBox 엔티티 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE) 
    suspend fun insertAll(outBoxEntities: List<OutBoxEntity>)
    
    /**
     * OutBox 엔티티 업데이트
     * @param outBoxEntity 업데이트할 OutBox 엔티티
     */
    @Update
    suspend fun update(outBoxEntity: OutBoxEntity)
    
    /**
     * OutBox 엔티티 삭제
     * @param outBoxEntity 삭제할 OutBox 엔티티
     */
    @Delete
    suspend fun delete(outBoxEntity: OutBoxEntity)

    // ================================
    // SyncManager용 조회 메서드
    // ================================
    
    /**
     * 특정 상태의 OutBox 작업들을 우선순위/생성시간 순으로 조회
     * @param status 조회할 상태
     * @return 우선순위 오름차순, 생성시간 오름차순으로 정렬된 OutBox 목록
     */
    @Query("""
        SELECT * FROM ${OutBox.TABLE_NAME} 
        WHERE ${OutBox.COLUMN_STATUS} = :status 
        ORDER BY ${OutBox.COLUMN_PRIORITY} ASC, ${OutBox.COLUMN_CREATED_AT} ASC
    """)
    suspend fun getByStatus(status: String): List<OutBoxEntity>
    
    /**
     * 여러 상태의 OutBox 작업들을 우선순위 순으로 조회
     * @param statuses 조회할 상태 목록
     * @return 우선순위 오름차순으로 정렬된 OutBox 목록
     */
    @Query("""
        SELECT * FROM ${OutBox.TABLE_NAME} 
        WHERE ${OutBox.COLUMN_STATUS} IN (:statuses) 
        ORDER BY ${OutBox.COLUMN_PRIORITY} ASC, ${OutBox.COLUMN_CREATED_AT} ASC
    """)
    suspend fun getByStatuses(statuses: List<String>): List<OutBoxEntity>
    
    /**
     * ID로 특정 OutBox 작업 조회
     * @param id OutBox ID
     * @return OutBox 엔티티 (없으면 null)
     */
    @Query("""
        SELECT * FROM ${OutBox.TABLE_NAME} 
        WHERE ${OutBox.COLUMN_ID} = :id
    """)
    suspend fun getById(id: String): OutBoxEntity?
    
    /**
     * 특정 엔티티 타입과 엔티티 ID로 OutBox 작업 조회
     * @param entityType 엔티티 타입
     * @param entityId 엔티티 ID
     * @return 해당 엔티티의 OutBox 목록
     */
    @Query("""
        SELECT * FROM ${OutBox.TABLE_NAME} 
        WHERE ${OutBox.COLUMN_ENTITY_TYPE} = :entityType 
        AND ${OutBox.COLUMN_ENTITY_ID} = :entityId 
        ORDER BY ${OutBox.COLUMN_CREATED_AT} DESC
    """)
    suspend fun getByEntityTypeAndId(entityType: String, entityId: String): List<OutBoxEntity>

    // ================================
    // 배치 처리용 메서드
    // ================================
    
    /**
     * 대기 중인 OutBox 작업들을 제한된 개수만큼 조회 (SyncManager 배치 처리용)
     * @param limit 조회할 최대 개수
     * @return 우선순위 오름차순으로 정렬된 대기 중인 OutBox 목록
     */
    @Query("""
        SELECT * FROM ${OutBox.TABLE_NAME} 
        WHERE ${OutBox.COLUMN_STATUS} = 'PENDING' 
        ORDER BY ${OutBox.COLUMN_PRIORITY} ASC, ${OutBox.COLUMN_CREATED_AT} ASC 
        LIMIT :limit
    """)
    suspend fun getPendingOperations(limit: Int): List<OutBoxEntity>
    
    /**
     * 여러 OutBox 작업들의 상태를 일괄 업데이트
     * @param ids 업데이트할 OutBox ID 목록
     * @param newStatus 새로운 상태
     * @return 업데이트된 행의 개수
     */
    @Query("""
        UPDATE ${OutBox.TABLE_NAME} 
        SET ${OutBox.COLUMN_STATUS} = :newStatus 
        WHERE ${OutBox.COLUMN_ID} IN (:ids)
    """)
    suspend fun updateStatusByIds(ids: List<String>, newStatus: String): Int
    
    /**
     * 실패한 작업들 중 재시도 가능한 것들을 PENDING 상태로 복원
     * @return 복원된 작업 개수
     */
    @Query("""
        UPDATE ${OutBox.TABLE_NAME} 
        SET ${OutBox.COLUMN_STATUS} = 'PENDING' 
        WHERE ${OutBox.COLUMN_STATUS} = 'FAILED' 
        AND ${OutBox.COLUMN_ATTEMPTS} < ${OutBox.COLUMN_MAX_RETRIES}
    """)
    suspend fun resetRetryableFailedOperations(): Int

    // ================================
    // 관리용 메서드  
    // ================================
    
    /**
     * 완료된 OutBox 작업들을 삭제 (정리용)
     * @return 삭제된 작업 개수
     */
    @Query("""
        DELETE FROM ${OutBox.TABLE_NAME} 
        WHERE ${OutBox.COLUMN_STATUS} = 'COMPLETED'
    """)
    suspend fun deleteCompleted(): Int
    
    /**
     * 특정 상태의 OutBox 작업 개수 조회
     * @param status 조회할 상태
     * @return 해당 상태의 작업 개수
     */
    @Query("""
        SELECT COUNT(*) FROM ${OutBox.TABLE_NAME} 
        WHERE ${OutBox.COLUMN_STATUS} = :status
    """)
    suspend fun getCountByStatus(status: String): Int
    
    /**
     * 전체 OutBox 작업 개수 조회
     * @return 전체 작업 개수
     */
    @Query("SELECT COUNT(*) FROM ${OutBox.TABLE_NAME}")
    suspend fun getTotalCount(): Int
    
    /**
     * 만료된 OutBox 작업들을 삭제
     * @param currentTimeMs 현재 시간 (epoch milliseconds)
     * @param timeoutMs 타임아웃 시간 (milliseconds)
     * @return 삭제된 작업 개수
     */
    @Query("""
        DELETE FROM ${OutBox.TABLE_NAME} 
        WHERE (:currentTimeMs - ${OutBox.COLUMN_CREATED_AT}) > :timeoutMs
        AND ${OutBox.COLUMN_STATUS} IN ('FAILED', 'COMPLETED')
    """)
    suspend fun deleteExpiredOperations(currentTimeMs: Long, timeoutMs: Long): Int
    
    /**
     * 모든 OutBox 작업 삭제 (테스트/초기화용)
     */
    @Query("DELETE FROM ${OutBox.TABLE_NAME}")
    suspend fun deleteAll()
    
    // ================================
    // 디버깅 및 모니터링용 메서드
    // ================================
    
    /**
     * 모든 OutBox 작업을 생성시간 순으로 조회 (디버깅용)
     * @return 생성시간 내림차순으로 정렬된 모든 OutBox 목록
     */
    @Query("""
        SELECT * FROM ${OutBox.TABLE_NAME} 
        ORDER BY ${OutBox.COLUMN_CREATED_AT} DESC
    """)
    suspend fun getAllForDebug(): List<OutBoxEntity>
    
    /**
     * 상태별 OutBox 작업 통계 조회
     * @return 상태별 작업 개수 통계
     */
    @Query("""
        SELECT ${OutBox.COLUMN_STATUS} as status, COUNT(*) as count 
        FROM ${OutBox.TABLE_NAME} 
        GROUP BY ${OutBox.COLUMN_STATUS}
    """)
    suspend fun getStatusStatistics(): List<OutBoxStatusStat>
}

/**
 * OutBox 상태별 통계 데이터 클래스
 */
data class OutBoxStatusStat(
    val status: String,
    val count: Int
)