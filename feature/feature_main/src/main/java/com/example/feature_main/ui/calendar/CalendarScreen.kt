package com.example.feature_main.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import com.example.feature_main.viewmodel.CalendarViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.tooling.preview.Preview
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.feature_main.viewmodel.CalendarUiState
import com.example.feature_main.viewmodel.ScheduleItem
import java.time.LocalDate
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
 * @param onClickFAB 일정 추가 버튼 클릭 시 호출될 콜백, 네비게이션 경로 문자열을 인자로 받음
 * @param onNavigateToScheduleDetail 일정 상세화면으로 이동하는 콜백, 일정 ID를 인자로 받음
 * @param onNavigateToCalendar24Hour 24시간 캘린더 화면으로 이동하는 콜백, 연도,월,일을 인자로 받음
 * @param viewModel 캘린더 화면의 상태와 로직을 관리하는 ViewModel 인스턴스
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    onClickFAB: (route: String) -> Unit,
    onNavigateToScheduleDetail: (String) -> Unit = {}, 
    onNavigateToCalendar24Hour: (Int, Int, Int) -> Unit = { _, _, _ -> },
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
    
    // 일정 섹션 진입 애니메이션 상태 제거

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
                    kotlinx.coroutines.delay(150) // 애니메이션 효과 지연
                    onClickFAB(
                        com.example.navigation.AddSchedule.createRoute(
                            uiState.selectedDate.year,
                            uiState.selectedDate.monthValue,
                            uiState.selectedDate.dayOfMonth,
                        )
                    )
                    kotlinx.coroutines.delay(50) // 애니메이션 효과 지연
                    isFabVisible = true
                }
                is CalendarEvent.NavigateToScheduleDetail -> {
                    onNavigateToScheduleDetail(event.scheduleId)
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

    // 날짜 변경 감지 LaunchedEffect 제거

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
            
            // 선택된 날짜의 일정 섹션 (하단) - 애니메이션 제거하고 직접 표시
            ScheduleSection(
                uiState = uiState,
                onScheduleClick = viewModel::onScheduleClick,
                onDateClick24Hour = { date ->
                    onNavigateToCalendar24Hour(date.year, date.monthValue, date.dayOfMonth)
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
     * 프리뷰용 샘플 일정 생성
     */
    fun getSampleSchedules(): List<ScheduleItem> = listOf(
        ScheduleItem(
            "s1",
            "팀 회의",
            LocalDate.now(),
            LocalTime.of(10, 0),
            LocalTime.of(11, 30),
            0xFFEF5350
        ),
        ScheduleItem(
            "s2",
            "점심 약속",
            LocalDate.now(),
            LocalTime.of(12, 0),
            LocalTime.of(13, 0),
            0xFF66BB6A
        ),
        ScheduleItem(
            "s3",
            "프로젝트 회의",
            LocalDate.now(),
            LocalTime.of(14, 0),
            LocalTime.of(15, 30),
            0xFF42A5F5
        )
    )
    
    /**
     * 미리보기용 UI 상태 생성
     */
    fun getPreviewState(isEmpty: Boolean = false, isLoading: Boolean = false): CalendarUiState {
        return CalendarUiState(
            schedulesForSelectedDate = if (isEmpty) emptyList() else getSampleSchedules(),
            isLoading = isLoading
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