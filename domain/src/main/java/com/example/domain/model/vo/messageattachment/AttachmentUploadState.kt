package com.example.domain.model.vo.messageattachment

import com.example.domain.model.enum.MessageAttachmentUploadStatus

/**
 * 메시지 첨부파일의 업로드 상태를 나타내는 불변 데이터 클래스
 * 진행률과 상태를 원자적으로 관리합니다.
 */
data class AttachmentUploadState(
    val status: MessageAttachmentUploadStatus,
    val progress: MessageAttachmentUploadProgress,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    
    /**
     * 상태가 완료된 상태인지 확인합니다.
     */
    val isCompleted: Boolean
        get() = status == MessageAttachmentUploadStatus.COMPLETED
    
    /**
     * 상태가 진행 중인지 확인합니다.
     */
    val isInProgress: Boolean
        get() = status == MessageAttachmentUploadStatus.UPLOADING || 
                status == MessageAttachmentUploadStatus.PENDING
    
    /**
     * 상태가 실패했는지 확인합니다.
     */
    val isFailed: Boolean
        get() = status == MessageAttachmentUploadStatus.FAILED
    
    /**
     * 진행률을 퍼센트로 반환합니다.
     */
    val progressPercentage: Int
        get() = (progress.value * 100).toInt()

    companion object {
        /**
         * 초기 상태를 생성합니다.
         */
        fun initial(): AttachmentUploadState {
            return AttachmentUploadState(
                status = MessageAttachmentUploadStatus.PENDING,
                progress = MessageAttachmentUploadProgress.zero()
            )
        }
        
        /**
         * 완료 상태를 생성합니다.
         */
        fun completed(): AttachmentUploadState {
            return AttachmentUploadState(
                status = MessageAttachmentUploadStatus.COMPLETED,
                progress = MessageAttachmentUploadProgress.complete()
            )
        }
        
        /**
         * 실패 상태를 생성합니다.
         */
        fun failed(errorMessage: String): AttachmentUploadState {
            return AttachmentUploadState(
                status = MessageAttachmentUploadStatus.FAILED,
                progress = MessageAttachmentUploadProgress.zero(),
                errorMessage = errorMessage
            )
        }
        
        /**
         * 업로드 중 상태를 생성합니다.
         */
        fun uploading(progress: Float): AttachmentUploadState {
            return AttachmentUploadState(
                status = MessageAttachmentUploadStatus.UPLOADING,
                progress = MessageAttachmentUploadProgress(progress)
            )
        }
    }

    /**
     * 새로운 진행률로 상태를 업데이트합니다.
     * 상태 일관성을 보장합니다.
     */
    fun withProgress(newProgress: Float): AttachmentUploadState {
        val progressValue = MessageAttachmentUploadProgress(newProgress)
        val newStatus = when {
            newProgress >= 1.0f -> MessageAttachmentUploadStatus.COMPLETED
            newProgress > 0.0f -> MessageAttachmentUploadStatus.UPLOADING
            else -> MessageAttachmentUploadStatus.PENDING
        }
        
        return copy(
            status = newStatus,
            progress = progressValue,
            errorMessage = if (newStatus != MessageAttachmentUploadStatus.FAILED) null else errorMessage,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 실패 상태로 변경합니다.
     */
    fun withError(error: String): AttachmentUploadState {
        return copy(
            status = MessageAttachmentUploadStatus.FAILED,
            errorMessage = error,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 상태 전환이 유효한지 검증합니다.
     */
    fun canTransitionTo(newState: AttachmentUploadState): Boolean {
        return when (this.status to newState.status) {
            // 대기 상태에서는 모든 상태로 전환 가능
            MessageAttachmentUploadStatus.PENDING to MessageAttachmentUploadStatus.PENDING,
            MessageAttachmentUploadStatus.PENDING to MessageAttachmentUploadStatus.UPLOADING,
            MessageAttachmentUploadStatus.PENDING to MessageAttachmentUploadStatus.COMPLETED,
            MessageAttachmentUploadStatus.PENDING to MessageAttachmentUploadStatus.FAILED -> true
            
            // 업로드 중에서는 완료, 실패로만 전환 가능 (또는 진행률 업데이트)
            MessageAttachmentUploadStatus.UPLOADING to MessageAttachmentUploadStatus.COMPLETED,
            MessageAttachmentUploadStatus.UPLOADING to MessageAttachmentUploadStatus.FAILED,
            MessageAttachmentUploadStatus.UPLOADING to MessageAttachmentUploadStatus.UPLOADING -> true
            
            // 완료 상태에서는 변경 불가
            MessageAttachmentUploadStatus.COMPLETED to MessageAttachmentUploadStatus.PENDING,
            MessageAttachmentUploadStatus.COMPLETED to MessageAttachmentUploadStatus.UPLOADING,
            MessageAttachmentUploadStatus.COMPLETED to MessageAttachmentUploadStatus.COMPLETED,
            MessageAttachmentUploadStatus.COMPLETED to MessageAttachmentUploadStatus.FAILED -> false
            
            // 실패 상태에서는 대기 상태로만 전환 가능 (재시도)
            MessageAttachmentUploadStatus.FAILED to MessageAttachmentUploadStatus.PENDING -> true
            MessageAttachmentUploadStatus.FAILED to MessageAttachmentUploadStatus.UPLOADING,
            MessageAttachmentUploadStatus.FAILED to MessageAttachmentUploadStatus.COMPLETED,
            MessageAttachmentUploadStatus.FAILED to MessageAttachmentUploadStatus.FAILED -> false
            
            else -> false
        }
    }
    
    override fun toString(): String {
        return "AttachmentUploadState(status=$status, progress=${progressPercentage}%, error=$errorMessage)"
    }
}