package com.example.teamnovapersonalprojectprojectingkotlin.feature_schedule.ui


import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.teamnovapersonalprojectprojectingkotlin.feature_schedule.viewmodel.AddScheduleEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_schedule.viewmodel.AddScheduleUiState
import com.example.teamnovapersonalprojectprojectingkotlin.feature_schedule.viewmodel.AddScheduleViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.feature_schedule.viewmodel.ProjectSelectionItem
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

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
    val context = LocalContext.current

    // TimePickerDialog 표시 상태
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddScheduleEvent.NavigateBack -> onNavigateBack()
                is AddScheduleEvent.ShowStartTimePicker -> showStartTimePicker = true
                is AddScheduleEvent.ShowEndTimePicker -> showEndTimePicker = true
                is AddScheduleEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // 저장 성공 시 뒤로 가기
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNavigateBack()
        }
    }

    // Start Time Picker Dialog
    if (showStartTimePicker) {
        val calendar = Calendar.getInstance()
        val currentHour = uiState.startTime?.hour ?: calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = uiState.startTime?.minute ?: calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                viewModel.onStartTimeSelected(hour, minute)
                showStartTimePicker = false // Picker 닫기
                viewModel.requestEndTimePicker() // EndTimePicker 표시 요청 (ViewModel 통해)
            },
            currentHour,
            currentMinute,
            true // 24시간 형식 사용
        ).apply {
            setOnDismissListener { showStartTimePicker = false } // 외부 클릭 등으로 닫혔을 때
            show()
        }
    }

    // End Time Picker Dialog
    if (showEndTimePicker) {
        val calendar = Calendar.getInstance()
        // 시작 시간이 있으면 그 이후, 없으면 현재 시간 기준
        val initialHour = uiState.endTime?.hour ?: uiState.startTime?.hour ?: calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = uiState.endTime?.minute ?: uiState.startTime?.minute ?: calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                viewModel.onEndTimeSelected(hour, minute)
                showEndTimePicker = false // Picker 닫기
            },
            initialHour,
            initialMinute,
            true // 24시간 형식 사용
        ).apply {
            setOnDismissListener { showEndTimePicker = false } // 외부 클릭 등으로 닫혔을 때
            show()
        }
    }


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
            onTimeClick = viewModel::requestStartTimePicker, // ViewModel에 시작 요청
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
                value = uiState.selectedProject?.name ?: "프로젝트 선택",
                onValueChange = {}, // 읽기 전용
                readOnly = true,
                label = { Text("프로젝트") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor() // 메뉴가 TextField 아래에 열리도록 함
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
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTimeClick), // 클릭 시 TimePicker 표시
            label = { Text("시간") },
            trailingIcon = { Icon(Icons.Default.AccessTime, contentDescription = "시간 선택") }, // 시간 아이콘 추가 (material-icons-extended 필요)
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
        start != null && end != null -> "${start.format(formatter)} ~ ${end.format(formatter)}" // -> 추가
        start != null                -> "${start.format(formatter)} ~ 시간 미정"                 // -> 추가
        else                         -> "시간 미정"
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
private fun AddScheduleContentPreview() {
    val sampleProjects = listOf(
        ProjectSelectionItem(1, "개인 프로젝트"),
        ProjectSelectionItem(2, "팀 프로젝트 A")
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
        ProjectSelectionItem(1, "개인 프로젝트"),
        ProjectSelectionItem(2, "팀 프로젝트 A")
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