package com.example.feature_main.ui.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.core_ui.dialogs.ui.ProjectStructureEditDialog
import com.example.core_ui.dialogs.viewmodel.ProjectStructureEditDialogViewModel
import com.example.core_ui.dialogs.viewmodel.ProjectStructureEditEvent
import kotlinx.coroutines.flow.collectLatest

/**
 * 프로젝트 구조 편집 다이얼로그 래퍼 컴포넌트
 * 
 * @param projectId 편집할 프로젝트 ID
 * @param onDismiss 다이얼로그가 닫힐 때 호출되는 콜백
 * @param onStructureUpdated 구조가 업데이트되었을 때 호출되는 콜백
 * @param onShowSnackbar 스낵바 메시지를 표시할 때 호출되는 콜백
 */
@Composable
fun ProjectStructureEditDialogWrapper(
    projectId: String,
    onDismiss: () -> Unit,
    onStructureUpdated: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    viewModel: ProjectStructureEditDialogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ProjectStructureEditEvent.SavedChanges -> {
                    onStructureUpdated()
                    onShowSnackbar("프로젝트 구조가 업데이트되었습니다.")
                    onDismiss()
                }
                is ProjectStructureEditEvent.Dismissed -> {
                    onDismiss()
                }
                is ProjectStructureEditEvent.Error -> {
                    onShowSnackbar(event.message)
                }
                is ProjectStructureEditEvent.ShowSnackbar -> {
                    onShowSnackbar(event.message)
                }
                else -> {
                    // 다른 이벤트는 다이얼로그 내부에서 처리됨
                }
            }
        }
    }
    
    // 에러 메시지 표시
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            onShowSnackbar(error)
        }
    }
    
    // 수정된 다이얼로그 호출
    ProjectStructureEditDialog(
        onDismiss = onDismiss,
        viewModel = viewModel,
        projectId = projectId
    )
} 