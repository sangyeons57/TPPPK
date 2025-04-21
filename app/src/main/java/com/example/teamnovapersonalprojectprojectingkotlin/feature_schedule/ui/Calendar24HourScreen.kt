package com.example.teamnovapersonalprojectprojectingkotlin.feature_schedule.ui

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.teamnovapersonalprojectprojectingkotlin.feature_schedule.viewmodel.Calendar24HourEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_schedule.viewmodel.Calendar24HourUiState
import com.example.teamnovapersonalprojectprojectingkotlin.feature_schedule.viewmodel.Calendar24HourViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.feature_schedule.viewmodel.ScheduleItem24Hour
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Calendar24HourScreen: 특정 날짜의 24시간 스케줄 표시 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Calendar24HourScreen(
    modifier: Modifier = Modifier,
    viewModel: Calendar24HourViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToAddSchedule: (Int, Int, Int) -> Unit, // year, month, day 전달
    onNavigateToScheduleDetail: (String) -> Unit // scheduleId 전달
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentUiState = uiState
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditDialog by remember { mutableStateOf<String?>(null) } // 편집 다이얼로그 표시 상태

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is Calendar24HourEvent.NavigateBack -> onNavigateBack()
                is Calendar24HourEvent.NavigateToAddSchedule -> {
                    if (currentUiState is Calendar24HourUiState.Success) {
                        val date = currentUiState.selectedDate
                        if (date != null) {
                            onNavigateToAddSchedule(date.year, date.monthValue, date.dayOfMonth)
                        }
                    }
                }
                is Calendar24HourEvent.NavigateToScheduleDetail -> onNavigateToScheduleDetail(event.scheduleId)
                is Calendar24HourEvent.ShowScheduleEditDialog -> showEditDialog = event.scheduleId
                is Calendar24HourEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val titleText = when (currentUiState) {
                        is Calendar24HourUiState.Success ->
                            currentUiState.selectedDate?.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")) ?: "날짜 정보 없음"
                        is Calendar24HourUiState.Loading -> "로딩 중..."
                        is Calendar24HourUiState.Error -> "오류"
                    }
                    Text(titleText)
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onAddScheduleClick) {
                        Icon(Icons.Filled.Add, contentDescription = "일정 추가")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (currentUiState) {
            is Calendar24HourUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Calendar24HourUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("오류 발생: ${currentUiState.message}")
                }
            }
            is Calendar24HourUiState.Success -> {
                Calendar24HourContent(
                    modifier = Modifier.padding(paddingValues),
                    schedules = currentUiState.schedules,
                    onScheduleClick = viewModel::onScheduleClick,
                    onScheduleLongClick = viewModel::onScheduleLongClick
                )
            }
        }
    }

    // 일정 수정/삭제 다이얼로그 (Dialog는 Screen 레벨에서 관리)
    if (showEditDialog != null) {
        ScheduleEditDialog(
            scheduleId = showEditDialog!!,
            onDismiss = { showEditDialog = null },
            onDeleteClick = {
                viewModel.deleteSchedule(it)
                showEditDialog = null
            },
            onEditClick = {
                // TODO: 일정 수정 화면으로 이동하는 로직 구현 필요
                println("Edit schedule $it (Not implemented)")
                showEditDialog = null
            }
        )
    }
}

/**
 * Calendar24HourContent: 24시간 타임라인 및 스케줄 표시 (Stateless)
 * TimeRangeViewGroup을 Compose 방식으로 재구현
 */
@Composable
fun Calendar24HourContent(
    modifier: Modifier = Modifier,
    schedules: List<ScheduleItem24Hour>,
    onScheduleClick: (String) -> Unit,
    onScheduleLongClick: (String) -> Unit
) {
    val hourHeight = 60.dp // 1시간 높이
    val totalHeight = hourHeight * 24 // 24시간 전체 높이
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Box(modifier = modifier.verticalScroll(rememberScrollState())) {
        Canvas(modifier = Modifier.fillMaxWidth().height(totalHeight)) {
            drawTimeline(hourHeight, textMeasurer)
            drawSchedules(schedules, hourHeight, onScheduleClick, onScheduleLongClick, textMeasurer)
        }
    }
}

// 시간선 및 시간 텍스트 그리기
fun DrawScope.drawTimeline(hourHeight: Dp, textMeasurer: TextMeasurer) {
    val hourHeightPx = hourHeight.toPx()
    val timelineStartPadding = 60.dp.toPx() // 시간 텍스트를 위한 시작 패딩

    for (hour in 0..24) {
        val y = hour * hourHeightPx
        drawLine(
            color = Color.LightGray,
            start = Offset(timelineStartPadding, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx()
        )
        if (hour < 24) {
            val timeText = LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("HH:mm"))
            val textLayoutResult = textMeasurer.measure(
                text = AnnotatedString(timeText),
                style = TextStyle(fontSize = 12.sp, color = Color.Gray)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(timelineStartPadding - textLayoutResult.size.width - 8.dp.toPx(), y - textLayoutResult.size.height / 2)
            )
        }
    }
    // 시간축 세로선
    drawLine(
        color = Color.Gray,
        start = Offset(timelineStartPadding, 0f),
        end = Offset(timelineStartPadding, hourHeightPx * 24),
        strokeWidth = 2.dp.toPx()
    )
}

// 스케줄 블록 그리기
fun DrawScope.drawSchedules(
    schedules: List<ScheduleItem24Hour>,
    hourHeight: Dp,
    onScheduleClick: (String) -> Unit,
    onScheduleLongClick: (String) -> Unit,
    textMeasurer: TextMeasurer
) {
    val hourHeightPx = hourHeight.toPx()
    val timelineStartPadding = 60.dp.toPx()
    val schedulePadding = 4.dp.toPx()

    schedules.forEach { schedule ->
        val startY = schedule.startTime.toSecondOfDay() / 3600f * hourHeightPx
        val endY = schedule.endTime.toSecondOfDay() / 3600f * hourHeightPx
        val scheduleHeight = endY - startY
        val scheduleWidth = size.width - timelineStartPadding - schedulePadding * 2

        val rectTopLeft = Offset(timelineStartPadding + schedulePadding, startY)
        val rectSize = androidx.compose.ui.geometry.Size(scheduleWidth, scheduleHeight)
        val rect = androidx.compose.ui.geometry.Rect(rectTopLeft, rectSize)

        // 스케줄 블록 배경
        drawRect(
            color = Color(schedule.color),
            topLeft = rectTopLeft,
            size = rectSize,
            // TODO: Round corners?
        )
        // 스케줄 테두리
        drawRect(
            color = Color.Black,
            topLeft = rectTopLeft,
            size = rectSize,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )

        // 스케줄 텍스트
        val textLayoutResult = textMeasurer.measure(
            text = AnnotatedString(schedule.title),
            style = TextStyle(fontSize = 14.sp, color = Color.Black), // TODO: Color contrast
            constraints = androidx.compose.ui.unit.Constraints(
                maxWidth = scheduleWidth.toInt() - (2 * 4.dp.toPx()).toInt() // 텍스트 좌우 패딩 고려
            ),
            maxLines = (scheduleHeight / (14.sp.toPx() * 1.2f)).toInt().coerceAtLeast(1), // 높이에 따라 최대 줄 수 계산
            overflow = TextOverflow.Ellipsis
        )
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(rectTopLeft.x + 4.dp.toPx(), rectTopLeft.y + 4.dp.toPx())
        )

        // --- 클릭 리스너 추가 (Canvas에서는 직접 추가 불가, Box 위에서 처리 필요) ---
        // Canvas 외부에서 Modifier.pointerInput 사용하여 영역 감지 및 콜백 호출
    }
}

// 일정 수정/삭제 다이얼로그
@Composable
fun ScheduleEditDialog(
    scheduleId: String,
    onDismiss: () -> Unit,
    onDeleteClick: (String) -> Unit,
    onEditClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("일정 편집") },
        text = { Text("이 일정을 수정하거나 삭제하시겠습니까?") },
        confirmButton = {
            TextButton(onClick = { onEditClick(scheduleId) }) {
                Text("수정")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDeleteClick(scheduleId) }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        }
    )
}


// --- Preview ---
@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Calendar24HourScreenPreview() {
    val sampleSchedules = listOf(
        ScheduleItem24Hour("1", "팀 회의", LocalTime.of(10, 0), LocalTime.of(11, 30), 0xFFEF5350),
        ScheduleItem24Hour("2", "점심 약속", LocalTime.of(12, 30), LocalTime.of(13, 30), 0xFF66BB6A),
        ScheduleItem24Hour("3", "개인 프로젝트", LocalTime.of(15, 0), LocalTime.of(17, 0), 0xFF42A5F5)
    )
    val previewState = Calendar24HourUiState.Success(LocalDate.now(), sampleSchedules)

    TeamnovaPersonalProjectProjectingKotlinTheme {
        // Scaffold 포함된 전체 화면 미리보기 (이벤트 핸들러는 빈 람다)
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(previewState.selectedDate?.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")) ?: "") },
                    navigationIcon = { IconButton({}) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "") } },
                    actions = { IconButton({}) { Icon(Icons.Filled.Add, "") } }
                )
            }
        ) { paddingValues ->
            Calendar24HourContent(
                modifier = Modifier.padding(paddingValues),
                schedules = previewState.schedules,
                onScheduleClick = {},
                onScheduleLongClick = {}
            )
        }
    }
}