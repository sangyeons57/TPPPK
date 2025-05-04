package com.example.feature_schedule.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.* // Material 3 컴포넌트 사용
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_schedule.viewmodel.AddScheduleEvent
import com.example.feature_schedule.viewmodel.AddScheduleUiState
import com.example.feature_schedule.viewmodel.AddScheduleViewModel
import com.example.feature_schedule.viewmodel.ProjectSelectionItem
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar // 초기 시간 설정을 위해 사용
import java.util.Locale
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import kotlinx.coroutines.delay

/**
 * AddScheduleScreen: 일정 추가 화면 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleScreen(
    modifier: Modifier = Modifier,
    viewModel: AddScheduleViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // val context = LocalContext.current // Compose TimePickerDialog는 Context가 직접 필요하지 않음


    // 이벤트 처리
    LaunchedEffect(viewModel) { 
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddScheduleEvent.NavigateBack -> onNavigateBack()
                is AddScheduleEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
            }
        }
    }

    // 저장 성공 시 자동 뒤로가기
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            // 스낵바가 보이도록 잠시 대기
            delay(1000)
            viewModel.navigateBack()
        }
    }

    // --- Compose TimePickerDialog 사용 ---

    // Start Time Picker Dialog
    CustomAlertDialogTimePicker(
        showDialog = uiState.isShowStartTimePicker,
        onDismissRequest = { viewModel.requestStartTimePicker(false) },
        onTimeSelected = { hour, minute ->
            viewModel.onStartTimeSelected(hour, minute)
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
                    IconButton(onClick = onNavigateBack) { // 뒤로가기
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
                value = uiState.selectedProject?.name ?: "개인 일정",
                onValueChange = {}, // 읽기 전용
                readOnly = true,
                label = { Text("프로젝트") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectDropdownExpanded) },
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
                            text = { Text(project.name) },
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
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp), // 여러 줄 입력 가능하도록 높이 조절
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

// 시간 범위 포맷 함수
fun formatTimeRange(start: LocalTime?, end: LocalTime?): String {
    val formatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN) // 예: 오후 3:00
    return when {
        start != null && end != null -> "${start.format(formatter)} ~ ${end.format(formatter)}"
        start != null                -> "${start.format(formatter)} ~ 시간 미정"
        else                         -> "시간 미정"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAlertDialogTimePicker(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    initialHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    initialMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
    is24Hour: Boolean = true, // 기본값을 true로 설정
    title: String = "시간 선택",
    confirmButtonText: String = "확인",
    dismissButtonText: String = "취소",
    useTimeInput: Boolean = false) {
    if (showDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = is24Hour // 파라미터로 받은 is24Hour 사용 (기본값 true)
        )

        AlertDialog(
            onDismissRequest = onDismissRequest,
            // 확인 버튼
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                        onDismissRequest() // 확인 후 다이얼로그 닫기
                    }
                ) {
                    Text(confirmButtonText)
                }
            },
            // 취소 버튼
            dismissButton = {
                TextButton(
                    onClick = onDismissRequest // 취소 시 onDismissRequest 호출
                ) {
                    Text(dismissButtonText)
                }
            },
            // 제목
            title = { Text(text = title) },
            // 내용 영역 (TimePicker 또는 TimeInput 배치)
            text = {
                // TimePicker/TimeInput을 가운데 정렬하거나 패딩을 주기 위해 Box 사용 (선택 사항)
                Box(
                    modifier = Modifier.fillMaxWidth(), // 너비를 채우도록 설정
                    contentAlignment = Alignment.Center // 내부 컨텐츠 중앙 정렬
                ) {
                    if (useTimeInput) {
                        TimeInput(state = timePickerState)
                    } else {
                        TimePicker(state = timePickerState)
                    }
                }
            },
            modifier = Modifier.padding(vertical = 16.dp) // 다이얼로그 내부 상하 패딩 조절 (선택 사항)
        )
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun AddScheduleContentPreview() {
    val sampleProjects = listOf(
        ProjectSelectionItem("1", "개인 프로젝트"),
        ProjectSelectionItem("2", "팀 프로젝트 A")
    )
    val previewState = AddScheduleUiState(
        availableProjects = sampleProjects,
        selectedDate = LocalDate.now()
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AddScheduleContent(
            uiState = previewState,
            onProjectSelected = {},
            onTitleChange = {},
            onContentChange = {},
            onTimeClick = {},
            onSaveClick = {}
        )
    }
}

@Preview(showBackground = true, name="Add Schedule with Time")
@Composable
private fun AddScheduleContentWithTimePreview() {
    val sampleProjects = listOf(
        ProjectSelectionItem("1", "개인 프로젝트"),
        ProjectSelectionItem("2", "팀 프로젝트 A")
    )
    val previewState = AddScheduleUiState(
        availableProjects = sampleProjects,
        selectedProject = sampleProjects[0],
        scheduleTitle = "미팅 준비",
        startTime = LocalTime.of(14, 0),
        endTime = LocalTime.of(15, 30),
        selectedDate = LocalDate.now()
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        AddScheduleContent(
            uiState = previewState,
            onProjectSelected = {},
            onTitleChange = {},
            onContentChange = {},
            onTimeClick = {},
            onSaveClick = {}
        )
    }
}