package com.example.domain.util

import com.example.domain.model.enum.MessageAttachmentUploadStatus
import com.example.domain.model.vo.messageattachment.AttachmentUploadState
import com.example.domain.model.vo.messageattachment.MessageAttachmentUploadProgress
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
// import timber.log.Timber - Domain 모듈에서 제거됨
import javax.inject.Inject

/**
 * 업로드 상태 전환 이벤트
 */
sealed class UploadStateEvent {
    object StartUpload : UploadStateEvent()
    data class UpdateProgress(val progress: Float) : UploadStateEvent()
    data class CompleteUpload(val finalUrl: String? = null) : UploadStateEvent()
    data class FailUpload(val error: String) : UploadStateEvent()
    object RetryUpload : UploadStateEvent()
    object CancelUpload : UploadStateEvent()
}

/**
 * 상태 전환 결과
 */
sealed class StateTransitionResult {
    data class Success(
        val previousState: AttachmentUploadState,
        val newState: AttachmentUploadState,
        val sideEffect: SideEffect? = null
    ) : StateTransitionResult()
    
    data class Failure(
        val currentState: AttachmentUploadState,
        val event: UploadStateEvent,
        val reason: String
    ) : StateTransitionResult()
}

/**
 * 상태 전환 시 실행될 부수 효과
 */
sealed class SideEffect {
    object NotifyProgress : SideEffect()
    object NotifyCompletion : SideEffect()
    data class NotifyError(val error: String) : SideEffect()
    object CleanupResources : SideEffect()
}

/**
 * 업로드 상태를 원자적으로 관리하는 상태 머신
 * 상태 전환의 일관성을 보장하고 불법적인 상태 전환을 방지합니다.
 */
class UploadStateMachine @Inject constructor() {
    
    private val stateLock = Mutex()
    
    /**
     * 상태 전환을 안전하게 실행합니다.
     * @param currentState 현재 상태
     * @param event 발생한 이벤트
     * @return 상태 전환 결과
     */
    suspend fun transitionState(
        currentState: AttachmentUploadState,
        event: UploadStateEvent
    ): StateTransitionResult = stateLock.withLock {
        
        val result = when {
            // 대기 상태에서의 전환
            currentState.status == MessageAttachmentUploadStatus.PENDING && event == UploadStateEvent.StartUpload -> {
                val newState = currentState.copy(
                    status = MessageAttachmentUploadStatus.UPLOADING,
                    progress = MessageAttachmentUploadProgress.zero(),
                    timestamp = System.currentTimeMillis()
                )
                StateTransitionResult.Success(
                    previousState = currentState,
                    newState = newState,
                    sideEffect = SideEffect.NotifyProgress
                )
            }
            
            currentState.status == MessageAttachmentUploadStatus.PENDING && event is UploadStateEvent.UpdateProgress -> {
                val newState = currentState.withProgress(event.progress)
                StateTransitionResult.Success(
                    previousState = currentState,
                    newState = newState,
                    sideEffect = SideEffect.NotifyProgress
                )
            }
            
            currentState.status == MessageAttachmentUploadStatus.PENDING && event is UploadStateEvent.FailUpload -> {
                val newState = currentState.withError(event.error)
                StateTransitionResult.Success(
                    previousState = currentState,
                    newState = newState,
                    sideEffect = SideEffect.NotifyError(event.error)
                )
            }
            
            // 업로드 중 상태에서의 전환
            currentState.status == MessageAttachmentUploadStatus.UPLOADING && event is UploadStateEvent.UpdateProgress -> {
                val newState = currentState.withProgress(event.progress)
                StateTransitionResult.Success(
                    previousState = currentState,
                    newState = newState,
                    sideEffect = SideEffect.NotifyProgress
                )
            }
            
            currentState.status == MessageAttachmentUploadStatus.UPLOADING && event is UploadStateEvent.CompleteUpload -> {
                val newState = AttachmentUploadState.completed()
                StateTransitionResult.Success(
                    previousState = currentState,
                    newState = newState,
                    sideEffect = SideEffect.NotifyCompletion
                )
            }
            
            currentState.status == MessageAttachmentUploadStatus.UPLOADING && event is UploadStateEvent.FailUpload -> {
                val newState = currentState.withError(event.error)
                StateTransitionResult.Success(
                    previousState = currentState,
                    newState = newState,
                    sideEffect = SideEffect.NotifyError(event.error)
                )
            }
            
            currentState.status == MessageAttachmentUploadStatus.UPLOADING && event == UploadStateEvent.CancelUpload -> {
                val newState = currentState.withError("업로드가 취소되었습니다")
                StateTransitionResult.Success(
                    previousState = currentState,
                    newState = newState,
                    sideEffect = SideEffect.CleanupResources
                )
            }
            
            // 실패 상태에서의 전환 (재시도 허용)
            currentState.status == MessageAttachmentUploadStatus.FAILED && event == UploadStateEvent.RetryUpload -> {
                val newState = AttachmentUploadState.initial()
                StateTransitionResult.Success(
                    previousState = currentState,
                    newState = newState,
                    sideEffect = SideEffect.NotifyProgress
                )
            }
            
            currentState.status == MessageAttachmentUploadStatus.FAILED && event == UploadStateEvent.StartUpload -> {
                val newState = currentState.copy(
                    status = MessageAttachmentUploadStatus.UPLOADING,
                    progress = MessageAttachmentUploadProgress.zero(),
                    errorMessage = null,
                    timestamp = System.currentTimeMillis()
                )
                StateTransitionResult.Success(
                    previousState = currentState,
                    newState = newState,
                    sideEffect = SideEffect.NotifyProgress
                )
            }
            
            // 완료 상태에서는 변경 불가 (불변성 보장)
            currentState.status == MessageAttachmentUploadStatus.COMPLETED -> {
                StateTransitionResult.Failure(
                    currentState = currentState,
                    event = event,
                    reason = "완료된 업로드는 변경할 수 없습니다"
                )
            }
            
            // 기타 불법적인 전환
            else -> {
                StateTransitionResult.Failure(
                    currentState = currentState,
                    event = event,
                    reason = "허용되지 않는 상태 전환입니다: ${currentState.status} -> $event"
                )
            }
        }
        
        // 로깅
        when (result) {
            is StateTransitionResult.Success -> {
                println("DEBUG: State transition: ${result.previousState.status} -> ${result.newState.status}")
            }
            is StateTransitionResult.Failure -> {
                println("WARNING: Invalid state transition: ${result.reason}")
            }
        }
        
        result
    }
    
    /**
     * 상태 전환이 가능한지 미리 확인합니다.
     * @param currentState 현재 상태
     * @param event 발생할 이벤트
     * @return 전환 가능 여부
     */
    fun canTransition(currentState: AttachmentUploadState, event: UploadStateEvent): Boolean {
        return when {
            currentState.status == MessageAttachmentUploadStatus.PENDING && event == UploadStateEvent.StartUpload -> true
            currentState.status == MessageAttachmentUploadStatus.PENDING && event is UploadStateEvent.UpdateProgress -> true
            currentState.status == MessageAttachmentUploadStatus.PENDING && event is UploadStateEvent.FailUpload -> true
            
            currentState.status == MessageAttachmentUploadStatus.UPLOADING && event is UploadStateEvent.UpdateProgress -> true
            currentState.status == MessageAttachmentUploadStatus.UPLOADING && event is UploadStateEvent.CompleteUpload -> true
            currentState.status == MessageAttachmentUploadStatus.UPLOADING && event is UploadStateEvent.FailUpload -> true
            currentState.status == MessageAttachmentUploadStatus.UPLOADING && event == UploadStateEvent.CancelUpload -> true
            
            currentState.status == MessageAttachmentUploadStatus.FAILED && event == UploadStateEvent.RetryUpload -> true
            currentState.status == MessageAttachmentUploadStatus.FAILED && event == UploadStateEvent.StartUpload -> true
            
            currentState.status == MessageAttachmentUploadStatus.COMPLETED -> false // 완료 상태는 불변
            
            else -> false
        }
    }
    
    /**
     * 진행률 값이 유효한지 검증합니다.
     * @param progress 진행률 (0.0 ~ 1.0)
     * @return 유효성 여부
     */
    fun isValidProgress(progress: Float): Boolean {
        return progress >= 0.0f && progress <= 1.0f && !progress.isNaN() && progress.isFinite()
    }
    
    /**
     * 진행률에 따른 자동 상태 전환을 계산합니다.
     * @param currentState 현재 상태
     * @param progress 새로운 진행률
     * @return 권장되는 상태 전환 이벤트
     */
    fun calculateAutoTransition(
        currentState: AttachmentUploadState,
        progress: Float
    ): UploadStateEvent? {
        if (!isValidProgress(progress)) return null
        
        return when {
            progress >= 1.0f && currentState.status == MessageAttachmentUploadStatus.UPLOADING -> {
                UploadStateEvent.CompleteUpload()
            }
            progress > 0.0f && currentState.status == MessageAttachmentUploadStatus.PENDING -> {
                UploadStateEvent.StartUpload
            }
            else -> null
        }
    }
    
    /**
     * 상태 머신의 현재 상태를 검증합니다.
     * @param state 검증할 상태
     * @return 상태 일관성 여부
     */
    fun validateState(state: AttachmentUploadState): Boolean {
        return when (state.status) {
            MessageAttachmentUploadStatus.PENDING -> {
                state.progress.value == 0.0f && state.errorMessage == null
            }
            MessageAttachmentUploadStatus.UPLOADING -> {
                state.progress.value > 0.0f && state.progress.value < 1.0f && state.errorMessage == null
            }
            MessageAttachmentUploadStatus.COMPLETED -> {
                state.progress.value == 1.0f && state.errorMessage == null
            }
            MessageAttachmentUploadStatus.FAILED -> {
                state.errorMessage != null
            }
        }
    }
}