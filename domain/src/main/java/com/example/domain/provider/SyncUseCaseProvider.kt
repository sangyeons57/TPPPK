package com.example.domain.provider

import com.example.domain.model.AggregateRoot
import com.example.domain.repository.local.base.SyncableRepository
import com.example.domain.repository.remote.DefaultRepository
import com.example.domain.usecase.sync.ResolveDataConflictUseCase
import com.example.domain.usecase.sync.SyncAllDataFromServerUseCase
import com.example.domain.usecase.sync.SyncIncrementalDataFromServerUseCase
import com.example.domain.usecase.sync.SyncLatestDataFromServerUseCase
import com.example.domain.usecase.sync.SyncOlderDataFromServerUseCase
import com.example.domain.usecase.sync.SyncPendingLocalChangesToServerUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync UseCase Provider
 * 모든 도메인 엔티티에 대해 재사용 가능한 동기화 UseCase들을 제공
 *
 * 72개의 개별 UseCase를 6개의 제네릭 UseCase로 통합
 * - SyncPendingLocalChangesToServerUseCase
 * - SyncIncrementalDataFromServerUseCase
 * - ResolveDataConflictUseCase
 * - SyncOlderDataFromServerUseCase
 * - SyncLatestDataFromServerUseCase
 * - SyncAllDataFromServerUseCase
 *
 * 사용 예시:
 * ```kotlin
 * @HiltViewModel
 * class CategorySyncViewModel @Inject constructor(
 *     private val syncUseCaseProvider: SyncUseCaseProvider,
 *     private val remoteCategoryRepository: DefaultRepository<Category>,
 *     private val localCategoryRepository: LocalCategoryRepository
 * ) : ViewModel() {
 *
 *     fun syncCategories() {
 *         val syncUseCases = syncUseCaseProvider.create<Category>()
 *
 *         viewModelScope.launch {
 *             // 증분 동기화
 *             syncUseCases.syncIncremental(remoteCategoryRepository, localCategoryRepository)
 *
 *             // 로컬 변경사항 업로드
 *             syncUseCases.syncPending(remoteCategoryRepository, localCategoryRepository)
 *         }
 *     }
 * }
 * ```
 */
@Singleton
class SyncUseCaseProvider @Inject constructor(
    // TODO: Replace with domain repository interfaces when data layer is integrated
    // private val outboxRepository: OutboxRepository,
    // private val syncMetadataRepository: SyncMetadataRepository
) {

    /**
     * 특정 도메인 타입을 위한 Sync UseCase 그룹 생성
     *
     * @param T 도메인 모델 타입 (Category, Project, User 등)
     * @return 해당 타입에 특화된 Sync UseCase 그룹
     */
    fun <T> create(): SyncUseCases<T> where T : AggregateRoot {
        // TODO: Implement when data layer is integrated
        // Currently returns placeholder implementations that will fail at runtime
        return SyncUseCases(
            syncPending = SyncPendingLocalChangesToServerUseCase<T>(),
            syncIncremental = SyncIncrementalDataFromServerUseCase<T>(),
            resolveConflict = ResolveDataConflictUseCase<T>(),
            syncOlder = SyncOlderDataFromServerUseCase<T>(),
            syncLatest = SyncLatestDataFromServerUseCase<T>(),
            syncAll = SyncAllDataFromServerUseCase<T>()
        )
    }
}

/**
 * 특정 도메인 타입을 위한 Sync UseCase 그룹
 * 타입 안전성을 보장하면서 편리한 API 제공
 *
 * @param T 도메인 모델 타입
 */
data class SyncUseCases<T>(
    private val syncPending: SyncPendingLocalChangesToServerUseCase<T>,
    private val syncIncremental: SyncIncrementalDataFromServerUseCase<T>,
    private val resolveConflict: ResolveDataConflictUseCase<T>,
    private val syncOlder: SyncOlderDataFromServerUseCase<T>,
    private val syncLatest: SyncLatestDataFromServerUseCase<T>,
    private val syncAll: SyncAllDataFromServerUseCase<T>
) where T : AggregateRoot {

    /**
     * 로컬 변경사항을 서버로 동기화
     * Outbox 패턴 기반 오프라인 변경사항 업로드
     */
    suspend fun syncPending(
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>
    ) = syncPending.invoke(remoteRepository, localRepository)

    /**
     * 서버에서 증분 데이터 동기화
     * 마지막 동기화 시점 이후 변경된 데이터만 다운로드
     */
    suspend fun syncIncremental(
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>
    ) = syncIncremental.invoke(remoteRepository, localRepository)

    /**
     * 데이터 충돌 해결
     * 로컬과 서버 데이터 간 충돌 상황을 해결
     */
    suspend fun resolveConflict(
        entityId: String,
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>
    ) = resolveConflict.invoke(entityId, remoteRepository, localRepository)

    /**
     * 과거 데이터 동기화
     * 페이지네이션을 통한 점진적 과거 데이터 로드
     */
    suspend fun syncOlder(
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>,
        batchSize: Long = 50L
    ) = syncOlder.invoke(remoteRepository, localRepository, batchSize)

    /**
     * 최신 데이터 동기화
     * 서버의 최신 데이터와 로컬 데이터 동기화
     */
    suspend fun syncLatest(
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>,
        batchSize: Long = 100L
    ) = syncLatest.invoke(remoteRepository, localRepository, batchSize)

    /**
     * 전체 데이터 동기화
     * 서버의 모든 데이터를 로컬로 동기화 (초기화용)
     */
    suspend fun syncAll(
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>,
        batchSize: Long = 200L,
        clearLocalFirst: Boolean = false
    ) = syncAll.invoke(remoteRepository, localRepository, batchSize, clearLocalFirst)
}