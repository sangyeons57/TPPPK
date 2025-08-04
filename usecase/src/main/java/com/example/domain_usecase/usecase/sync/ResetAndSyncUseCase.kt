package com.example.domain_usecase.usecase.sync

import com.example.core_common.result.CustomResult
import com.example.domain.model.sync.SyncCoordinator
import com.example.domain.model.sync.SyncScope
import com.example.domain_repository.base.MessageRepository
import javax.inject.Inject

/**
 * 로컬 캐시를 완전히 클리어하고 전체 동기화를 실행하는 UseCase
 *
 * 데이터 손상이나 동기화 문제 해결을 위해 로컬 캐시를 리셋하고
 * 서버에서 최신 데이터를 다시 가져와 동기화를 수행합니다.
 */
class ResetAndSyncUseCase @Inject constructor(
    private val syncManager: SyncCoordinator,
    private val messageRepository: MessageRepository // 일단 메시지만, 나중에 확장 가능
) {
    /**
     * 로컬 캐시 완전 클리어 후 전체 동기화 실행
     *
     * @param tableName 동기화할 테이블/스트림 이름 (예: "messages", "users", "projects")
     * @param channelId 클리어할 채널 ID (messages 테이블용, 다른 테이블은 nullable)
     * @return 동기화 성공/실패 결과
     */
    suspend operator fun invoke(
        tableName: String,
        channelId: String? = null
    ): CustomResult<Unit, Exception> {
        return try {
            // 1. 테이블별 로컬 캐시 클리어
            when (tableName) {
                "messages" -> {
                    requireNotNull(channelId) { "channelId is required for messages table" }
                    when (val clearResult = messageRepository.clearLocalCache(channelId)) {
                        is CustomResult.Failure -> return clearResult
                        else -> { /* 성공 시 계속 진행 */
                        }
                    }
                }
                // 추후 다른 테이블 추가 가능
                // "users" -> userRepository.clearLocalCache()
                // "projects" -> projectRepository.clearLocalCache() 
                else -> {
                    // 알려지지 않은 테이블명에 대해서는 경고 로그만 출력하고 계속 진행
                    // throw IllegalArgumentException("Unsupported table name: $tableName")
                }
            }

            // 2. 전체 동기화 실행 (DefaultSyncManager 활용)
            syncManager.sync(SyncScope.Stream(tableName))

            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}