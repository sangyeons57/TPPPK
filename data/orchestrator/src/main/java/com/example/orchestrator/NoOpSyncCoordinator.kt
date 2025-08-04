package com.example.orchestrator

import com.example.domain.model.sync.SyncCoordinator
import com.example.domain.model.sync.SyncScope
import javax.inject.Inject

/**
 * SyncCoordinator의 NoOp 구현체
 * 현재 단순한 운영 환경에서 복잡한 동기화 로직이 불필요하므로
 * 빈 구현을 제공하여 Hilt DI 오류를 해결합니다.
 */
class NoOpSyncCoordinator @Inject constructor() : SyncCoordinator {

    override suspend fun syncAll() {
        // No operation - 동기화 로직이 불필요한 현재 상황에서는 아무것도 하지 않음
    }

    override suspend fun sync(scope: SyncScope) {
        // No operation
    }
}