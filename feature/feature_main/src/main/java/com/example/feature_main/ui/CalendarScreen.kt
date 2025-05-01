package com.example.teamnovapersonalprojectprojectingkotlin.feature_main.ui

import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.CalendarEvent
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.CalendarUiState
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.CalendarViewModel
import com.example.teamnovapersonalprojectprojectingkotlin.feature_main.viewmodel.ScheduleItem
import com.example.teamnovapersonalprojectprojectingkotlin.ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add // 추가 버튼 아이콘
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.teamnovapersonalprojectprojectingkotlin.navigation.AddSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * CalendarScreen: 상태 관리 및 이벤트 처리 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class) // Scaffold, FAB 등
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    onClickFAB: (route: String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 이벤트 처리 (스낵바, 다이얼로그 등)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is CalendarEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
                is CalendarEvent.ShowAddScheduleDialog -> {
                    // TODO: 일정 추가 다이얼로그 표시 로직
                    //snackbarHostState.showSnackbar("일정 추가 다이얼로그 (미구현)")
                    onClickFAB(
                        AddSchedule.createRoute(
                            uiState.selectedDate.year,
                            uiState.selectedDate.monthValue,
                            uiState.selectedDate.dayOfMonth,
                        )
                    )
                }
                // is CalendarEvent.NavigateToScheduleDetail -> { ... }
                else -> { snackbarHostState.showSnackbar("기본 (else)") }
            }
        }
    }

    // 에러 메시지 스낵바로 표시
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            snackbarHostState.showSnackbar(uiState.errorMessage!!, duration = SnackbarDuration.Short)
            viewModel.errorMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = { // 일정 추가 FAB
            FloatingActionButton(onClick = viewModel::onAddScheduleClick) {
                Icon(Icons.Filled.Add, contentDescription = "일정 추가")
            }
        }
    ) { paddingValues ->
        CalendarContent(
            modifier = Modifier.padding(paddingValues), // Scaffold 패딩 적용
            uiState = uiState,
            onPreviousMonthClick = viewModel::onPreviousMonthClick,
            onNextMonthClick = viewModel::onNextMonthClick,
            onDateClick = viewModel::onDateSelected, // 달력 UI 구현 시 날짜 클릭 연결
            onScheduleClick = viewModel::onScheduleClick
        )
    }
}

/**
 * CalendarContent: UI 렌더링 (Stateless)
 */
@Composable
fun CalendarContent(
    modifier: Modifier = Modifier,
    uiState: CalendarUiState,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onDateClick: (LocalDate) -> Unit, // 날짜 클릭 콜백
    onScheduleClick: (String) -> Unit // 스케줄 클릭 콜백
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 1. 월 이동 헤더
        MonthHeader(
            yearMonthText = uiState.currentYearMonth.format(uiState.monthYearFormatter),
            onPreviousClick = onPreviousMonthClick,
            onNextClick = onNextMonthClick,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
        )

        // 2. 요일 헤더
        DayOfWeekHeader(modifier = Modifier.padding(horizontal = 16.dp))

        // 3. 달력 그리드 영역 (실제 구현 필요)
        /**
        CalendarGridPlaceholder(
            selectedDate = uiState.selectedDate,
            onDateClick = onDateClick, // 실제 달력 구현 시 날짜 클릭 이벤트 연결
            modifier = Modifier.padding(horizontal = 16.dp).weight(1f) // 남은 공간 차지
        )
        **/
        CalendarGrid(
            dates = uiState.datesInMonth,
            selectedDate = uiState.selectedDate,
            onDateClick = onDateClick,
            modifier = Modifier.padding(horizontal = 16.dp).weight(1f) // 공간 차지
        )

        // 구분선
        HorizontalDivider()

        // 4. 선택된 날짜의 스케줄 리스트
        Text(
            text = " ${uiState.selectedDate.format(uiState.selectedDateFormatter)} 일정",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        )
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.schedulesForSelectedDate.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                Text("선택된 날짜에 일정이 없습니다.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 250.dp) // 높이 제한 (필요에 따라 조절)
                    .padding(horizontal = 16.dp)
            ) {
                items(items = uiState.schedulesForSelectedDate, key = { it.id }) { schedule ->
                    ScheduleListItem(schedule = schedule, onClick = { onScheduleClick(schedule.id) })
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp)) // 하단 여백
    }
}


// --- 달력 그리드 Composable ---
// CalendarGrid 함수 - List<LocalDate?> 사용 및 올바른 itemsIndexed 호출
@Composable
fun CalendarGrid(
    dates: List<LocalDate?>, // ViewModel에서 생성한 Nullable LocalDate 리스트
    selectedDate: LocalDate, // 선택된 날짜 (Non-nullable)
    onDateClick: (LocalDate) -> Unit, // 날짜 클릭 콜백 (Non-nullable LocalDate 받음)
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7), // 7열 그리드
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // --- itemsIndexed<T>(items: List<T>, ...) 확장 함수 사용 ---
        // 이 함수는 List<LocalDate?> 타입의 'dates'를 올바르게 받습니다.
        itemsIndexed(
            items = dates,
            // key: index와 date를 사용하여 고유하고 안정적인 키 생성
            key = { index, date -> date?.toEpochDay() ?: (-index - 1L) } // Null이면 음수 인덱스 사용
        ) { index, date: LocalDate? -> // 람다 파라미터: index, date (LocalDate?)

            // date가 null인지 확인
            if (date != null) {
                // 여기서는 'date'가 Non-null LocalDate로 스마트 캐스트됨
                DayCell(
                    date = date,
                    isSelected = date == selectedDate,
                    onClick = { onDateClick(date) }
                )
            } else {
                // date가 null이면 (빈 칸) Spacer 표시
                Spacer(modifier = Modifier
                    .aspectRatio(1f) // DayCell과 크기 맞춤
                    .padding(2.dp)   // DayCell과 패딩 맞춤
                )
            }
        } // --- End of itemsIndexed ---
    } // --- End of LazyVerticalGrid ---
}





    // --- 각 날짜 셀 Composable ---
@Composable
fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isToday = date == today

    Box(
        modifier = modifier
            .aspectRatio(1f) // 정사각형 모양 유지
            .padding(2.dp) // 셀 간 간격
            .clip(CircleShape) // 원형 클리핑
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary // 선택된 날짜 배경
                    isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) // 오늘 날짜 배경
                    else -> Color.Transparent // 기본 투명 배경
                }
            )
            .clickable(onClick = onClick), // 클릭 가능
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary // 선택된 날짜 텍스트 색상
                isToday -> MaterialTheme.colorScheme.onSecondaryContainer // 오늘 날짜 텍스트 색상
                date.dayOfWeek == DayOfWeek.SUNDAY -> Color.Red.copy(alpha = 0.8f) // 일요일
                date.dayOfWeek == DayOfWeek.SATURDAY -> Color.Blue.copy(alpha = 0.8f) // 토요일
                else -> LocalContentColor.current // 기본 텍스트 색상
            },
            style = MaterialTheme.typography.bodyMedium
        )
        // TODO: 해당 날짜에 스케줄이 있는지 여부를 표시하는 마커(점 등) 추가 가능
    }
}

// --- Helper Composables ---

@Composable
fun MonthHeader(
    yearMonthText: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPreviousClick) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 달")
        }
        Text(
            text = yearMonthText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNextClick) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 달")
        }
    }
}

@Composable
fun DayOfWeekHeader(modifier: Modifier = Modifier) {
    // --- 변경: 원하는 요일 순서대로 리스트 생성 ---
    val daysOfWeekOrder = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // 일요일부터 토요일까지
        daysOfWeekOrder.forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = if (dayOfWeek == DayOfWeek.SUNDAY) Color.Red else if (dayOfWeek == DayOfWeek.SATURDAY) Color.Blue else LocalContentColor.current
            )
        }
    }
}

// 실제 달력 그리드 대신 임시 Placeholder
@Composable
fun CalendarGridPlaceholder(
    selectedDate: LocalDate,
    onDateClick: (LocalDate) -> Unit, // 실제 구현 시 사용
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) // 임시 배경
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "[달력 영역]",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "(실제 달력 UI 구현 필요)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            // 임시로 날짜 변경 버튼 (테스트용)
            Row {
                Button(onClick = { onDateClick(selectedDate.minusDays(1)) }, modifier=Modifier.padding(end=4.dp)) { Text("<") }
                Text("현재 선택: ${selectedDate.dayOfMonth}일", modifier = Modifier.align(Alignment.CenterVertically))
                Button(onClick = { onDateClick(selectedDate.plusDays(1)) }, modifier=Modifier.padding(start=4.dp)) { Text(">") }
            }
        }
    }
}

@Composable
fun ScheduleListItem(
    schedule: ScheduleItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card( // Material 3 Card 사용
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 스케줄 색상 표시 (선택 사항)
            Box(modifier = Modifier.size(8.dp).background(Color(schedule.color), shape = MaterialTheme.shapes.small))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = schedule.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                // 시간 표시 (있는 경우)
                if (schedule.startTime != null) {
                    val timeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
                    val timeText = schedule.startTime.format(timeFormatter) +
                            (schedule.endTime?.let { " - ${it.format(timeFormatter)}" } ?: "")
                    Text(text = timeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
// --- Preview 업데이트 ---
@Preview(showBackground = true, name = "Calendar Content Preview")
@Composable
fun CalendarContentPreviewUpdated() {
    val initialDate = LocalDate.now()
    val viewModel = CalendarViewModel() // 임시 ViewModel
    val previewState by viewModel.uiState.collectAsState() // 상태 구독

    LaunchedEffect(Unit){ // Preview용 데이터 로드 (실제 앱에서는 ViewModel init에서)
        viewModel.onDateSelected(initialDate)
    }

    TeamnovaPersonalProjectProjectingKotlinTheme {
        CalendarContent(
            uiState = previewState,
            onPreviousMonthClick = {},
            onNextMonthClick = {},
            onDateClick = {},
            onScheduleClick = {}
        )
    }
}

// --- Preview ---
@Preview(showBackground = true)
@Composable
fun CalendarContentPreview() {
    val previewState = CalendarUiState(
        schedulesForSelectedDate = listOf(
            ScheduleItem("s1", "팀 회의", LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 30), 0xFFEF5350),
            ScheduleItem("s2", "점심 약속", LocalDate.now(), LocalTime.of(12, 0), LocalTime.of(13, 0), 0xFF66BB6A)
        )
    )
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CalendarContent(
            uiState = previewState,
            onPreviousMonthClick = {},
            onNextMonthClick = {},
            onDateClick = {},
            onScheduleClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Calendar Empty Schedule")
@Composable
fun CalendarContentEmptyPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        CalendarContent(
            uiState = CalendarUiState(schedulesForSelectedDate = emptyList()),
            onPreviousMonthClick = {},
            onNextMonthClick = {},
            onDateClick = {},
            onScheduleClick = {}
        )
    }
}
