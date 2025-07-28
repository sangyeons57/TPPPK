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
 * 제네릭 최신 데이터 서버 동기화 UseCase
 * 모든 도메인 엔티티에 대해 재사용 가능한 최신 데이터 동기화 로직
 *
 * 사용 사례:
 * - 앱 시작 시 최신 데이터 확인
 * - Pull-to-refresh 동작
 * - 실시간 동기화 보완
 * - 중요한 데이터의 최신 상태 보장
 *
 * @param T 엔티티 타입 (Category, Project, User 등)
 */
class SyncLatestDataFromServerUseCase<T> @Inject constructor(
    private val syncMetadataDao: SyncMetadataDao
) where T : AggregateRoot {

    suspend operator fun invoke(
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>,
        batchSize: Long = 100L
    ): CustomResult<SyncResult, Exception> {
        return try {
            val collectionName = localRepository.collectionName

            // 1. 서버에서 최신 데이터 조회 (최신 것부터)
            remoteRepository.setCollection(CollectionPath.from(collectionName))
            val serverResult = remoteRepository.findNByUpdatedAt(
                n = batchSize,
                updatedAt = Instant.EPOCH, // 전체 범위에서
                direction = Query.Direction.DESCENDING // 최신 것부터
            )

            val entities = when (serverResult) {
                is CustomResult.Success -> serverResult.data
                is CustomResult.Failure -> return CustomResult.Failure(serverResult.error)
                else -> return CustomResult.Failure(Exception("Unexpected result state"))
            }

            if (entities.isEmpty()) {
                return CustomResult.Success(
                    SyncResult(
                        totalCount = 0,
                        successCount = 0,
                        errorCount = 0,
                        errors = emptyList(),
                        latestTimestamp = null
                    )
                )
            }

            // 2. 로컬 데이터와 비교하여 업데이트 필요한 것만 처리
            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<Exception>()
            val updatedEntities = mutableListOf<T>()

            entities.forEach { serverEntity ->
                val entityId = getEntityId(serverEntity)
                if (entityId != null) {
                    // 로컬 엔티티와 비교
                    when (val localResult = localRepository.getEntityById(entityId)) {
                        is CustomResult.Success -> {
                            val localEntity = localResult.data
                            val shouldUpdate = if (localEntity == null) {
                                // 로컬에 없으면 무조건 추가
                                true
                            } else {
                                // 서버가 더 최신인지 확인
                                val serverUpdatedAt = getEntityUpdatedAt(serverEntity)
                                val localUpdatedAt = getEntityUpdatedAt(localEntity)

                                when {
                                    serverUpdatedAt == null -> false // 서버 타임스탬프 없으면 스킵
                                    localUpdatedAt == null -> true // 로컬 타임스탬프 없으면 서버 우선
                                    serverUpdatedAt.isAfter(localUpdatedAt) -> true // 서버가 더 최신
                                    else -> false // 로컬이 같거나 더 최신
                                }
                            }

                            if (shouldUpdate) {
                                when (val saveResult = localRepository.saveEntity(serverEntity)) {
                                    is CustomResult.Success -> {
                                        successCount++
                                        updatedEntities.add(serverEntity)
                                    }

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
                                // 업데이트가 필요하지 않음 (이미 최신)
                                successCount++
                            }
                        }

                        is CustomResult.Failure -> {
                            errorCount++
                            errors.add(localResult.error)
                        }

                        else -> {
                            errorCount++
                            errors.add(Exception("Unexpected local result state"))
                        }
                    }
                } else {
                    errorCount++
                    errors.add(Exception("Entity ID is null"))
                }
            }

            // 3. SyncMetadata 업데이트 (최신 동기화 시간 기록)
            val latestEntity = entities.firstOrNull() // 이미 최신 순으로 정렬됨
            val latestTimestamp = latestEntity?.let { getEntityUpdatedAt(it) } ?: Instant.now()

            val syncMetadata = syncMetadataDao.getSyncMetadata(collectionName)
                ?: SyncMetadataEntity(
                    collectionName = collectionName,
                    lastServerCursor = 0L,
                    lastSuccessfulSync = 0L
                )

            val updatedMetadata = syncMetadata.copy(
                lastServerCursor = latestTimestamp.toEpochMilli(),
                lastSuccessfulSync = System.currentTimeMillis()
            )
            syncMetadataDao.insertSyncMetadata(updatedMetadata)

            CustomResult.Success(
                SyncResult(
                    totalCount = entities.size,
                    successCount = successCount,
                    errorCount = errorCount,
                    errors = errors,
                    latestTimestamp = latestTimestamp
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
        val latestTimestamp: Instant?
    )
}