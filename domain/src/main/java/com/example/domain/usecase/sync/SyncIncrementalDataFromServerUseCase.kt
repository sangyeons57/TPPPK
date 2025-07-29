package com.example.domain.usecase.sync

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.repository.local.base.SyncableRepository
import com.example.domain.repository.remote.DefaultRepository
import java.time.Instant
import javax.inject.Inject

/**
 * 제네릭 증분 서버 동기화 UseCase
 * 모든 도메인 엔티티에 대해 재사용 가능한 증분 동기화 로직
 *
 * @param T 도메인 모델 타입 (Category, Project, User 등)
 */
class SyncIncrementalDataFromServerUseCase<T> @Inject constructor(
    // TODO: Replace with domain repository interface when data layer is integrated
    // private val syncMetadataRepository: SyncMetadataRepository
) where T : AggregateRoot {

    suspend operator fun invoke(
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>
    ): CustomResult<SyncResult, Exception> {
        // TODO: Implement incremental data synchronization from server
        // This UseCase should:
        // 1. Retrieve last sync metadata to determine the cursor/timestamp for incremental sync
        // 2. Query remote repository for entities updated after the last sync timestamp
        // 3. Filter entities to only include those newer than the last sync
        // 4. Save new/updated entities to local repository
        // 5. Update sync metadata with the latest sync timestamp
        // 6. Return sync results with counts and any errors encountered
        //
        // Key features:
        // - Uses timestamps/cursors to sync only changed data since last sync
        // - Filters out already-synced entities to avoid duplicates
        // - Updates sync metadata to track progress
        // - Handles errors gracefully and reports detailed results

        return CustomResult.Failure(Exception("UseCase implementation pending - data layer integration required"))
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