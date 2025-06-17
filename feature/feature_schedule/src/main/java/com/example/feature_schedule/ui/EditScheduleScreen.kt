package com.example.feature_schedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Save // 저장 아이콘
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker // Added
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState // Added
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_navigation.core.AppNavigator
import com.example.core_navigation.destination.AppRoutes // Keep this if other AppRoutes are used
import com.example.core_navigation.extension.REFRESH_SCHEDULE_LIST_KEY // Add this
import com.example.feature_schedule.viewmodel.EditScheduleEvent
import com.example.feature_schedule.viewmodel.EditScheduleUiState
import com.example.feature_schedule.viewmodel.EditScheduleViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.clickable

/**
 * 기존 일정을 수정하는 화면입니다. (Placeholder)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: EditScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is EditScheduleEvent.NavigateBack -> {
                    appNavigator.navigateBack()
                }
                is EditScheduleEvent.SaveSuccessAndRequestBackNavigation -> {
                    appNavigator.setResult(REFRESH_SCHEDULE_LIST_KEY, true) // Modified this line
                    appNavigator.navigateBack()
                }
                is EditScheduleEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // 시작 시간 선택 다이얼로그
    CustomAlertDialogTimePicker(
        showDialog = uiState.isShowStartTimePicker,
        onDismissRequest = { viewModel.requestStartTimePicker(false) },
        onTimeSelected = { hour, minute ->
            viewModel.onStartTimeSelected(hour, minute)
            // 시작 시간 선택 후 자동으로 종료 시간 선택 다이얼로그를 띄우는 로직은 ViewModel에 있음
        },
        initialHour = uiState.startTime?.hour ?: LocalTime.now().hour,
        initialMinute = uiState.startTime?.minute ?: LocalTime.now().minute
    )

    // 종료 시간 선택 다이얼로그
    CustomAlertDialogTimePicker(
        showDialog = uiState.isShowEndTimePicker,
        onDismissRequest = { viewModel.requestEndTimePicker(false) },
        onTimeSelected = viewModel::onEndTimeSelected,
        initialHour = uiState.endTime?.hour ?: uiState.startTime?.plusHours(1)?.hour ?: LocalTime.now().hour, // 종료시간 없으면 시작시간+1시간, 그것도 없으면 현재시간
        initialMinute = uiState.endTime?.minute ?: uiState.startTime?.minute ?: LocalTime.now().minute
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("일정 수정") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로 가기")
                    }
                },
                actions = {
                    if (!uiState.isLoading) { // 로딩 중 아닐 때만 저장 버튼 표시
                        IconButton(onClick = { viewModel.onSaveClicked() }, enabled = !uiState.isSaving) {
                            Icon(Icons.Filled.Save, "저장")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        EditScheduleContent(
            paddingValues = paddingValues,
            uiState = uiState,
            onTitleChanged = viewModel::onTitleChanged,
            onContentChanged = viewModel::onContentChanged,
            onTimeClick = { viewModel.onTimeClick() },
            onSaveClicked = viewModel::onSaveClicked
        )
    }
}

/**
 * 일정 수정 화면의 내용을 구성합니다.
 */
@Composable
fun EditScheduleContent(
    paddingValues: PaddingValues,
    uiState: EditScheduleUiState,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onTimeClick: () -> Unit,
    onSaveClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
                Text(
                    text = "오류: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.isSaving) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    OutlinedTextField(
                        value = uiState.title.value,
                        onValueChange = onTitleChanged,
                        label = { Text("일정 제목") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !uiState.isSaving
                    )

                    OutlinedTextField(
                        value = uiState.content.value,
                        onValueChange = onContentChanged,
                        label = { Text("상세 내용 (선택 사항)") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        enabled = !uiState.isSaving
                    )

                    // 시간 선택 (AddScheduleScreen.kt 참고)
                    OutlinedTextField(
                        value = formatTimeRange(uiState.startTime, uiState.endTime),
                        onValueChange = {}, // 직접 수정 불가
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onTimeClick), // 클릭 시 onTimeClick 호출
                        label = { Text("시간") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.AccessTime, // 시계 아이콘
                                contentDescription = "시간 선택",
                                modifier = Modifier.clickable(onClick = onTimeClick)
                            )
                        },
                        enabled = !uiState.isSaving
                        // isError = uiState.timeError != null // TODO: ViewModel에 시간 관련 에러 상태 추가 필요
                    )
                    // if (uiState.timeError != null) { // TODO: ViewModel에 시간 관련 에러 상태 추가 필요
                    // Text(uiState.timeError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    // }

                    Spacer(modifier = Modifier.weight(1f)) // 버튼을 하단으로 밀기

                    Button(
                        onClick = onSaveClicked,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving && uiState.title.value.isNotBlank()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("저장 중...")
                        } else {
                            Text("일정 저장")
                        }
                    }
                }
            }
        }
    }
}

/**
 * CustomAlertDialogTimePicker: 시간 선택 다이얼로그 (AddScheduleScreen.kt에서 가져옴)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAlertDialogTimePicker(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit, // 시간, 분을 선택했을 때 호출되는 콜백
    initialHour: Int, // 초기 시간 추가
    initialMinute: Int // 초기 분 추가
) {
    if (showDialog) {
        val timePickerState = rememberTimePickerState( // Corrected typo and uncommented
            initialHour = initialHour, // 전달받은 초기 시간 사용
            initialMinute = initialMinute, // 전달받은 초기 분 사용
            is24Hour = false // 24시간 형식이면 true, or true if you prefer
        )

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
                    TimePicker(state = timePickerState) // Uncommented
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

// 시간 포맷팅 함수 (시작 시간과 종료 시간) (AddScheduleScreen.kt에서 가져옴)
private fun formatTimeRange(startTime: LocalTime?, endTime: LocalTime?): String {
    if (startTime == null && endTime == null) return "시간을 선택하세요"

    val formatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
    val start = startTime?.format(formatter) ?: "시작 시간 없음"
    val end = endTime?.format(formatter) ?: "종료 시간 없음"

    return "$start ~ $end"
} 