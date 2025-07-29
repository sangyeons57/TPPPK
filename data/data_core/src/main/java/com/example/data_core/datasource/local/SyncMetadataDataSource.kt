package com.example.data_core.datasource.local

import com.example.data_model.local.SyncMetadataEntity

/**
 * Sync Metadata Data Source Interface
 * 동기화 메타데이터 관리를 위한 데이터 소스 인터페이스
 *
 * 🎯 역할:
 * - 동기화 커서 및 메타데이터 관리
 * - 증분 동기화를 위한 타임스탬프 추적
 * - Repository와 DAO 사이의 추상화 계층
 *
 * 📋 주요 기능:
 * - getLastSyncCursor: 마지막 동기화 커서 조회
 * - updateSyncCursor: 동기화 커서 업데이트
 * - getSyncMetadata: 컬렉션별 동기화 메타데이터 조회
 */
interface SyncMetadataDataSource {

    /**
     * 특정 컬렉션의 동기화 메타데이터 조회
     * @param collectionName 컬렉션 이름
     * @return 동기화 메타데이터 (없으면 null)
     */
    suspend fun getSyncMetadata(collectionName: String): SyncMetadataEntity?

    /**
     * 모든 컬렉션의 동기화 메타데이터 조회
     * @return 전체 동기화 메타데이터 목록
     */
    suspend fun getAllSyncMetadata(): List<SyncMetadataEntity>

    /**
     * 동기화 메타데이터 저장/업데이트
     * @param metadata 저장할 동기화 메타데이터
     */
    suspend fun saveSyncMetadata(metadata: SyncMetadataEntity)

    /**
     * 여러 동기화 메타데이터 일괄 저장
     * @param metadataList 저장할 메타데이터 목록
     */
    suspend fun saveMultipleSyncMetadata(metadataList: List<SyncMetadataEntity>)

    /**
     * 마지막 동기화 커서 조회
     * @param collectionName 컬렉션 이름
     * @return 마지막 서버 커서 (없으면 null)
     */
    suspend fun getLastSyncCursor(collectionName: String): Long? {
        return getSyncMetadata(collectionName)?.lastServerCursor
    }

    /**
     * 동기화 커서 업데이트
     * @param collectionName 컬렉션 이름
     * @param cursor 새로운 서버 커서
     * @param timestamp 동기화 완료 시간
     */
    suspend fun updateSyncCursor(collectionName: String, cursor: Long, timestamp: Long) {
        val metadata = SyncMetadataEntity(
            collectionName = collectionName,
            lastServerCursor = cursor,
            lastSuccessfulSync = timestamp
        )
        saveSyncMetadata(metadata)
    }

    /**
     * 마지막 성공 동기화 시간 조회
     * @param collectionName 컬렉션 이름
     * @return 마지막 성공 동기화 시간 (없으면 null)
     */
    suspend fun getLastSuccessfulSyncTime(collectionName: String): Long? {
        return getSyncMetadata(collectionName)?.lastSuccessfulSync
    }

    /**
     * 특정 컬렉션의 동기화 메타데이터 삭제
     * @param collectionName 컬렉션 이름
     */
    suspend fun deleteSyncMetadata(collectionName: String)

    /**
     * 모든 동기화 메타데이터 삭제 (전체 초기화)
     */
    suspend fun deleteAllSyncMetadata()
}