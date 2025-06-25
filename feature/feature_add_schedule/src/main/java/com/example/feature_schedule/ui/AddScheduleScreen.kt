package com.example.feature_schedule.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.project.ProjectName
import com.example.feature_schedule.viewmodel.AddScheduleEvent
import com.example.feature_schedule.viewmodel.AddScheduleUiState
import com.example.feature_schedule.viewmodel.AddScheduleViewModel
import com.example.feature_schedule.viewmodel.ProjectSelectionItem
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * AddScheduleScreen: 일정 추가 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleScreen(
    navigationManger: NavigationManger,
    modifier: Modifier = Modifier,
    viewModel: AddScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddScheduleEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // --- Compose TimePickerDialog 사용 ---

    // Start Time Picker Dialog
    CustomAlertDialogTimePicker(
        showDialog = uiState.isShowStartTimePicker,
        onDismissRequest = { viewModel.requestStartTimePicker(false) },
        onTimeSelected = { hour, minute ->
            viewModel.onStartTimeSelected(hour, minute)
            viewModel.requestStartTimePicker(false) // EndTimePicker 표시 요청
            viewModel.requestEndTimePicker(true) // EndTimePicker 표시 요청
        },
    )

    CustomAlertDialogTimePicker(
        showDialog = uiState.isShowEndTimePicker,
        onDismissRequest = { viewModel.requestEndTimePicker(false) },
        onTimeSelected = { hour, minute ->
            viewModel.onEndTimeSelected(hour, minute)
            viewModel.requestEndTimePicker(false) // EndTimePicker 닫기
        },
    )


    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("일정 추가") },
                navigationIcon = {
                    IconButton(onClick = { navigationManger.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        AddScheduleContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onProjectSelected = viewModel::onProjectSelected,
            onTitleChange = viewModel::onTitleChange,
            onContentChange = viewModel::onContentChange,
            onTimeClick = { println("test"); viewModel.requestStartTimePicker(true)}, // ViewModel에 시작 요청
            onSaveClick = viewModel::onSaveClick
        )
    }
}

/**
 * AddScheduleContent: 일정 추가 UI 요소 (Stateless)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleContent(
    modifier: Modifier = Modifier,
    uiState: AddScheduleUiState,
    onProjectSelected: (ProjectSelectionItem) -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onTimeClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 프로젝트 선택 Dropdown
        ExposedDropdownMenuBox(
            expanded = projectDropdownExpanded,
            onExpandedChange = { projectDropdownExpanded = !projectDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.selectedProject?.name?.value ?: "개인 일정",
                onValueChange = {}, // 읽기 전용
                readOnly = true,
                label = { Text("프로젝트") },
                trailingIcon = { TrailingIcon(expanded = projectDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true) // 메뉴가 TextField 아래에 열리도록 함
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = projectDropdownExpanded,
                onDismissRequest = { projectDropdownExpanded = false }
            ) {
                if (uiState.availableProjects.isEmpty()){
                    DropdownMenuItem(
                        text = { Text("로드된 프로젝트 없음") },
                        onClick = { projectDropdownExpanded = false },
                        enabled = false
                    )
                } else {
                    uiState.availableProjects.forEach { project ->
                        DropdownMenuItem(
                            text = { Text(project.name.value) },
                            onClick = {
                                onProjectSelected(project)
                                projectDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 일정 제목
        OutlinedTextField(
            value = uiState.scheduleTitle,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("일정 제목") },
            singleLine = true,
            isError = uiState.titleError != null // 제목 에러 표시
        )
        if (uiState.titleError != null) {
            Text(uiState.titleError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 시간 선택
        OutlinedTextField(
            value = formatTimeRange(uiState.startTime, uiState.endTime),
            onValueChange = {}, // 직접 수정 불가
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("시간") },
            trailingIcon = {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = "시간 선택",
                    modifier = Modifier.clickable(onClick = {onTimeClick()})
                )
            }, // 시간 아이콘 추가
            isError = uiState.timeError != null // 시간 에러 표시
        )
        if (uiState.timeError != null) {
            Text(uiState.timeError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }


        Spacer(modifier = Modifier.height(16.dp))

        // 일정 내용
        OutlinedTextField(
            value = uiState.scheduleContent,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp), // 여러 줄 입력 가능하도록 높이 조절
            label = { Text("일정 내용 (선택 사항)") },
            maxLines = 5 // 예시: 최대 5줄
        )

        Spacer(modifier = Modifier.weight(1f)) // 완료 버튼을 맨 아래로 밀기

        // 완료 버튼
        Button(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading // 로딩 중 아닐 때만 활성화
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("완료")
            }
        }
    }
}

/**
 * CustomAlertDialogTimePicker: 시간 선택 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAlertDialogTimePicker(
    showDialog: Boolean,
    onDismissRequest: () -> Unit, 
    onTimeSelected: (Int, Int) -> Unit // 시간, 분을 선택했을 때 호출되는 콜백
) {
    if (showDialog) {
        val timePickerState = rememberTimePickerState()
        
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("시간 선택") },
            text = {
                // 시간 선택기 배치
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                    }
                ) { Text("선택") }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) { Text("취소") }
            }
        )
    }
}

// 시간 포맷팅 함수 (시작 시간과 종료 시간)
private fun formatTimeRange(startTime: LocalTime?, endTime: LocalTime?): String {
    if (startTime == null && endTime == null) return "시간을 선택하세요"
    
    val formatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
    val start = startTime?.format(formatter) ?: "시작 시간 없음"
    val end = endTime?.format(formatter) ?: "종료 시간 없음"
    
    return "$start ~ $end"
}

// --- Preview ---
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun AddScheduleContentPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AddScheduleContent(
            uiState = AddScheduleUiState(
                selectedDate = LocalDate.now(),
                availableProjects = listOf(
                    ProjectSelectionItem(DocumentId("p1"), ProjectName("프로젝트 1")),
                    ProjectSelectionItem(DocumentId("p2"), ProjectName("프로젝트 2"))
                ),
                scheduleTitle = "미팅"
            ),
            onProjectSelected = {},
            onTitleChange = {},
            onContentChange = {},
            onTimeClick = {},
            onSaveClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Time Picker Dialog")
@Composable
private fun TimePickerDialogPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        Surface {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CustomAlertDialogTimePicker(
                    showDialog = true,
                    onDismissRequest = {},
                    onTimeSelected = { _, _ -> }
                )
            }
        }
    }
}