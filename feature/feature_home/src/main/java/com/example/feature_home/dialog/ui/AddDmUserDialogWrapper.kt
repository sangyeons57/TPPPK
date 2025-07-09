package com.example.feature_home.dialog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserName
import com.example.feature_home.dialog.viewmodel.AddDmUserDialogViewModel
import com.example.feature_home.dialog.viewmodel.AddDmUserEvent
import kotlinx.coroutines.flow.collectLatest

/**
 * DM 추가 다이얼로그 래퍼 컴포넌트
 * 
 * @param onDismiss 다이얼로그가 닫힐 때 호출되는 콜백
 * @param onNavigateToDm DM 채팅 화면으로 이동할 때 호출되는 콜백. 채널 ID를 파라미터로 받음
 * @param onShowSnackbar 스낵바를 표시할 때 호출되는 콜백. 메시지를 파라미터로 받음
 */
@Composable
fun AddDmUserDialogWrapper(
    onDismiss: () -> Unit,
    onNavigateToDm: (DocumentId) -> Unit,
    onShowSnackbar: (String) -> Unit,
    viewModel: AddDmUserDialogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddDmUserEvent.NavigateToDmChat -> {
                    onNavigateToDm(event.channelId)
                    onDismiss()
                }
                is AddDmUserEvent.ShowSnackbar -> {
                    onShowSnackbar(event.message)
                }
                is AddDmUserEvent.DismissDialog -> {
                    onDismiss()
                }
            }
        }
    }
    
    AddDmUserDialog(
        onDismiss = viewModel::dismiss,
        onSearch = viewModel::searchUser,
        username = uiState.username,
        onUsernameChange = { viewModel.onUsernameChange(UserName(it)) },
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        isUserBlocked = uiState.isUserBlocked
    )
} 