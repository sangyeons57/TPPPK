package com.example.domain.usecase.sync

import com.example.core_common.result.CustomResult
import com.example.data_core.dao.SyncMetadataDao
import com.example.data_model.local.SyncMetadataEntity
import com.example.domain.model.AggregateRoot
import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.local.base.SyncableRepository
import com.example.domain.repository.remote.DefaultRepository
import com.google.firebase.firestore.Query
import java.time.Instant
import javax.inject.Inject

/**
 * 제네릭 과거 데이터 서버 동기화 UseCase
 * 모든 도메인 엔티티에 대해 재사용 가능한 과거 데이터 동기화 로직
 *
 * 사용 사례:
 * - 초기 설치 후 과거 데이터 로드
 * - 네트워크 복구 후 누락된 과거 데이터 보완
 * - 페이지네이션을 통한 점진적 데이터 로드
 *
 * @param T 엔티티 타입 (Category, Project, User 등)
 */
class SyncOlderDataFromServerUseCase<T> @Inject constructor(
    private val syncMetadataDao: SyncMetadataDao
) where T : AggregateRoot {

    suspend operator fun invoke(
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>,
        batchSize: Long = 50L
    ): CustomResult<SyncResult, Exception> {
        return try {
            val collectionName = localRepository.collectionName

            // 1. SyncMetadata에서 커서 정보 확인
            val syncMetadata = syncMetadataDao.getSyncMetadata(collectionName)
                ?: SyncMetadataEntity(
                    collectionName = collectionName,
                    lastServerCursor = System.currentTimeMillis(),
                    lastSuccessfulSync = System.currentTimeMillis()
                )

            // 2. 서버에서 과거 데이터 조회 (오래된 것부터)
            remoteRepository.setCollection(CollectionPath.from(collectionName))

            // 커서가 있으면 해당 지점부터, 없으면 가장 오래된 것부터
            val serverResult = if (syncMetadata.lastServerCursor != null) {
                // 커서 기반 페이지네이션
                remoteRepository.findNAfterCursor(
                    n = batchSize,
                    cursor = syncMetadata.lastServerCursor!!,
                    direction = Query.Direction.ASCENDING // 오래된 것부터
                )
            } else {
                // 처음부터 조회
                remoteRepository.findNByUpdatedAt(
                    n = batchSize,
                    updatedAt = Instant.EPOCH, // 가장 오래된 시점부터
                    direction = Query.Direction.ASCENDING
                )
            }

            val entities = when (serverResult) {
                is CustomResult.Success -> serverResult.data
                is CustomResult.Failure -> return CustomResult.Failure(serverResult.error)
                else -> return CustomResult.Failure(Exception("Unexpected result state"))
            }

            // 더 이상 로드할 데이터가 없으면 조기 반환
            if (entities.isEmpty()) {
                return CustomResult.Success(
                    SyncResult(
                        totalCount = 0,
                        successCount = 0,
                        errorCount = 0,
                        errors = emptyList(),
                        hasMoreData = false,
                        nextCursor = null
                    )
                )
            }

            // 3. 로컬에 저장 (중복 체크)
            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<Exception>()

            entities.forEach { entity ->
                val entityId = getEntityId(entity)
                if (entityId != null) {
                    // 중복 체크
                    when (val existsResult = localRepository.entityExists(entityId)) {
                        is CustomResult.Success -> {
                            if (!existsResult.data) {
                                // 존재하지 않으면 저장
                                when (val saveResult = localRepository.saveEntity(entity)) {
                                    is CustomResult.Success -> successCount++
                                    is CustomResult.Failure -> {
                                        errorCount++
                                        errors.add(saveResult.error)
                                    }

                                    else -> {
                                        errorCount++
                                        errors.add(Exception("Unexpected save result state"))
                                    }
                                }
                            } else {
                                // 이미 존재하면 스킵 (중복)
                                successCount++
                            }
                        }

                        is CustomResult.Failure -> {
                            errorCount++
                            errors.add(existsResult.error)
                        }

                        else -> {
                            errorCount++
                            errors.add(Exception("Unexpected exists check result"))
                        }
                    }
                } else {
                    errorCount++
                    errors.add(Exception("Entity ID is null"))
                }
            }

            // 4. SyncMetadata 업데이트 (커서 정보)
            val lastEntity = entities.lastOrNull()
            val nextCursor = lastEntity?.let { getEntityId(it) }
            val hasMoreData = entities.size == batchSize.toInt()

            if (nextCursor != null) {
                val updatedMetadata = syncMetadata.copy(
                    lastServerCursor = (getEntityUpdatedAt(lastEntity)
                        ?: Instant.now()).toEpochMilli(),
                    lastSuccessfulSync = System.currentTimeMillis()
                )
                syncMetadataDao.insertSyncMetadata(updatedMetadata)
            }

            CustomResult.Success(
                SyncResult(
                    totalCount = entities.size,
                    successCount = successCount,
                    errorCount = errorCount,
                    errors = errors,
                    hasMoreData = hasMoreData,
                    nextCursor = nextCursor
                )
            )

        } catch (exception: Exception) {
            CustomResult.Failure(exception)
        }
    }

    /**
     * 리플렉션을 사용하여 엔티티의 ID 필드 접근
     */
    private fun getEntityId(entity: T): String? {
        return try {
            val field = entity!!::class.java.getDeclaredField("id")
            field.isAccessible = true
            val idValue = field.get(entity)
            // ValueObject 패턴의 ID 처리
            when {
                idValue?.javaClass?.simpleName?.endsWith("Id") == true -> {
                    val valueField = idValue.javaClass.getDeclaredField("value")
                    valueField.isAccessible = true
                    valueField.get(idValue) as? String
                }

                else -> idValue as? String
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 리플렉션을 사용하여 엔티티의 updatedAt 필드 접근
     */
    private fun getEntityUpdatedAt(entity: T): Instant? {
        return try {
            val field = entity!!::class.java.getDeclaredField("updatedAt")
            field.isAccessible = true
            field.get(entity) as? Instant
        } catch (e: Exception) {
            null
        }
    }

    data class SyncResult(
        val totalCount: Int,
        val successCount: Int,
        val errorCount: Int,
        val errors: List<Exception>,
        val hasMoreData: Boolean,
        val nextCursor: String?
    )
}