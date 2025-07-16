package com.example.feature_home.ui.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.example.feature_home.viewmodel.HomeEvent
import com.example.feature_home.viewmodel.HomeUiState
import com.example.feature_home.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * HomeScreen의 부수효과(Side Effects)를 관리하는 컴포넌트
 */
@Composable
fun HomeScreenEffects(
    viewModel: HomeViewModel,
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState,
    dialogStates: DialogStates,
    onDialogStateChange: (DialogStates) -> Unit
) {
    rememberCoroutineScope()

    // 이벤트 처리 (스낵바, 다이얼로그 등)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is HomeEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }

                is HomeEvent.ShowAddProjectDialog -> {
                    snackbarHostState.showSnackbar("프로젝트 추가 다이얼로그 (미구현)")
                }

                is HomeEvent.ShowAddProjectElementDialog -> {
                    onDialogStateChange(
                        dialogStates.copy(
                            showAddProjectElementDialog = true,
                            currentProjectIdForDialog = event.projectId
                        )
                    )
                }

                is HomeEvent.ProjectDeleted -> {
                    // 삭제된 프로젝트에 대한 사용자 친화적인 메시지 표시
                    val message = "프로젝트 '${event.projectName}'이(가) 삭제되어 목록에서 제거되었습니다."
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Long
                    )
                }


                
                is HomeEvent.ShowReorderProjectStructureDialog -> {
                    onDialogStateChange(
                        dialogStates.copy(
                            showReorderProjectStructureDialog = true
                        )
                    )
                }
            }
        }
    }

    // 에러 메시지 스낵바로 표시
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != "default" && uiState.errorMessage.isNotBlank()) {
            snackbarHostState.showSnackbar(
                message = uiState.errorMessage,
                duration = SnackbarDuration.Short
            )
            viewModel.errorMessageShown()
        }
    }
}