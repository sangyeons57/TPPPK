package com.example.data_datasource.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data_model.local.ScopeMetadataEntity
import com.example.domain.model.sync.ScopeMetadata

@Dao
interface ScopeMetadataDao {

    // ================================
    // 기본 CRUD 작업
    // ================================
    
    /**
     * ScopeMetadata 엔티티 삽입/업데이트
     * @param entity 저장할 ScopeMetadata 엔티티
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScopeMetadataEntity)
    
    /**
     * 여러 ScopeMetadata 엔티티들을 배치로 삽입/업데이트
     * @param entities 저장할 ScopeMetadata 엔티티 목록
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ScopeMetadataEntity>)
    
    /**
     * ScopeMetadata 엔티티 업데이트
     * @param entity 업데이트할 ScopeMetadata 엔티티
     */
    @Update
    suspend fun update(entity: ScopeMetadataEntity)
    
    /**
     * ScopeMetadata 엔티티 삭제
     * @param entity 삭제할 ScopeMetadata 엔티티
     */
    @Delete
    suspend fun delete(entity: ScopeMetadataEntity)

    // ================================
    // SyncManager용 조회 메서드
    // ================================
    
    /**
     * 키로 특정 ScopeMetadata 조회
     * @param key 조회할 메타데이터 키
     * @return ScopeMetadata 엔티티 (없으면 null)
     */
    @Query("""
        SELECT * FROM ${ScopeMetadata.TABLE_NAME} 
        WHERE ${ScopeMetadata.COLUMN_KEY} = :key
    """)
    suspend fun getByKey(key: String): ScopeMetadataEntity?
    
    /**
     * 모든 ScopeMetadata 조회
     * @return 모든 ScopeMetadata 엔티티 목록
     */
    @Query("SELECT * FROM ${ScopeMetadata.TABLE_NAME}")
    suspend fun getAll(): List<ScopeMetadataEntity>
    
    /**
     * 키 패턴으로 ScopeMetadata 조회 (컬렉션별 메타데이터 조회용)
     * @param keyPattern LIKE 패턴 (예: "messages_%")
     * @return 패턴에 매치되는 ScopeMetadata 목록
     */
    @Query("""
        SELECT * FROM ${ScopeMetadata.TABLE_NAME} 
        WHERE ${ScopeMetadata.COLUMN_KEY} LIKE :keyPattern
    """)
    suspend fun getByKeyPattern(keyPattern: String): List<ScopeMetadataEntity>
    
    /**
     * 특정 시간 이후에 동기화된 메타데이터들 조회
     * @param afterTimestamp 기준 시간 (epoch milliseconds)
     * @return 해당 시간 이후 동기화된 메타데이터 목록
     */
    @Query("""
        SELECT * FROM ${ScopeMetadata.TABLE_NAME} 
        WHERE ${ScopeMetadata.COLUMN_LAST_SYNCED_AT} > :afterTimestamp 
        ORDER BY ${ScopeMetadata.COLUMN_LAST_SYNCED_AT} DESC
    """)
    suspend fun getSyncedAfter(afterTimestamp: Long): List<ScopeMetadataEntity>

    // ================================
    // 키-값 관리 메서드
    // ================================
    
    /**
     * 키로 특정 ScopeMetadata 삭제
     * @param key 삭제할 메타데이터 키
     * @return 삭제된 행의 개수
     */
    @Query("""
        DELETE FROM ${ScopeMetadata.TABLE_NAME} 
        WHERE ${ScopeMetadata.COLUMN_KEY} = :key
    """)
    suspend fun deleteByKey(key: String): Int
    
    /**
     * 키 패턴으로 ScopeMetadata 삭제 (컬렉션별 메타데이터 정리용)
     * @param keyPattern LIKE 패턴 (예: "messages_%")
     * @return 삭제된 행의 개수
     */
    @Query("""
        DELETE FROM ${ScopeMetadata.TABLE_NAME} 
        WHERE ${ScopeMetadata.COLUMN_KEY} LIKE :keyPattern
    """)
    suspend fun deleteByKeyPattern(keyPattern: String): Int
    
    /**
     * 키의 존재 여부 확인
     * @param key 확인할 메타데이터 키
     * @return 존재 여부
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM ${ScopeMetadata.TABLE_NAME} 
            WHERE ${ScopeMetadata.COLUMN_KEY} = :key
        )
    """)
    suspend fun keyExists(key: String): Boolean

    // ================================
    // 동기화 상태 관리 메서드
    // ================================
    
    /**
     * 에러가 발생한 메타데이터들 조회 (에러 복구용)
     * @param minErrorCount 최소 에러 개수
     * @return 에러가 발생한 메타데이터 목록
     */
    @Query("""
        SELECT * FROM ${ScopeMetadata.TABLE_NAME} 
        WHERE ${ScopeMetadata.COLUMN_ERROR_COUNT} >= :minErrorCount 
        ORDER BY ${ScopeMetadata.COLUMN_ERROR_COUNT} DESC
    """)
    suspend fun getErrorMetadata(minErrorCount: Long = 1): List<ScopeMetadataEntity>
    
    /**
     * 최근 동기화 활동이 활발한 메타데이터들 조회
     * @param minSyncCount 최소 동기화 횟수
     * @param afterTimestamp 기준 시간 이후
     * @return 활발한 동기화 메타데이터 목록
     */
    @Query("""
        SELECT * FROM ${ScopeMetadata.TABLE_NAME} 
        WHERE ${ScopeMetadata.COLUMN_SYNC_COUNT} >= :minSyncCount 
        AND ${ScopeMetadata.COLUMN_LAST_SYNCED_AT} > :afterTimestamp 
        ORDER BY ${ScopeMetadata.COLUMN_SYNC_COUNT} DESC
    """)
    suspend fun getActiveSyncMetadata(minSyncCount: Long, afterTimestamp: Long): List<ScopeMetadataEntity>
    
    /**
     * 모든 메타데이터의 에러 카운트 초기화
     * @return 업데이트된 행의 개수
     */
    @Query("""
        UPDATE ${ScopeMetadata.TABLE_NAME} 
        SET ${ScopeMetadata.COLUMN_ERROR_COUNT} = 0, 
            ${ScopeMetadata.COLUMN_LAST_ERROR_MESSAGE} = NULL
    """)
    suspend fun resetAllErrorCounts(): Int

    // ================================
    // 관리용 메서드
    // ================================
    
    /**
     * 전체 메타데이터 개수 조회
     * @return 전체 메타데이터 개수
     */
    @Query("SELECT COUNT(*) FROM ${ScopeMetadata.TABLE_NAME}")
    suspend fun getTotalCount(): Int
    
    /**
     * 오래된 메타데이터 정리 (특정 시간 이전 동기화된 것들)
     * @param beforeTimestamp 기준 시간 이전 (epoch milliseconds)
     * @return 삭제된 행의 개수
     */
    @Query("""
        DELETE FROM ${ScopeMetadata.TABLE_NAME} 
        WHERE ${ScopeMetadata.COLUMN_LAST_SYNCED_AT} < :beforeTimestamp
    """)
    suspend fun deleteOldMetadata(beforeTimestamp: Long): Int
    
    /**
     * 모든 ScopeMetadata 삭제 (테스트/초기화용)
     */
    @Query("DELETE FROM ${ScopeMetadata.TABLE_NAME}")
    suspend fun deleteAll()

    // ================================
    // 디버깅 및 모니터링용 메서드
    // ================================
    
    /**
     * 모든 ScopeMetadata를 최근 동기화 순으로 조회 (디버깅용)
     * @return 최근 동기화 시간 내림차순으로 정렬된 모든 메타데이터 목록
     */
    @Query("""
        SELECT * FROM ${ScopeMetadata.TABLE_NAME} 
        ORDER BY ${ScopeMetadata.COLUMN_LAST_SYNCED_AT} DESC
    """)
    suspend fun getAllForDebug(): List<ScopeMetadataEntity>
    
    /**
     * 동기화 통계 조회 (총 동기화 횟수, 평균 에러 횟수 등)
     * @return 동기화 통계 정보
     */
    @Query("""
        SELECT 
            COUNT(*) as totalCount,
            SUM(${ScopeMetadata.COLUMN_SYNC_COUNT}) as totalSyncCount,
            AVG(${ScopeMetadata.COLUMN_SYNC_COUNT}) as avgSyncCount,
            SUM(${ScopeMetadata.COLUMN_ERROR_COUNT}) as totalErrorCount,
            AVG(${ScopeMetadata.COLUMN_ERROR_COUNT}) as avgErrorCount,
            MAX(${ScopeMetadata.COLUMN_LAST_SYNCED_AT}) as lastSyncTime
        FROM ${ScopeMetadata.TABLE_NAME}
    """)
    suspend fun getSyncStatistics(): ScopeMetadataSyncStat
}

/**
 * ScopeMetadata 동기화 통계 데이터 클래스
 */
data class ScopeMetadataSyncStat(
    val totalCount: Int,
    val totalSyncCount: Long,
    val avgSyncCount: Double,
    val totalErrorCount: Long,
    val avgErrorCount: Double,
    val lastSyncTime: Long?
)