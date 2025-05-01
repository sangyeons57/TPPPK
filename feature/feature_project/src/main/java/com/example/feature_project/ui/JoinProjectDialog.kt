package com.example.teamnovapersonalprojectprojectingkotlin.feature_project.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project.viewmodel.JoinProjectDialogEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project.viewmodel.JoinProjectDialogUiState
import com.example.teamnovapersonalprojectprojectingkotlin.feature_project.viewmodel.JoinProjectDialogViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * JoinProjectDialog: 초대 토큰을 기반으로 프로젝트 정보를 보여주고 참여를 유도하는 다이얼로그 (Stateful)
 *
 * @param token 초대 토큰 (외부에서 전달받음)
 * @param onDismissRequest 다이얼로그 닫기 요청 콜백
 * @param onJoinSuccess 프로젝트 참여 성공 시 호출될 콜백 (예: 화면 새로고침)
 * @param viewModel ViewModel 인스턴스 (Hilt 주입)
 */
@Composable
fun JoinProjectDialog(
    token: String, // 초대 토큰은 필수
    onDismissRequest: () -> Unit,
    onJoinSuccess: () -> Unit, // 참여 성공 후 액션
    viewModel: JoinProjectDialogViewModel = hiltViewModel()
) {
    // ViewModel에 토큰 전달 (초기 1회)
    LaunchedEffect(token) {
        viewModel.setToken(token)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 이벤트 처리 (스낵바, 성공 시 닫기 등)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is JoinProjectDialogEvent.DismissDialog -> onDismissRequest()
                is JoinProjectDialogEvent.ShowSnackbar -> {
                    // TODO: 스낵바 표시 (호출한 Screen에서 처리 권장)
                    println("Snackbar: ${event.message}")
                }
                is JoinProjectDialogEvent.JoinSuccess -> {
                    onJoinSuccess() // 성공 콜백 호출
                    onDismissRequest() // 다이얼로그 닫기
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = MaterialTheme.shapes.large) {
            JoinProjectDialogContent(
                uiState = uiState,
                onJoinClick = viewModel::joinProject, // 참여 버튼 클릭 시 ViewModel 함수 호출
                onDismiss = onDismissRequest // 취소/닫기 버튼 클릭
            )
        }
    }
}

/**
 * JoinProjectDialogContent: 프로젝트 참여 다이얼로그 내부 UI (Stateless)
 */
@Composable
fun JoinProjectDialogContent(
    uiState: JoinProjectDialogUiState,
    onJoinClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 로딩 중 또는 에러 발생 시 처리
        when {
            uiState.isLoading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("초대 정보 확인 중...", style = MaterialTheme.typography.bodyMedium)
            }
            uiState.error != null -> {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss) { // 에러 시 닫기 버튼만 표시
                    Text("확인")
                }
            }
            uiState.projectInfo != null -> { // 프로젝트 정보 로드 성공
                val project = uiState.projectInfo
                Text(
                    text = project.projectName,
                    style = MaterialTheme.typography.headlineSmall, // 프로젝트 이름 크게
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                // TODO: 멤버 수 등 추가 정보 표시 (XML에는 있었음)
                Text(
                    text = "멤버 ${project.memberCount}명", // 예시
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onJoinClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isJoining // 참여 요청 중 아닐 때 활성화
                ) {
                    if (uiState.isJoining) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("참여하기")
                    }
                }
                // 취소 버튼 (선택적)
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("취소")
                }
            }
            else -> {
                // 초기 상태 또는 알 수 없는 상태
                Text("초대 정보를 가져올 수 없습니다.", color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss) { Text("확인") }
            }
        }
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun JoinProjectDialogPreview_Success() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            JoinProjectDialogContent(
                uiState = JoinProjectDialogUiState(
                    projectInfo = JoinProjectDialogUiState.ProjectInfo("멋진 새 프로젝트", 15)
                ),
                onJoinClick = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinProjectDialogPreview_Loading() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            JoinProjectDialogContent(
                uiState = JoinProjectDialogUiState(isLoading = true),
                onJoinClick = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinProjectDialogPreview_Error() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            JoinProjectDialogContent(
                uiState = JoinProjectDialogUiState(error = "유효하지 않은 초대입니다."),
                onJoinClick = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinProjectDialogPreview_Joining() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            JoinProjectDialogContent(
                uiState = JoinProjectDialogUiState(
                    projectInfo = JoinProjectDialogUiState.ProjectInfo("참여 중인 프로젝트", 10),
                    isJoining = true
                ),
                onJoinClick = {},
                onDismiss = {}
            )
        }
    }
}