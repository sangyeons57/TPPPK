package com.example.core_ui.dialogs.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 확인 다이얼로그의 상태를 나타냅니다.
 *
 * @property isVisible 다이얼로그 표시 여부
 * @property title 다이얼로그 제목
 * @property message 다이얼로그 메시지
 * @property confirmButtonText 확인 버튼 텍스트
 * @property dismissButtonText 취소 버튼 텍스트
 * @property onConfirm 확인 콜백. 호출 후 다이얼로그가 자동으로 닫힙니다.
 * @property onDismiss 취소 콜백. 호출 후 다이얼로그가 자동으로 닫힙니다.
 */
@Stable
data class ConfirmationDialogState(
    val isVisible: Boolean = false,
    val title: String = "확인",
    val message: String = "계속하시겠습니까?",
    val confirmButtonText: String = "확인",
    val dismissButtonText: String = "취소",
    internal val onConfirm: () -> Unit = {}, // ViewModel 내부에서 호출될 실제 로직 포함
    internal val onDismiss: () -> Unit = {}  // ViewModel 내부에서 호출될 실제 로직 포함
)

/**
 * 재사용 가능한 확인 다이얼로그의 로직을 관리하는 ViewModel입니다.
 * 이 ViewModel은 Hilt를 통해 주입받아 사용합니다.
 *
 * 사용 예시:
 * ```
 * // Composable 내부
 * val confirmationViewModel: ConfirmationDialogViewModel = hiltViewModel()
 * val dialogState by confirmationViewModel.dialogState.collectAsState()
 *
 * ConfirmationDialog(
 *     state = dialogState,
 *     onConfirm = confirmationViewModel::handleConfirm, // 또는 dialogState.onConfirm 직접 호출
 *     onDismiss = confirmationViewModel::handleDismiss  // 또는 dialogState.onDismiss 직접 호출
 * )
 *
 * // 다이얼로그 표시하기
 * Button(onClick = {
 *     confirmationViewModel.show(
 *         title = "삭제 확인",
 *         message = "정말로 이 항목을 삭제하시겠습니까?",
 *         onConfirm = { /* 삭제 로직 */ }
 *     )
 * }) {
 *     Text("삭제")
 * }
 * ```
 */
@HiltViewModel
class ConfirmationDialogViewModel @Inject constructor() : ViewModel() {

    private val _dialogState = MutableStateFlow(ConfirmationDialogState())
    val dialogState: StateFlow<ConfirmationDialogState> = _dialogState.asStateFlow()

    /**
     * 확인 다이얼로그를 표시합니다.
     *
     * @param title 다이얼로그 제목
     * @param message 다이얼로그 메시지
     * @param confirmButtonText 확인 버튼 텍스트 (기본값: "확인")
     * @param dismissButtonText 취소 버튼 텍스트 (기본값: "취소")
     * @param onConfirmAction 확인 버튼 클릭 시 실행될 핵심 로직. 이 콜백 실행 후 다이얼로그는 자동으로 닫힙니다.
     * @param onDismissAction 취소 버튼 클릭 시 또는 다이얼로그 외부 클릭 시 실행될 핵심 로직. (기본값: 없음). 이 콜백 실행 후 다이얼로그는 자동으로 닫힙니다.
     */
    fun show(
        title: String,
        message: String,
        confirmButtonText: String = "확인",
        dismissButtonText: String = "취소",
        onConfirmAction: () -> Unit,
        onDismissAction: (() -> Unit)? = null
    ) {
        _dialogState.update {
            it.copy(
                isVisible = true,
                title = title,
                message = message,
                confirmButtonText = confirmButtonText,
                dismissButtonText = dismissButtonText,
                onConfirm = {
                    onConfirmAction()
                    hide()
                },
                onDismiss = {
                    onDismissAction?.invoke()
                    hide()
                }
            )
        }
    }

    /**
     * 확인 다이얼로그를 숨깁니다.
     * 일반적으로 이 메서드는 다이얼로그 UI의 onDismissRequest 나 버튼 콜백 내부에서 직접 호출되기보다는,
     * [show] 메서드에서 설정된 [ConfirmationDialogState.onConfirm] 또는 [ConfirmationDialogState.onDismiss] 콜백의 일부로 호출됩니다.
     */
    fun hide() {
        _dialogState.update { it.copy(isVisible = false) }
    }

    /**
     * UI에서 확인 버튼이 클릭되었을 때 호출됩니다.
     * [dialogState]에 저장된 [ConfirmationDialogState.onConfirm] 람다를 실행합니다.
     */
    fun handleConfirm() {
        _dialogState.value.onConfirm()
    }

    /**
     * UI에서 취소 버튼이 클릭되었거나 다이얼로그가 외부 터치 등으로 닫혔을 때 호출됩니다.
     * [dialogState]에 저장된 [ConfirmationDialogState.onDismiss] 람다를 실행합니다.
     */
    fun handleDismiss() {
        _dialogState.value.onDismiss()
    }
} 