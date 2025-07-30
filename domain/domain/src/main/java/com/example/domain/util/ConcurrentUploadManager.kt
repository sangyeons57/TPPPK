package com.example.domain.util

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 동시 업로드 충돌 방지를 위한 업로드 관리자
 * 같은 첨부파일에 대한 중복 업로드를 방지하고 업로드 상태를 추적합니다.
 */
@Singleton
class ConcurrentUploadManager @Inject constructor() {
    
    private val uploadLocks = ConcurrentHashMap<String, Mutex>()
    private val activeUploads = ConcurrentHashMap<String, UploadSession>()
    private val managerLock = Mutex()
    
    /**
     * 업로드 세션 정보
     */
    data class UploadSession(
        val attachmentId: DocumentId,
        val uploadId: String,
        val startTime: Long = System.currentTimeMillis(),
        var isCancelled: Boolean = false,
        var isCompleted: Boolean = false
    )
    
    /**
     * 동시 업로드 오류
     */
    sealed class ConcurrentUploadError : Exception() {
        data class AlreadyUploading(val attachmentId: DocumentId) : ConcurrentUploadError() {
            override val message: String = "첨부파일이 이미 업로드 중입니다: ${attachmentId.value}"
        }
        
        data class UploadCancelled(val attachmentId: DocumentId) : ConcurrentUploadError() {
            override val message: String = "업로드가 취소되었습니다: ${attachmentId.value}"
        }
        
        data class SessionNotFound(val attachmentId: DocumentId) : ConcurrentUploadError() {
            override val message: String = "업로드 세션을 찾을 수 없습니다: ${attachmentId.value}"
        }
    }
    
    /**
     * 업로드를 시작하고 중복 업로드를 방지합니다.
     * @param attachmentId 첨부파일 ID
     * @param uploadId 업로드 세션 ID
     * @return 업로드 진행 가능 여부
     */
    suspend fun startUpload(
        attachmentId: DocumentId, 
        uploadId: String
    ): CustomResult<UploadSession, ConcurrentUploadError> {
        return managerLock.withLock {
            val attachmentKey = attachmentId.value
            
            // 이미 업로드 중인지 확인
            val existingSession = activeUploads[attachmentKey]
            if (existingSession != null && !existingSession.isCompleted && !existingSession.isCancelled) {
                return@withLock CustomResult.Failure(ConcurrentUploadError.AlreadyUploading(attachmentId))
            }
            
            // 새로운 업로드 세션 생성
            val session = UploadSession(
                attachmentId = attachmentId,
                uploadId = uploadId
            )
            
            // 세션 등록
            activeUploads[attachmentKey] = session
            
            // 첨부파일별 Mutex 생성 (없으면)
            uploadLocks.computeIfAbsent(attachmentKey) { Mutex() }
            
            CustomResult.Success(session)
        }
    }
    
    /**
     * 업로드 완료를 알립니다.
     * @param attachmentId 첨부파일 ID
     */
    suspend fun completeUpload(attachmentId: DocumentId): CustomResult<Unit, ConcurrentUploadError> {
        return managerLock.withLock {
            val attachmentKey = attachmentId.value
            val session = activeUploads[attachmentKey]
                ?: return@withLock CustomResult.Failure(ConcurrentUploadError.SessionNotFound(attachmentId))
            
            session.isCompleted = true
            
            // 완료된 세션은 제거 (메모리 절약)
            activeUploads.remove(attachmentKey)
            uploadLocks.remove(attachmentKey)
            
            CustomResult.Success(Unit)
        }
    }
    
    /**
     * 업로드를 취소합니다.
     * @param attachmentId 첨부파일 ID
     */
    suspend fun cancelUpload(attachmentId: DocumentId): CustomResult<Unit, ConcurrentUploadError> {
        return managerLock.withLock {
            val attachmentKey = attachmentId.value
            val session = activeUploads[attachmentKey]
                ?: return@withLock CustomResult.Failure(ConcurrentUploadError.SessionNotFound(attachmentId))
            
            session.isCancelled = true
            
            // 취소된 세션은 제거
            activeUploads.remove(attachmentKey)
            uploadLocks.remove(attachmentKey)
            
            CustomResult.Success(Unit)
        }
    }
    
    /**
     * 첨부파일별 업로드 락을 얻습니다.
     * 동일한 첨부파일에 대한 동시 작업을 방지합니다.
     */
    suspend fun <T> withUploadLock(
        attachmentId: DocumentId,
        action: suspend () -> T
    ): CustomResult<T, ConcurrentUploadError> {
        val attachmentKey = attachmentId.value
        val uploadLock = uploadLocks[attachmentKey]
            ?: return CustomResult.Failure(ConcurrentUploadError.SessionNotFound(attachmentId))
        
        return try {
            uploadLock.withLock {
                // 업로드가 취소되었는지 확인
                val session = activeUploads[attachmentKey]
                if (session?.isCancelled == true) {
                    return@withLock CustomResult.Failure(ConcurrentUploadError.UploadCancelled(attachmentId))
                }
                
                val result = action()
                CustomResult.Success(result)
            }
        } catch (e: Exception) {
            // 예외 발생 시 업로드 취소
            cancelUpload(attachmentId)
            throw e
        }
    }
    
    /**
     * 현재 업로드 중인 첨부파일들의 목록을 반환합니다.
     */
    fun getActiveUploads(): List<UploadSession> {
        return activeUploads.values.toList()
    }
    
    /**
     * 특정 첨부파일의 업로드 세션을 조회합니다.
     */
    fun getUploadSession(attachmentId: DocumentId): UploadSession? {
        return activeUploads[attachmentId.value]
    }
    
    /**
     * 첨부파일이 업로드 중인지 확인합니다.
     */
    fun isUploading(attachmentId: DocumentId): Boolean {
        val session = activeUploads[attachmentId.value]
        return session != null && !session.isCompleted && !session.isCancelled
    }
    
    /**
     * 오래된 업로드 세션들을 정리합니다.
     * @param maxAgeMs 최대 보관 시간 (밀리초)
     */
    suspend fun cleanupOldSessions(maxAgeMs: Long = 30 * 60 * 1000L) { // 30분
        managerLock.withLock {
            val currentTime = System.currentTimeMillis()
            val expiredSessions = activeUploads.filter { (_, session) ->
                currentTime - session.startTime > maxAgeMs
            }
            
            expiredSessions.forEach { (attachmentKey, _) ->
                activeUploads.remove(attachmentKey)
                uploadLocks.remove(attachmentKey)
            }
        }
    }
    
    /**
     * 모든 업로드 세션을 강제로 정리합니다.
     * 주로 테스트나 앱 종료 시 사용됩니다.
     */
    suspend fun clearAllSessions() {
        managerLock.withLock {
            activeUploads.clear()
            uploadLocks.clear()
        }
    }
}