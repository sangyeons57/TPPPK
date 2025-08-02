package com.example.data_datasource.local

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Message
import com.example.domain.model.enum.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * Message 로컬 데이터 소스 인터페이스
 * Repository에서 메시지 CRUD 처리 및 SyncManager에서 동기화 처리용
 */
interface MessageDataSource {

    // ================================
    // Repository용 - 기본 CRUD 작업
    // ================================
    
    /**
     * 단일 Message 저장 (생성/업데이트)
     * @param message 저장할 Message 도메인 모델
     * @return 성공/실패 결과
     */
    suspend fun save(message: Message): CustomResult<Unit, Exception>
    
    /**
     * 여러 Message들을 배치로 저장
     * @param messages 저장할 Message 도메인 모델 목록
     * @return 성공/실패 결과
     */
    suspend fun saveAll(messages: List<Message>): CustomResult<Unit, Exception>
    
    /**
     * Message 삭제 (실제 삭제가 아닌 deleted 플래그 설정)
     * @param message 삭제할 Message 도메인 모델
     * @return 성공/실패 결과
     */
    suspend fun delete(message: Message): CustomResult<Unit, Exception>

    // ================================
    // Repository용 - 기본 조회 작업
    // ================================
    
    /**
     * ID로 특정 Message 조회
     * @param id 메시지 ID
     * @return Message 도메인 모델 (없으면 null)
     */
    suspend fun getById(id: String): CustomResult<Message, Exception>
    
    /**
     * 모든 Message 조회
     * @return 모든 Message 목록 (최신순)
     */
    suspend fun getAll(): CustomResult<List<Message>, Exception>
    
    /**
     * ID로 특정 Message 관찰
     * @param id 관찰할 메시지 ID
     * @return Message Flow
     */
    fun observeById(id: String): Flow<CustomResult<Message?, Exception>>
    
    /**
     * 모든 Message 관찰
     * @return 모든 Message Flow (최신순)
     */
    fun observeAll(): Flow<CustomResult<List<Message>, Exception>>

    // ================================
    // Repository용 - 시간 범위별 조회
    // ================================
    
    /**
     * 특정 시간 이후의 메시지들 조회 (페이징용)
     * @param afterTimestamp 기준 시간 (epoch milliseconds)
     * @param limit 조회할 개수 제한
     * @return 해당 시간 이후의 메시지 목록
     */
    suspend fun getMessagesAfter(
        afterTimestamp: Long, 
        limit: Int = 50
    ): CustomResult<List<Message>, Exception>
    
    /**
     * 특정 시간 이전의 메시지들 조회 (페이징용)
     * @param beforeTimestamp 기준 시간 (epoch milliseconds)
     * @param limit 조회할 개수 제한
     * @return 해당 시간 이전의 메시지 목록
     */
    suspend fun getMessagesBefore(
        beforeTimestamp: Long, 
        limit: Int = 50
    ): CustomResult<List<Message>, Exception>
    
    /**
     * 특정 시간 범위의 메시지들 조회
     * @param startTimestamp 시작 시간 (epoch milliseconds)
     * @param endTimestamp 종료 시간 (epoch milliseconds)
     * @return 해당 시간 범위의 메시지 목록
     */
    suspend fun getMessagesBetween(
        startTimestamp: Long, 
        endTimestamp: Long
    ): CustomResult<List<Message>, Exception>

    // ================================
    // Repository용 - 사용자별 조회
    // ================================
    
    /**
     * 특정 사용자가 보낸 메시지들 조회
     * @param senderId 발신자 ID
     * @return 해당 사용자의 메시지 목록
     */
    suspend fun getMessagesBySender(senderId: String): CustomResult<List<Message>, Exception>
    
    /**
     * 답글 메시지들 조회
     * @param replyToMessageId 원본 메시지 ID
     * @return 해당 메시지에 대한 답글 목록
     */
    suspend fun getRepliesByMessageId(replyToMessageId: String): CustomResult<List<Message>, Exception>

    // ================================
    // SyncManager용 - 동기화 관련 작업
    // ================================
    
    /**
     * 특정 동기화 상태의 메시지들 조회
     * @param syncStatus 동기화 상태
     * @return 해당 상태의 메시지 목록
     */
    suspend fun getMessagesBySyncStatus(syncStatus: SyncStatus): CustomResult<List<Message>, Exception>
    
    /**
     * 동기화가 필요한 메시지들 조회
     * @return 동기화가 필요한 메시지 목록
     */
    suspend fun getUnsyncedMessages(): CustomResult<List<Message>, Exception>
    
    /**
     * 에러 상태의 메시지들 조회
     * @return 동기화 에러가 발생한 메시지 목록
     */
    suspend fun getErrorMessages(): CustomResult<List<Message>, Exception>
    
    /**
     * 특정 서버 버전 이후의 메시지들 조회
     * @param version 기준 서버 버전
     * @return 해당 버전 이후의 메시지 목록
     */
    suspend fun getMessagesAfterVersion(version: Long): CustomResult<List<Message>, Exception>
    
    /**
     * Message의 동기화 상태 업데이트
     * @param messageId 메시지 ID
     * @param syncStatus 새로운 동기화 상태
     * @param serverVersion 서버 버전 (선택사항)
     * @param serverUpdatedAt 서버 업데이트 시간 (선택사항)
     * @return 성공/실패 결과
     */
    suspend fun updateSyncStatus(
        messageId: String,
        syncStatus: SyncStatus,
        serverVersion: Long? = null,
        serverUpdatedAt: Long? = null
    ): CustomResult<Unit, Exception>

    // ================================
    // SyncManager용 - 배치 처리
    // ================================
    
    /**
     * 동기화가 필요한 메시지들을 제한된 개수만큼 조회 (배치 처리용)
     * @param limit 조회할 최대 개수
     * @return 생성시간 순으로 정렬된 미동기화 메시지 목록
     */
    suspend fun getUnsyncedMessagesWithLimit(limit: Int): CustomResult<List<Message>, Exception>
    
    /**
     * 여러 메시지들의 동기화 상태를 일괄 업데이트
     * @param messageIds 업데이트할 메시지 ID 목록
     * @param newSyncStatus 새로운 동기화 상태
     * @return 업데이트된 메시지 개수
     */
    suspend fun updateSyncStatusByIds(
        messageIds: List<String>, 
        newSyncStatus: SyncStatus
    ): CustomResult<Int, Exception>

    // ================================
    // Repository용 - 정리 및 관리
    // ================================
    
    /**
     * ID로 특정 Message 물리적 삭제
     * @param id 삭제할 메시지 ID
     * @return 삭제된 행의 개수
     */
    suspend fun deleteById(id: String): CustomResult<Int, Exception>
    
    /**
     * 오래된 메시지들 정리 (특정 시간 이전)
     * @param beforeTimestamp 기준 시간 이전 (epoch milliseconds)
     * @return 삭제된 메시지 개수
     */
    suspend fun deleteOldMessages(beforeTimestamp: Long): CustomResult<Int, Exception>
    
    /**
     * 삭제 마크된 메시지들 완전 제거
     * @return 삭제된 메시지 개수
     */
    suspend fun deleteMarkedMessages(): CustomResult<Int, Exception>

    // ================================
    // Repository용 - 통계 및 개수 조회
    // ================================
    
    /**
     * 전체 메시지 개수 조회
     * @return 전체 메시지 개수
     */
    suspend fun getTotalCount(): CustomResult<Int, Exception>
    
    /**
     * 동기화가 필요한 메시지 개수 조회
     * @return 미동기화 메시지 개수
     */
    suspend fun getUnsyncedCount(): CustomResult<Int, Exception>
    
    /**
     * 동기화 상태별 메시지 통계 조회
     * @return 상태별 메시지 개수 통계
     */
    suspend fun getSyncStatusStatistics(): CustomResult<Map<SyncStatus, Int>, Exception>

    // ================================
    // Repository용 - 존재 여부 확인
    // ================================
    
    /**
     * 메시지 ID 존재 여부 확인
     * @param id 확인할 메시지 ID
     * @return 존재 여부
     */
    suspend fun exists(id: String): CustomResult<Boolean, Exception>

    // ================================
    // 테스트 및 디버깅용
    // ================================
    
    /**
     * 모든 Message를 생성시간 순으로 조회 (디버깅용)
     * @return 생성시간 내림차순으로 정렬된 모든 Message 목록
     */
    suspend fun getAllForDebug(): CustomResult<List<Message>, Exception>
    
    /**
     * 모든 Message 삭제 (테스트/초기화용)
     * @return 성공/실패 결과
     */
    suspend fun deleteAll(): CustomResult<Unit, Exception>

    // ================================
    // 전송 상태 관리
    // ================================

    /**
     * Message의 전송 상태 업데이트
     * @param messageId 메시지 ID
     * @param deliveryStatus 새로운 전송 상태 (SENDING/SENT/FAILED)
     * @return 성공/실패 결과
     */
    suspend fun updateDeliveryStatus(
        messageId: String,
        deliveryStatus: String
    ): CustomResult<Unit, Exception>

    /**
     * Message 저장 시 전송 상태 지정
     * @param message 저장할 메시지
     * @param deliveryStatus 전송 상태 (SENDING/SENT/FAILED)
     * @return 성공/실패 결과
     */
    suspend fun saveWithDeliveryStatus(
        message: Message,
        deliveryStatus: String
    ): CustomResult<com.example.domain.model.vo.DocumentId, Exception>
}