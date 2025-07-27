package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.repository.factory.context.MessageRepositoryFactoryContext
import java.time.Instant

// 메시지 전송 시 사용할 첨부파일 모델 (도메인 모델 MessageAttachment와 구분)
data class MessageAttachmentToSend(
    val fileName: String,
    val mimeType: String,
    val sourceUri: String // 예시: content URI 또는 file URI
    // val bytes: ByteArray? // 또는 직접 바이트를 전달할 경우
)

/**
 * Remote Message Repository Interface (Sync-Only)
 * 클라이언트 주도 동기화 전용 - 직접 읽기/쓰기 불가능
 *
 * 🔒 제약사항:
 * - Room 접근 금지 (LocalMessageRepository 사용)
 * - Flow/LiveData 반환 금지 (비동기 fetch-only)
 * - 직접적인 CRUD 작업 불가능
 *
 * ✅ 역할:
 * - 서버에서 증분 데이터 가져오기 (updatedAt > cursor)
 * - 로컬 변경사항을 서버에 반영 (Outbox → Firestore)
 * - 동기화 충돌 해결 (서버 vs 로컬)
 * - 커서 기반 동기화 메타데이터 관리
 */
interface MessageRepository {
    val factoryContext: MessageRepositoryFactoryContext

    /**
     * 서버에서 증분 데이터 가져오기 (Client-driven Sync)
     * @param lastSyncCursor 마지막 동기화 커서 (null이면 전체 동기화)
     * @param channelId 특정 채널 동기화 (null이면 전체 채널)
     * @return 새로운 메시지 목록과 다음 커서
     */
    suspend fun syncFromServer(
        lastSyncCursor: Long? = null,
        channelId: String? = null
    ): CustomResult<SyncResult<Message>, Exception>

    /**
     * 로컬 변경사항을 서버에 반영 (Outbox Processing)
     * @param channelId 특정 채널의 Outbox만 처리 (null이면 전체)
     * @return 처리된 Outbox 작업 수
     */
    suspend fun syncToServer(
        channelId: String? = null
    ): CustomResult<Int, Exception>

    /**
     * 강제 전체 동기화 (예: 첫 로그인, 데이터 불일치 해결)
     * @param channelId 특정 채널만 동기화 (null이면 전체)
     * @return 동기화된 메시지 수
     */
    suspend fun forceSyncAll(
        channelId: String? = null
    ): CustomResult<Int, Exception>

    /**
     * 동기화 충돌 해결 (서버 우선 정책)
     * @param conflictedMessageIds 충돌이 발생한 메시지 ID 목록
     * @return 해결된 충돌 수
     */
    suspend fun resolveConflicts(
        conflictedMessageIds: List<String>
    ): CustomResult<Int, Exception>
}

/**
 * 동기화 결과 데이터 클래스
 */
data class SyncResult<T>(
    val data: List<T>,
    val nextCursor: Long?,
    val hasMore: Boolean = false
)
