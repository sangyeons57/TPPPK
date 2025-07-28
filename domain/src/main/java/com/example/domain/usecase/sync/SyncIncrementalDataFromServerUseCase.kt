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
 * 제네릭 증분 서버 동기화 UseCase
 * 모든 도메인 엔티티에 대해 재사용 가능한 증분 동기화 로직
 *
 * @param T 도메인 모델 타입 (Category, Project, User 등)
 */
class SyncIncrementalDataFromServerUseCase<T> @Inject constructor(
    private val syncMetadataDao: SyncMetadataDao
) where T : AggregateRoot {

    suspend operator fun invoke(
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>
    ): CustomResult<SyncResult, Exception> {
        return try {
            val collectionName = localRepository.collectionName

            // 1. SyncMetadata에서 마지막 동기화 시간 확인
            val syncMetadata = syncMetadataDao.getSyncMetadata(collectionName)
                ?: SyncMetadataEntity(
                    collectionName = collectionName,
                    lastServerCursor = 0L,
                    lastSuccessfulSync = System.currentTimeMillis() - 3600000L // 1시간 전부터
                )

            // 2. 서버에서 증분 데이터 조회 (마지막 동기화 이후)
            remoteRepository.setCollection(CollectionPath.from(collectionName))
            val serverResult = remoteRepository.findNByUpdatedAt(
                n = 1000L, // 증분 동기화는 큰 제한으로 설정
                updatedAt = Instant.ofEpochMilli(syncMetadata.lastServerCursor),
                direction = Query.Direction.DESCENDING
            )

            val entities = when (serverResult) {
                is CustomResult.Success -> serverResult.data.filter { entity ->
                    // 엔티티별로 updatedAt 접근 방식이 다를 수 있으므로 리플렉션 사용
                    getEntityUpdatedAt(entity)?.isAfter(Instant.ofEpochMilli(syncMetadata.lastServerCursor)) == true
                }

                is CustomResult.Failure -> return CustomResult.Failure(serverResult.error)
                else -> return CustomResult.Failure(Exception("Unexpected result state"))
            }

            // 변경사항이 없으면 조기 반환
            if (entities.isEmpty()) {
                return CustomResult.Success(
                    SyncResult(
                        totalCount = 0,
                        successCount = 0,
                        errorCount = 0,
                        errors = emptyList(),
                        isIncremental = true
                    )
                )
            }

            // 3. 로컬에 저장
            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<Exception>()

            entities.forEach { entity ->
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
            }

            // 4. SyncMetadata 업데이트 (가장 최근 업데이트 시간으로)
            val latestUpdateTime = entities.maxByOrNull { getEntityUpdatedAt(it) ?: Instant.MIN }
                ?.let { getEntityUpdatedAt(it) } ?: Instant.now()
            val updatedMetadata = syncMetadata.copy(
                lastServerCursor = latestUpdateTime.toEpochMilli(),
                lastSuccessfulSync = System.currentTimeMillis()
            )
            syncMetadataDao.insertSyncMetadata(updatedMetadata)

            CustomResult.Success(
                SyncResult(
                    totalCount = entities.size,
                    successCount = successCount,
                    errorCount = errorCount,
                    errors = errors,
                    isIncremental = true
                )
            )

        } catch (exception: Exception) {
            CustomResult.Failure(exception)
        }
    }

    /**
     * 리플렉션을 사용하여 엔티티의 updatedAt 필드 접근
     * TODO: 성능 개선을 위해 sealed class나 interface 방식으로 변경 고려
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

    /**
     * 리플렉션을 사용하여 엔티티의 ID 필드 접근
     * TODO: 성능 개선을 위해 sealed class나 interface 방식으로 변경 고려
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

    data class SyncResult(
        val totalCount: Int,
        val successCount: Int,
        val errorCount: Int,
        val errors: List<Exception>,
        val isIncremental: Boolean = false
    )
}