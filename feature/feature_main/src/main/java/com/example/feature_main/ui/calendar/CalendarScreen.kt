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
import com.example.feature_main.viewmodel.CalendarEvent
import com.example.feature_main.viewmodel.CalendarUiState
import com.example.feature_main.viewmodel.CalendarViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.example.core_navigation.core.AppNavigator
// import com.example.core_navigation.destination.AppRoutes // No longer needed for REFRESH_SCHEDULE_LIST_KEY
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.extension.REFRESH_SCHEDULE_LIST_KEY
import com.example.core_navigation.extension.ObserveNavigationResult
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model._new.enum.ScheduleStatus
import com.example.domain.model.base.Schedule
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.OwnerId
import com.example.domain.model.vo.ProjectId
import com.example.domain.model.vo.schedule.ScheduleContent
import com.example.domain.model.vo.schedule.ScheduleTitle

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
 * @param appNavigator 네비게이션 관리자
 * @param viewModel 캘린더 화면의 상태와 로직을 관리하는 ViewModel 인스턴스
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    appNavigator: AppNavigator,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // FAB 애니메이션 상태
    var isFabVisible by remember { mutableStateOf(true) }
    val fabScale by animateFloatAsState(
        targetValue = if (isFabVisible) 1f else 0f,
        animationSpec = spring(),
        label = "FAB Scale Animation"
    )

    ObserveNavigationResult<Boolean>(
        appNavigator = appNavigator,
        resultKey = REFRESH_SCHEDULE_LIST_KEY
    ) { needsRefresh ->
        if (needsRefresh == true) { // Explicitly check for true
            viewModel.refreshSchedules() // Call the existing refresh method
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
                    viewModel.onAddSchedule()
                    delay(50) // 애니메이션 효과 지연
                    isFabVisible = true
                }
                is CalendarEvent.NavigateToScheduleDetail -> {
                    // 이벤트가 발생하면 ViewModel의 onScheduleClick 함수가 호출되므로 이 블록은 더 이상 사용되지 않습니다.
                    // 이벤트 흐름은 유지하되 네비게이션 코드는 ScheduleService로 이동했습니다.
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
                onDateClick24Hour = viewModel::onDate24HourClick
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
        val now = Instant.now()
        return listOf(
            Schedule.registerNewSchedule(
                scheduleId = DocumentId("s1"),
                projectId = ProjectId("p1"),
                title = ScheduleTitle("팀 회의"),
                content = ScheduleContent("주간 진행 상황 공유"),
                startTime = LocalDateTime.of(today, LocalTime.of(10, 0)).atZone(ZoneId.systemDefault()).toInstant(),
                endTime = LocalDateTime.of(today, LocalTime.of(11, 30)).atZone(ZoneId.systemDefault()).toInstant(),
                creatorId = OwnerId("sample_creator_id_1"),
                createdAt = now,
                updatedAt = now,
                status = ScheduleStatus.CONFIRMED
            ),
            Schedule.registerNewSchedule(
                scheduleId = DocumentId("s2"),
                projectId = ProjectId("p2"),
                title = ScheduleTitle("점심 약속"),
                content = ScheduleContent("김대표님과 식사"),
                startTime = LocalDateTime.of(today, LocalTime.of(12, 0)).atZone(ZoneId.systemDefault()).toInstant(),
                endTime = LocalDateTime.of(today, LocalTime.of(13, 0)).atZone(ZoneId.systemDefault()).toInstant(),
                creatorId = OwnerId("sample_creator_id_2"),
                createdAt = now,
                updatedAt = now,
                status = ScheduleStatus.UNKNOWN
            ),
            Schedule.registerNewSchedule(
                scheduleId = DocumentId("s3"),
                projectId = ProjectId("p2"),
                title = ScheduleTitle("프로젝트 회의"),
                content = ScheduleContent("UI 디자인 검토"),
                startTime = LocalDateTime.of(today, LocalTime.of(14, 0)).atZone(ZoneId.systemDefault()).toInstant(),
                endTime = LocalDateTime.of(today, LocalTime.of(15, 30)).atZone(ZoneId.systemDefault()).toInstant(),
                creatorId = OwnerId("sample_creator_id_3"),
                createdAt = now,
                updatedAt = now,
                status = ScheduleStatus.CANCELLED
            ),
             Schedule.registerNewSchedule(
                scheduleId = DocumentId("s4"),
                projectId = ProjectId("p1"),
                title = ScheduleTitle("종일 이벤트"),
                content = ScheduleContent("워크샵 준비"),
                startTime = LocalDateTime.of(today, LocalTime.MIDNIGHT).atZone(ZoneId.systemDefault()).toInstant(),
                endTime = LocalDateTime.of(today, LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant(),
                creatorId = OwnerId("sample_creator_id_4"),
                createdAt = now,
                updatedAt = now,
                status = ScheduleStatus.COMPLETED
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