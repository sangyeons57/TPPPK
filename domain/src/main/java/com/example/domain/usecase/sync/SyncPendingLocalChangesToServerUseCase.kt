package com.example.domain.usecase.sync

import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.repository.local.base.SyncableRepository
import com.example.domain.repository.remote.DefaultRepository
import javax.inject.Inject

/**
 * 제네릭 로컬 변경사항 서버 동기화 UseCase
 * 모든 도메인 엔티티에 대해 재사용 가능한 범용 동기화 로직
 *
 * @param T 도메인 모델 타입 (Category, Project, User 등)
 */
class SyncPendingLocalChangesToServerUseCase<T> @Inject constructor(
    // TODO: Replace with domain repository interface when data layer is integrated
    // private val outboxRepository: OutboxRepository
) where T : AggregateRoot {

    suspend operator fun invoke(
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>
    ): CustomResult<SyncResult, Exception> {
        // TODO: Implement pending local changes synchronization to server
        // This UseCase should:
        // 1. Query outbox repository for pending operations (CREATE, UPDATE, DELETE) for the collection
        // 2. Process each outbox entry by operation type:
        //    - CREATE: Get entity from local repository and create on server
        //    - UPDATE: Get entity from local repository and update on server
        //    - DELETE: Delete entity from server using document ID
        // 3. Track success/failure for each operation
        // 4. Remove successfully processed outbox entries
        // 5. Return sync results with operation counts and any errors
        //
        // Key features:
        // - Outbox pattern: ensures reliable sync of local changes to server
        // - Operation-specific handling (CREATE/UPDATE/DELETE)
        // - Transactional: only remove outbox entries after successful server sync
        // - Error resilience: failed operations remain in outbox for retry

        return CustomResult.Failure(Exception("UseCase implementation pending - data layer integration required"))
    }


    data class SyncResult(
        val totalCount: Int,
        val successCount: Int,
        val errorCount: Int,
        val errors: List<Exception>
    )
}