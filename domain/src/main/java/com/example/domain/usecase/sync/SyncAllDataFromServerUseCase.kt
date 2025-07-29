package com.example.domain.usecase.sync

// TODO: data 레이어 의존성 - SyncMetadataDao, SyncMetadataEntity 제거 필요
import com.example.core_common.result.CustomResult
import com.example.domain.model.AggregateRoot
import com.example.domain.repository.local.base.SyncableRepository
import com.example.domain.repository.remote.DefaultRepository
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
    // TODO: SyncMetadataDao는 data 레이어 의존성이므로 domain에서 제거 필요
) where T : AggregateRoot {

    suspend operator fun invoke(
        remoteRepository: DefaultRepository<T>,
        localRepository: SyncableRepository<T>,
        batchSize: Long = 200L,
        clearLocalFirst: Boolean = false
    ): CustomResult<SyncResult, Exception> {
        // TODO: 전체 서버 동기화 로직 구현 필요
        // - 서버에서 전체 데이터 배치 조회
        // - 로컬 저장소에 저장
        // - SyncMetadata 업데이트 (data 레이어에서)
        // - 동기화 결과 반환
        TODO("SyncAllDataFromServerUseCase implementation needed - requires data layer integration")
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