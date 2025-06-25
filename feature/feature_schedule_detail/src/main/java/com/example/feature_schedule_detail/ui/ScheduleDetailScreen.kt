package com.example.feature_schedule_detail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle
import com.example.feature_schedule_detail.viewmodel.ScheduleDetailEvent
import com.example.feature_schedule_detail.viewmodel.ScheduleDetailItem
import com.example.feature_schedule_detail.viewmodel.ScheduleDetailUiState
import com.example.feature_schedule_detail.viewmodel.ScheduleDetailViewModel
import kotlinx.coroutines.flow.collectLatest

const val REFRESH_SCHEDULE_LIST_KEY = "refresh_schedule_list"

/**
 * ScheduleDetailScreen: 일정 상세 정보 표시 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDetailScreen(
    navigationManger: NavigationManger,
    modifier: Modifier = Modifier,
    viewModel: ScheduleDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 화면이 표시될 때마다 데이터 새로고침 
    LaunchedEffect(Unit) {
        viewModel.refreshScheduleDetails() // 일정 상세 데이터 새로고침
    }

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ScheduleDetailEvent.ShowDeleteConfirmDialog -> showDeleteDialog = true
                is ScheduleDetailEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // 삭제 성공 시 뒤로 가기
    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            navigationManger.navigateBackWithResult(
                REFRESH_SCHEDULE_LIST_KEY,
                true
            ) // Modified this line
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("일정 상세") },
                navigationIcon = {
                    DebouncedBackButton(onClick = { navigationManger.navigateBack() })
                },
                actions = {
                    // 로딩 중이 아니고, 스케줄 정보가 있을 때만 버튼 표시
                    if (!uiState.isLoading && uiState.scheduleDetail != null) {
                        IconButton(onClick = viewModel::onEditClick) {
                            Icon(Icons.Filled.Edit, contentDescription = "수정")
                        }
                        IconButton(onClick = viewModel::onDeleteClick) {
                            Icon(Icons.Filled.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        ScheduleDetailContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState
        )
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("일정 삭제") },
            text = { Text("정말로 이 일정을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

/**
 * ScheduleDetailContent: 상세 정보 UI 요소 (Stateless)
 */
@Composable
fun ScheduleDetailContent(
    modifier: Modifier = Modifier,
    uiState: ScheduleDetailUiState
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
                Text(
                    text = "오류: ${uiState.error}",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            uiState.scheduleDetail != null -> {
                val schedule = uiState.scheduleDetail
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp) // 패딩 조정
                ) {
                    // 제목
                    Text(
                        text = schedule.title.value,
                        style = MaterialTheme.typography.headlineMedium, // 제목 스타일 조정
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 프로젝트 이름 (있을 경우)
                    if (schedule.projectName != null) {
                        Text(
                            text = schedule.projectName,
                            style = MaterialTheme.typography.titleMedium, // 프로젝트 이름 스타일 조정
                            color = MaterialTheme.colorScheme.secondary // 보조 색상 사용
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Spacer(modifier = Modifier.height(8.dp)) // 프로젝트 이름 없을 때 간격 조정
                    }


                    // 날짜
                    Text(
                        text = schedule.date,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 시간
                    Text(
                        text = schedule.time,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline // 약간 흐린 색상
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 구분선
                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(16.dp))

                    // 내용
                    Text(
                        text = schedule.content.value ?: "내용 없음",
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp // 줄 간격 조정
                    )
                    Spacer(modifier = Modifier.height(16.dp)) // 하단 여백
                }
            }
            else -> {
                // 스케줄 정보가 없는 경우 (오류는 아니지만 데이터가 null)
                Text(
                    text = "일정 정보를 불러올 수 없습니다.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun ScheduleDetailContentPreview() {
    val previewSchedule = ScheduleDetailItem(
        id = DocumentId("test"),
        title = ScheduleTitle("프리뷰 일정 제목"),
        projectName = "샘플 프로젝트",
        date = "2025년 4월 15일 (화)",
        time = "오후 2:00 ~ 오후 3:30",
        content = ScheduleContent("이것은 프리뷰를 위한 일정 상세 내용입니다.\n여러 줄에 걸쳐서 표시될 수 있습니다.\nCompose로 마이그레이션 중입니다.")
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ScheduleDetailContent(
            uiState = ScheduleDetailUiState(isLoading = false, scheduleDetail = previewSchedule)
        )
    }
}

@Preview(showBackground = true, name = "Schedule Detail Loading")
@Composable
private fun ScheduleDetailContentLoadingPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ScheduleDetailContent(uiState = ScheduleDetailUiState(isLoading = true))
    }
}

@Preview(showBackground = true, name = "Schedule Detail Error")
@Composable
private fun ScheduleDetailContentErrorPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        ScheduleDetailContent(uiState = ScheduleDetailUiState(
            isLoading = false,
            error = "네트워크 연결 오류"
        )
        )
    }
}