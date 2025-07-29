package com.example.data_core.datasource.local

import android.util.Log
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_model.local.SyncMetadataEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync Metadata Data Source Implementation
 * SyncMetadataDao를 통한 동기화 메타데이터 관리
 *
 * 🎯 역할:
 * - SyncMetadataDao 래핑 및 추상화
 * - 비즈니스 로직 처리 (기본값 설정, 검증)
 * - 에러 처리 및 로깅
 * - Repository 계층과 DAO 계층 분리
 *
 * 📋 구현 특징:
 * - 모든 DAO 호출을 래핑하여 일관된 인터페이스 제공
 * - 동기화 메타데이터 생성시 필요한 검증 로직 추가
 * - 예외 처리 및 로깅으로 디버깅 지원
 */
@Singleton
class SyncMetadataDataSourceImpl @Inject constructor(
    private val syncMetadataDao: SyncMetadataDao
) : SyncMetadataDataSource {

    companion object {
        private const val TAG = "SyncMetadataDataSource"
    }

    override suspend fun getSyncMetadata(collectionName: String): SyncMetadataEntity? {
        return try {
            Log.d(TAG, "Getting sync metadata for collection: $collectionName")
            val metadata = syncMetadataDao.getSyncMetadata(collectionName)
            Log.d(TAG, "Found sync metadata for $collectionName: ${metadata != null}")
            metadata

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get sync metadata for collection: $collectionName", e)
            throw e
        }
    }

    override suspend fun getAllSyncMetadata(): List<SyncMetadataEntity> {
        return try {
            Log.d(TAG, "Getting all sync metadata")
            val metadataList = syncMetadataDao.getAllSyncMetadata()
            Log.d(TAG, "Found ${metadataList.size} sync metadata entries")
            metadataList

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all sync metadata", e)
            throw e
        }
    }

    override suspend fun saveSyncMetadata(metadata: SyncMetadataEntity) {
        try {
            Log.d(TAG, "Saving sync metadata for collection: ${metadata.collectionName}")

            // 검증
            require(metadata.collectionName.isNotBlank()) {
                "Collection name cannot be blank"
            }
            require(metadata.lastServerCursor >= 0) {
                "Last server cursor must be non-negative: ${metadata.lastServerCursor}"
            }
            require(metadata.lastSuccessfulSync >= 0) {
                "Last successful sync must be non-negative: ${metadata.lastSuccessfulSync}"
            }

            syncMetadataDao.insertSyncMetadata(metadata)
            Log.d(TAG, "Successfully saved sync metadata for: ${metadata.collectionName}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save sync metadata for collection: ${metadata.collectionName}", e)
            throw e
        }
    }

    override suspend fun saveMultipleSyncMetadata(metadataList: List<SyncMetadataEntity>) {
        try {
            Log.d(TAG, "Saving ${metadataList.size} sync metadata entries")

            // 모든 항목 검증
            metadataList.forEach { metadata ->
                require(metadata.collectionName.isNotBlank()) {
                    "Collection name cannot be blank in batch save"
                }
                require(metadata.lastServerCursor >= 0) {
                    "Last server cursor must be non-negative: ${metadata.lastServerCursor}"
                }
                require(metadata.lastSuccessfulSync >= 0) {
                    "Last successful sync must be non-negative: ${metadata.lastSuccessfulSync}"
                }
            }

            syncMetadataDao.insertSyncMetadata(metadataList)
            Log.d(TAG, "Successfully saved ${metadataList.size} sync metadata entries")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save ${metadataList.size} sync metadata entries", e)
            throw e
        }
    }

    override suspend fun deleteSyncMetadata(collectionName: String) {
        try {
            Log.d(TAG, "Deleting sync metadata for collection: $collectionName")
            syncMetadataDao.deleteSyncMetadata(collectionName)
            Log.d(TAG, "Successfully deleted sync metadata for: $collectionName")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete sync metadata for collection: $collectionName", e)
            throw e
        }
    }

    override suspend fun deleteAllSyncMetadata() {
        try {
            Log.d(TAG, "Deleting all sync metadata")
            syncMetadataDao.deleteAllSyncMetadata()
            Log.d(TAG, "Successfully deleted all sync metadata")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all sync metadata", e)
            throw e
        }
    }
}