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
 * 제네릭 전체 데이터 서버 동기화 UseCase
 * 모든 도메인 엔티티에 대해 재사용 가능한 전체 동기화 로직
 *
 * 사용 사례:
 * - 초기 설치 후 전체 데이터 다운로드
 * - 데이터 불일치 해결을 위한 전체 재동기화
 * - 오프라인 모드 준비를 위한 전체 데이터 캐싱
 * - 데이터베이스 초기화 후 복구
 *
 * 주의사항:
 * - 대용량 데이터셋에서는 메모리 사용량과 성능에 주의
 * - 배치 처리를 통한 점진적 동기화 권장
 *
 * @param T 엔티티 타입 (Category, Project, User 등)
 */
class SyncAllDataFromServerUseCase<T> @Inject constructor(
    private val syncMetadataDao: SyncMetadataDao
) where T : AggregateRoot {

    suspend operator fun invoke(
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>,
        batchSize: Long = 200L,
        clearLocalFirst: Boolean = false
    ): CustomResult<SyncResult, Exception> {
        return try {
            val collectionName = localRepository.collectionName

            // 1. 로컬 데이터 초기화 (선택적)
            if (clearLocalFirst) {
                when (val clearResult = localRepository.clearAllEntities()) {
                    is CustomResult.Failure -> return CustomResult.Failure(clearResult.error)
                    else -> { /* 성공 또는 계속 진행 */
                    }
                }
            }

            // 2. 서버에서 전체 데이터 조회 (배치 처리)
            remoteRepository.setCollection(CollectionPath.from(collectionName))

            var totalProcessed = 0
            var totalSuccess = 0
            var totalErrors = 0
            val allErrors = mutableListOf<Exception>()
            var cursor: String? = null
            var hasMoreData = true

            while (hasMoreData) {
                // 배치별로 데이터 조회
                val serverResult = if (cursor != null) {
                    remoteRepository.findNAfterCursor(
                        n = batchSize,
                        cursor = cursor,
                        direction = Query.Direction.ASCENDING
                    )
                } else {
                    remoteRepository.findNByUpdatedAt(
                        n = batchSize,
                        updatedAt = Instant.EPOCH,
                        direction = Query.Direction.ASCENDING
                    )
                }

                val entities = when (serverResult) {
                    is CustomResult.Success -> serverResult.data
                    is CustomResult.Failure -> return CustomResult.Failure(serverResult.error)
                    else -> return CustomResult.Failure(Exception("Unexpected result state"))
                }

                // 더 이상 데이터가 없으면 종료
                if (entities.isEmpty()) {
                    hasMoreData = false
                    break
                }

                // 3. 배치 데이터를 로컬에 저장
                var batchSuccess = 0
                var batchErrors = 0

                entities.forEach { entity ->
                    when (val saveResult = localRepository.saveEntity(entity)) {
                        is CustomResult.Success -> batchSuccess++
                        is CustomResult.Failure -> {
                            batchErrors++
                            allErrors.add(saveResult.error)
                        }

                        else -> {
                            batchErrors++
                            allErrors.add(Exception("Unexpected save result state"))
                        }
                    }
                }

                totalProcessed += entities.size
                totalSuccess += batchSuccess
                totalErrors += batchErrors

                // 4. 다음 배치를 위한 커서 업데이트
                val lastEntity = entities.lastOrNull()
                cursor = lastEntity?.let { getEntityId(it) }
                hasMoreData = entities.size == batchSize.toInt()

                // 5. 중간 진행상황 로깅 (옵션)
                if (totalProcessed % (batchSize * 5) == 0L) {
                    // 큰 배치마다 로그 출력 (디버깅용)
                    println("Sync progress: $totalProcessed processed, $totalSuccess succeeded, $totalErrors failed")
                }
            }

            // 6. SyncMetadata 업데이트 (전체 동기화 완료 기록)
            val syncMetadata = SyncMetadataEntity(
                collectionName = collectionName,
                lastServerCursor = System.currentTimeMillis(),
                lastSuccessfulSync = System.currentTimeMillis()
            )
            syncMetadataDao.insertSyncMetadata(syncMetadata)

            // 7. 최종 통계 확인
            val localCountResult = localRepository.getTotalEntityCount()
            val finalLocalCount = when (localCountResult) {
                is CustomResult.Success -> localCountResult.data
                else -> -1 // 카운트 실패
            }

            CustomResult.Success(
                SyncResult(
                    totalCount = totalProcessed,
                    successCount = totalSuccess,
                    errorCount = totalErrors,
                    errors = allErrors,
                    finalLocalCount = finalLocalCount,
                    fullSyncCompleted = totalErrors == 0
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

    data class SyncResult(
        val totalCount: Int,
        val successCount: Int,
        val errorCount: Int,
        val errors: List<Exception>,
        val finalLocalCount: Int,
        val fullSyncCompleted: Boolean
    )
}