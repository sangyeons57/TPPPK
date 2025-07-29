package com.example.domain.model.base


import com.example.domain.model.AggregateRoot
import com.example.domain.event.messageattachment.MessageAttachmentAddedEvent
import com.example.domain.model.enum.MessageAttachmentType
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileName
import com.example.domain.model.vo.messageattachment.MessageAttachmentFileSize
import com.example.domain.model.vo.messageattachment.MessageAttachmentUrl
import com.example.domain.model.vo.messageattachment.MessageAttachmentThumbnailUrl
import com.example.domain.model.vo.messageattachment.MessageAttachmentUploadProgress
import com.example.domain.model.vo.messageattachment.AttachmentUploadState
import com.example.domain.model.enum.MessageAttachmentUploadStatus
import com.example.domain.util.UploadStateMachine
import com.example.domain.util.UploadStateEvent
import com.example.domain.util.StateTransitionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import com.example.core_common.util.DateTimeUtil
// import timber.log.Timber - Domain 모듈에서 제거됨

class MessageAttachment private constructor(
    initialAttachmentType: MessageAttachmentType, // e.g., IMAGE, FILE, VIDEO
    initialAttachmentUrl: MessageAttachmentUrl, // URL to the file in storage
    initialFileName: MessageAttachmentFileName?,
    initialFileSize: MessageAttachmentFileSize?,
    initialThumbnailUrl: MessageAttachmentThumbnailUrl?,
    initialUploadStatus: MessageAttachmentUploadStatus,
    initialUploadProgress: MessageAttachmentUploadProgress,
    override val id: DocumentId,
    override val isNew: Boolean,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    private val stateMachine: UploadStateMachine = UploadStateMachine()
) : AggregateRoot() {


    val attachmentType: MessageAttachmentType = initialAttachmentType
    val attachmentUrl: MessageAttachmentUrl = initialAttachmentUrl
    val fileName: MessageAttachmentFileName? = initialFileName
    val fileSize: MessageAttachmentFileSize? = initialFileSize
    val thumbnailUrl: MessageAttachmentThumbnailUrl? = initialThumbnailUrl
    
    // 동시성 안전을 위한 상태 관리
    private val stateLock = Mutex()
    private val _uploadState = MutableStateFlow(
        AttachmentUploadState(
            status = initialUploadStatus,
            progress = initialUploadProgress
        )
    )
    
    /**
     * 업로드 상태를 관찰할 수 있는 StateFlow
     * UI에서 상태 변화를 실시간으로 구독할 수 있습니다.
     */
    val uploadState: StateFlow<AttachmentUploadState> = _uploadState.asStateFlow()
    
    // 기존 호환성을 위한 프로퍼티들 (StateFlow에서 파생)
    val uploadStatus: MessageAttachmentUploadStatus
        get() = _uploadState.value.status
    val uploadProgress: MessageAttachmentUploadProgress
        get() = _uploadState.value.progress


    /**
     * 업로드 진행률을 업데이트합니다.
     * 상태 머신을 사용하여 안전한 상태 전환을 보장합니다.
     */
    suspend fun updateUploadProgress(progress: MessageAttachmentUploadProgress) {
        val event = UploadStateEvent.UpdateProgress(progress.value)
        executeStateTransition(event)
    }

    /**
     * 업로드 상태를 업데이트합니다.
     * 상태 머신을 사용하여 안전한 상태 전환을 보장합니다.
     */
    suspend fun updateUploadStatus(status: MessageAttachmentUploadStatus, errorMessage: String? = null) {
        val event = when (status) {
            MessageAttachmentUploadStatus.FAILED -> UploadStateEvent.FailUpload(errorMessage ?: "업로드 실패")
            MessageAttachmentUploadStatus.UPLOADING -> UploadStateEvent.StartUpload
            MessageAttachmentUploadStatus.COMPLETED -> UploadStateEvent.CompleteUpload()
            MessageAttachmentUploadStatus.PENDING -> UploadStateEvent.RetryUpload
        }
        executeStateTransition(event)
    }
    
    /**
     * 현재 업로드 상태 정보를 안전하게 가져옵니다.
     */
    fun getCurrentUploadState(): AttachmentUploadState {
        return _uploadState.value
    }
    
    /**
     * 업로드가 완료되었는지 확인합니다.
     */
    fun isUploadCompleted(): Boolean {
        return _uploadState.value.isCompleted
    }
    
    /**
     * 업로드가 실패했는지 확인합니다.
     */
    fun isUploadFailed(): Boolean {
        return _uploadState.value.isFailed
    }
    
    /**
     * 업로드가 진행 중인지 확인합니다.
     */
    fun isUploadInProgress(): Boolean {
        return _uploadState.value.isInProgress
    }
    
    /**
     * 업로드 세션을 시작합니다. (중복 업로드 방지용)
     */
    suspend fun startUploadSession(uploadId: String): Boolean {
        val currentState = _uploadState.value
        val event = UploadStateEvent.StartUpload
        
        return if (stateMachine.canTransition(currentState, event)) {
            executeStateTransition(event)
            true
        } else {
            false // 이미 업로드 중이거나 완료된 상태
        }
    }
    
    /**
     * 업로드 세션을 종료합니다.
     */
    suspend fun completeUploadSession() {
        val event = UploadStateEvent.CompleteUpload()
        executeStateTransition(event)
    }
    
    /**
     * 업로드 세션을 취소합니다.
     */
    suspend fun cancelUploadSession(errorMessage: String = "업로드 취소") {
        val event = UploadStateEvent.CancelUpload
        executeStateTransition(event)
    }
    
    /**
     * 상태 머신을 사용하여 안전한 상태 전환을 실행합니다.
     */
    private suspend fun executeStateTransition(event: UploadStateEvent) {
        stateLock.withLock {
            val currentState = _uploadState.value
            val transitionResult = stateMachine.transitionState(currentState, event)
            
            when (transitionResult) {
                is StateTransitionResult.Success -> {
                    _uploadState.value = transitionResult.newState
                    markAsChanged()
                    
                    // 부수 효과 처리
                    transitionResult.sideEffect?.let { sideEffect ->
                        handleSideEffect(sideEffect)
                    }
                }
                is StateTransitionResult.Failure -> {
                    println("WARNING: State transition failed: ${transitionResult.reason}")
                    // 실패한 경우 현재 상태 유지
                }
            }
        }
    }
    
    /**
     * 상태 전환 시 발생하는 부수 효과를 처리합니다.
     */
    private fun handleSideEffect(sideEffect: com.example.domain.util.SideEffect) {
        when (sideEffect) {
            is com.example.domain.util.SideEffect.NotifyProgress -> {
                println("DEBUG: Upload progress updated: ${_uploadState.value.progressPercentage}%")
            }
            is com.example.domain.util.SideEffect.NotifyCompletion -> {
                println("DEBUG: Upload completed for attachment: ${id.value}")
            }
            is com.example.domain.util.SideEffect.NotifyError -> {
                println("ERROR: Upload error for attachment ${id.value}: ${sideEffect.error}")
            }
            is com.example.domain.util.SideEffect.CleanupResources -> {
                println("DEBUG: Cleaning up resources for attachment: ${id.value}")
            }
        }
    }
    
    /**
     * 업로드를 재시도합니다.
     */
    suspend fun retryUpload() {
        val event = UploadStateEvent.RetryUpload
        executeStateTransition(event)
    }

    companion object {
        const val COLLECTION_NAME = "message_attachments"
        const val KEY_ATTACHMENT_TYPE = "attachmentType"
        const val KEY_ATTACHMENT_URL = "attachmentUrl"
        const val KEY_FILE_NAME = "fileName"
        const val KEY_FILE_SIZE = "fileSize"
        const val KEY_THUMBNAIL_URL = "thumbnailUrl"
        const val KEY_UPLOAD_STATUS = "uploadStatus"
        const val KEY_UPLOAD_PROGRESS = "uploadProgress"
        /**
         * Factory method for creating a new message attachment.
         */
        fun create(
            id: DocumentId,
            attachmentType: MessageAttachmentType,
            attachmentUrl: MessageAttachmentUrl,
            fileName: MessageAttachmentFileName?,
            fileSize: MessageAttachmentFileSize?,
            thumbnailUrl: MessageAttachmentThumbnailUrl? = null,
            uploadStatus: MessageAttachmentUploadStatus = MessageAttachmentUploadStatus.PENDING,
            uploadProgress: MessageAttachmentUploadProgress = MessageAttachmentUploadProgress.zero()
        ): MessageAttachment {
            val attachment = MessageAttachment(
                initialAttachmentType = attachmentType,
                initialAttachmentUrl = attachmentUrl,
                createdAt = DateTimeUtil.nowInstant(),
                updatedAt = DateTimeUtil.nowInstant(),
                initialFileName = fileName,
                initialFileSize = fileSize,
                initialThumbnailUrl = thumbnailUrl,
                initialUploadStatus = uploadStatus,
                initialUploadProgress = uploadProgress,
                id = id,
                isNew = true,
            ).also {
                // 첨부파일 추가 이벤트 발생
                it.addEvent(
                    MessageAttachmentAddedEvent(
                        attachmentId = id,
                        attachmentType = attachmentType,
                        attachmentUrl = attachmentUrl,
                        fileName = fileName,
                        fileSize = fileSize,
                        occurredOn = DateTimeUtil.nowInstant()
                    )
                )
            }
            return attachment
        }

        /**
         * Factory method to reconstitute a MessageAttachment from a data source.
         */
        fun fromDataSource(
            id: DocumentId,
            attachmentType: MessageAttachmentType,
            attachmentUrl: MessageAttachmentUrl,
            createdAt: Instant?,
            updatedAt: Instant?,
            fileName: MessageAttachmentFileName?,
            fileSize: MessageAttachmentFileSize?,
            thumbnailUrl: MessageAttachmentThumbnailUrl? = null,
            uploadStatus: MessageAttachmentUploadStatus = MessageAttachmentUploadStatus.COMPLETED,
            uploadProgress: MessageAttachmentUploadProgress = MessageAttachmentUploadProgress.complete()
        ): MessageAttachment {
            return MessageAttachment(
                initialAttachmentType = attachmentType,
                initialAttachmentUrl = attachmentUrl,
                createdAt = createdAt ?: DateTimeUtil.nowInstant(),
                updatedAt = updatedAt ?: DateTimeUtil.nowInstant(),
                initialFileName = fileName,
                initialFileSize = fileSize,
                initialThumbnailUrl = thumbnailUrl,
                initialUploadStatus = uploadStatus,
                initialUploadProgress = uploadProgress,
                id = id,
                isNew = false,
            )
        }
    }
}
