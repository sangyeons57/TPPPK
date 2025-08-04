package com.example.domain_usecase.usecase.sync

import com.example.core_common.result.CustomResult
import com.example.domain.model.sync.SyncCoordinator
import com.example.domain.model.sync.SyncScope
import javax.inject.Inject

/**
 * 특정 테이블/스트림의 증분 동기화를 실행하는 UseCase
 *
 * DefaultSyncManager를 활용하여 커서 기반 증분 동기화를 수행합니다.
 * 네트워크 부하를 최소화하면서 변경된 데이터만 동기화합니다.
 */
class SyncUseCase @Inject constructor(
    private val syncManager: SyncCoordinator
) {
    /**
     * 특정 테이블/스트림의 증분 동기화 실행
     *
     * @param tableName 동기화할 테이블/스트림 이름 (예: "messages", "users", "projects")
     * @return 동기화 성공/실패 결과
     */
    suspend operator fun invoke(tableName: String): CustomResult<Unit, Exception> {
        return try {
            syncManager.sync(SyncScope.Stream(tableName))
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}