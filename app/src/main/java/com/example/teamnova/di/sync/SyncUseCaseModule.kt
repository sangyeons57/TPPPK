package com.example.teamnova.di.sync

import com.example.data_core.dao.OutboxDao
import com.example.data_core.dao.SyncMetadataDao
import com.example.domain.provider.SyncUseCaseProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Sync UseCase DI 모듈
 *
 * 기존 72개의 개별 UseCase를 6개의 제네릭 UseCase로 통합
 * SyncUseCaseProvider를 통해 동적으로 생성하는 방식 사용
 *
 * 제공하는 UseCase (Provider 내부에서 생성):
 * - SyncPendingLocalChangesToServerUseCase: 로컬 변경사항 서버 업로드
 * - SyncIncrementalDataFromServerUseCase: 증분 서버 동기화
 * - ResolveDataConflictUseCase: 데이터 충돌 해결
 * - SyncOlderDataFromServerUseCase: 과거 데이터 점진적 로드
 * - SyncLatestDataFromServerUseCase: 최신 데이터 동기화
 * - SyncAllDataFromServerUseCase: 전체 데이터 동기화
 *
 * 사용 방법:
 * ```kotlin
 * @Inject lateinit var syncUseCaseProvider: SyncUseCaseProvider
 *
 * val syncUseCases = syncUseCaseProvider.create<Category>()
 * syncUseCases.syncIncremental(remoteCategoryRepo, localCategoryRepo)
 * ```
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncUseCaseModule {

    /**
     * Sync UseCase Provider 제공
     * OutboxDao와 SyncMetadataDao를 주입받아 필요시 UseCase들을 직접 생성
     *
     * 장점:
     * - 타입 캐스팅 불필요
     * - 메모리 효율적 (필요할 때만 UseCase 생성)
     * - 다른 Provider 패턴과 일관성
     */
    @Provides
    @Singleton
    fun provideSyncUseCaseProvider(
        outboxDao: OutboxDao,
        syncMetadataDao: SyncMetadataDao
    ): SyncUseCaseProvider {
        return SyncUseCaseProvider(
            outboxDao = outboxDao,
            syncMetadataDao = syncMetadataDao
        )
    }
}