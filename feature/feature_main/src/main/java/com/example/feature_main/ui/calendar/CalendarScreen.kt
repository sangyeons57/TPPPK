package com.example.feature_main.ui.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_ui.theme.Dimens
import com.example.domain.model.Schedule
import com.example.feature_main.viewmodel.CalendarEvent
import com.example.feature_main.viewmodel.CalendarUiState
import com.example.feature_main.viewmodel.CalendarViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.LocalNavController
import com.example.core_navigation.core.ComposeNavigationHandler
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.core.NavigationCommand
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 캘린더 모듈
 * 
 * 이 모듈은 다음의 캘린더 관련 컴포넌트들을 제공합니다:
 * - CalendarScreen: 메인 캘린더 화면 Composable
 * - 캘린더 그리드 (CalendarContent): 월별 캘린더
 * - 일정 섹션 (ScheduleSection): 선택된 날짜의 일정 목록
 * 
 * CalendarComponents.kt에는 다음 UI 요소들이 포함되어 있습니다:
 * - DayCell: 날짜 셀 컴포넌트 (재사용 가능)
 * - ScheduleListItem: 개별 일정 아이템 UI (재사용 가능)
 * - 기타 캘린더 관련 UI 컴포넌트들
 * 
 * 참고: 이 파일은 이전에 `com.example.feature_main.ui.CalendarScreen`에 있던 화면을 대체합니다.
 * 모든 코드는 이제 이 파일과 관련 캘린더 컴포넌트 파일로 통합되었습니다.
 */

/**
 * 캘린더 화면의 메인 Composable 함수
 * 
 * 상태 관리 및 이벤트 처리(Stateful)를 담당하며, 앱 내 네비게이션을 처리합니다.
 * ViewModel로부터 상태를 수신하고 사용자 이벤트에 반응하는 로직을 포함합니다.
 * 
 * 모듈화된 구조:
 * - 이 파일: 메인 통합 화면, 이벤트 처리
 * - CalendarContent: 상단 캘린더 그리드
 * - ScheduleSection: 하단 일정 목록
 * - CalendarComponents: 공용 컴포넌트 (DayCell 등)
 * - CalendarDimens: UI 크기 상수
 *
 * @param modifier 이 컴포넌트에 적용할 Modifier
 * @param navigationManager 네비게이션 관리자
 * @param viewModel 캘린더 화면의 상태와 로직을 관리하는 ViewModel 인스턴스
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    navigationManager: ComposeNavigationHandler,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val localNavController = LocalNavController.current
    
    // Observe result from AddScheduleScreen/EditScheduleScreen
    val scheduleUpdateResult = localNavController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Boolean>("schedule_added_or_updated")?.observeAsState()

    LaunchedEffect(scheduleUpdateResult?.value) {
        if (scheduleUpdateResult?.value == true) {
            viewModel.refreshSchedules()
            localNavController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("schedule_added_or_updated")
        }
    }
    
    // FAB 애니메이션 상태
    var isFabVisible by remember { mutableStateOf(true) }
    val fabScale by animateFloatAsState(
        targetValue = if (isFabVisible) 1f else 0f,
        animationSpec = spring(),
        label = "FAB Scale Animation"
    )
    
    // "refresh_calendar" 키로 전달된 결과 관찰
    LaunchedEffect(navigationManager) {
        navigationManager.getResultFlow<Boolean>("refresh_calendar").collectLatest { refresh ->
            if (refresh) {
                viewModel.refreshSchedules()
                // 결과 소비 후 다시 false로 설정하여 반복적인 새로고침 방지
                navigationManager.setResult("refresh_calendar", false)
            }
        }
    }

    // 이벤트 처리
    LaunchedEffect(viewModel) { 
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is CalendarEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
                is CalendarEvent.ShowAddScheduleDialog -> {
                    // FAB 클릭 애니메이션
                    isFabVisible = false
                    delay(150) // 애니메이션 효과 지연
                    navigationManager.navigate(
                        NavigationCommand.NavigateToRoute(
                            AppRoutes.Main.Calendar.addSchedule(
                                uiState.selectedDate.year,
                                uiState.selectedDate.monthValue,
                                uiState.selectedDate.dayOfMonth
                            )
                        )
                    )
                    delay(50) // 애니메이션 효과 지연
                    isFabVisible = true
                }
                is CalendarEvent.NavigateToScheduleDetail -> {
                    navigationManager.navigate(
                        NavigationCommand.NavigateToRoute(
                            AppRoutes.Main.Calendar.scheduleDetail(event.scheduleId)
                        )
                    )
                }
            }
        }
    }

    // 에러 메시지 스낵바 표시
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            snackbarHostState.showSnackbar(uiState.errorMessage!!, duration = SnackbarDuration.Short)
            viewModel.errorMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onAddScheduleClick,
                modifier = Modifier.scale(fabScale)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "일정 추가")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // 캘린더 컨텐츠 (상단)
            CalendarContent(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Dimens.paddingXLarge),
                uiState = uiState,
                onPreviousMonthClick = viewModel::onPreviousMonthClick,
                onNextMonthClick = viewModel::onNextMonthClick,
                onDateClick = viewModel::onDateSelected
            )
            
            // 구분선
            HorizontalDivider()
            
            // 선택된 날짜의 일정 섹션 (하단)
            ScheduleSection(
                uiState = uiState,
                onScheduleClick = viewModel::onScheduleClick,
                onDateClick24Hour = { date ->
                    navigationManager.navigate(
                        NavigationCommand.NavigateToRoute(
                            AppRoutes.Main.Calendar.calendar24Hour(
                                date.year,
                                date.monthValue,
                                date.dayOfMonth
                            )
                        )
                    )
                }
            )
        }
    }
}

/**
 * 프리뷰 유틸리티 객체
 * 
 * 캘린더 관련 컴포넌트를 미리보기하기 위한 샘플 데이터를 제공합니다.
 */
private object PreviewUtils {
    
    /**
     * 프리뷰용 샘플 일정 생성 (Now returns List<Schedule>)
     */
    fun getSampleSchedules(): List<Schedule> {
        val today = LocalDate.now()
        return listOf(
            Schedule(
                id = "s1",
                projectId = "p1",
                title = "팀 회의",
                content = "주간 진행 상황 공유",
                startTime = LocalDateTime.of(today, LocalTime.of(10, 0)),
                endTime = LocalDateTime.of(today, LocalTime.of(11, 30)),
                participants = listOf("user1", "user2"),
                isAllDay = false
            ),
            Schedule(
                id = "s2",
                projectId = null,
                title = "점심 약속",
                content = "김대표님과 식사",
                startTime = LocalDateTime.of(today, LocalTime.of(12, 0)),
                endTime = LocalDateTime.of(today, LocalTime.of(13, 0)),
                participants = listOf("user1"),
                isAllDay = false
            ),
            Schedule(
                id = "s3",
                projectId = "p2",
                title = "프로젝트 회의",
                content = "UI 디자인 검토",
                startTime = LocalDateTime.of(today, LocalTime.of(14, 0)),
                endTime = LocalDateTime.of(today, LocalTime.of(15, 30)),
                participants = listOf("user1", "user3", "user4"),
                isAllDay = false
            ),
             Schedule(
                id = "s4",
                projectId = "p1",
                title = "종일 이벤트",
                content = "워크샵 준비",
                startTime = LocalDateTime.of(today, LocalTime.MIDNIGHT),
                endTime = LocalDateTime.of(today, LocalTime.MAX),
                participants = listOf("user1"),
                isAllDay = true
            )
        )
    }
    
    /**
     * 미리보기용 UI 상태 생성
     */
    fun getPreviewState(isEmpty: Boolean = false, isLoading: Boolean = false): CalendarUiState {
        return CalendarUiState(
            schedulesForSelectedDate = if (isEmpty) emptyList() else getSampleSchedules(),
            isLoading = isLoading,
            datesWithSchedules = if (isEmpty) emptySet() else setOf(LocalDate.now())
        )
    }
}

/**
 * 전체 캘린더 화면 미리보기
 */
@Preview(showBackground = true, name = "전체 캘린더 화면")
@Composable
fun CalendarScreenPreview() {
    TeamnovaPersonalProjectProjectingKotlinTheme {
        val previewState = PreviewUtils.getPreviewState()
        
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.Add, contentDescription = "일정 추가")
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // 상단 캘린더
                CalendarContent(
                    modifier = Modifier.weight(1f),
                    uiState = previewState,
                    onPreviousMonthClick = {},
                    onNextMonthClick = {},
                    onDateClick = {}
                )
                
                // 구분선
                HorizontalDivider()
                
                // 하단 일정 섹션
                ScheduleSection(
                    uiState = previewState,
                    onScheduleClick = {},
                    onDateClick24Hour = {}
                )
            }
        }
    }
} 