package com.example.feature_main.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core_ui.theme.Dimens
import com.example.feature_main.viewmodel.CalendarUiState
import com.example.feature_main.viewmodel.ScheduleItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * 캘린더 관련 UI 컴포넌트 모음
 * 
 * 이 파일은 캘린더 화면을 구성하는 여러 컴포넌트들을 포함합니다:
 * - 캘린더 콘텐츠 영역 (CalendarContent)
 * - 월 헤더 (MonthHeader)
 * - 요일 헤더 (DayOfWeekHeader)
 * - 캘린더 그리드 (CalendarGrid)
 * - 날짜 셀 (DayCell)
 * - 일정 섹션 (ScheduleSection)
 * - 일정 리스트 아이템 (ScheduleListItem)
 */

/**
 * 캘린더 관련 색상 상수
 */
object CalendarColors {
    // 요일 색상
    val sundayColor = Color.Red.copy(alpha = 0.8f)
    val saturdayColor = Color.Blue.copy(alpha = 0.8f)
}

/**
 * 달력 컨텐츠 UI 렌더링 (Stateless)
 *
 * 달력 그리드와 월 이동 네비게이션 UI를 표시합니다.
 *
 * @param modifier 이 컴포넌트에 적용할 Modifier
 * @param uiState 현재 캘린더 UI 상태 (현재월, 선택된 날짜, 일정 목록 등)
 * @param onPreviousMonthClick 이전 달로 이동 버튼 클릭 시 호출될 콜백
 * @param onNextMonthClick 다음 달로 이동 버튼 클릭 시 호출될 콜백
 * @param onDateClick 날짜 클릭 시 호출될 콜백, 선택된 날짜를 인자로 받음
 */
@Composable
fun CalendarContent(
    modifier: Modifier = Modifier,
    uiState: CalendarUiState,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 1. 월 이동 헤더
        MonthHeader(
            yearMonthText = uiState.currentYearMonth.format(uiState.monthYearFormatter),
            onPreviousClick = { 
                onPreviousMonthClick() 
            },
            onNextClick = { 
                onNextMonthClick() 
            },
            modifier = Modifier.padding(vertical = Dimens.paddingMedium, horizontal = Dimens.paddingXLarge)
        )

        // 2. 요일 헤더
        DayOfWeekHeader(modifier = Modifier.padding(horizontal = Dimens.paddingXLarge))

        // 3. 달력 그리드 영역 - 애니메이션 제거하고 직접 표시
        CalendarGrid(
            dates = uiState.datesInMonth,
            selectedDate = uiState.selectedDate,
            onDateClick = onDateClick,
            datesWithSchedules = uiState.datesWithSchedules,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 월 이동 헤더 컴포넌트 (Stateless)
 *
 * 현재 표시 중인 연월과 이전/다음 달로 이동하는 버튼을 표시합니다.
 *
 * @param yearMonthText 표시할 연월 텍스트 (예: "2025년 4월")
 * @param onPreviousClick 이전 달 버튼 클릭 시 호출될 콜백
 * @param onNextClick 다음 달 버튼 클릭 시 호출될 콜백
 * @param modifier 이 컴포넌트에 적용할 Modifier
 */
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
        // 이전 달 버튼
        IconButton(onClick = onPreviousClick) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft, 
                contentDescription = "이전 달"
            )
        }
        
        // 연월 텍스트
        Text(
            text = yearMonthText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        // 다음 달 버튼
        IconButton(onClick = onNextClick) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                contentDescription = "다음 달"
            )
        }
    }
}

/**
 * 요일 헤더 컴포넌트 (Stateless)
 *
 * 달력 상단에 일요일부터 토요일까지 요일명을 표시합니다.
 * 일요일과 토요일은 각각 빨간색과 파란색으로 강조됩니다.
 *
 * @param modifier 이 컴포넌트에 적용할 Modifier
 */
@Composable
fun DayOfWeekHeader(modifier: Modifier = Modifier) {
    // 요일 순서 정의 (일요일부터 토요일까지)
    val daysOfWeekOrder = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    )
    
    Row(modifier = modifier.fillMaxWidth().padding(vertical = Dimens.paddingMedium)) {
        // 각 요일 표시
        daysOfWeekOrder.forEach { dayOfWeek ->
            Text(
                // 현지화된 요일명 가져오기 (SHORT 형식: 일, 월, 화, ...)
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                // 일요일은 빨간색, 토요일은 파란색으로 강조
                color = when (dayOfWeek) {
                    DayOfWeek.SUNDAY -> CalendarColors.sundayColor
                    DayOfWeek.SATURDAY -> CalendarColors.saturdayColor
                    else -> LocalContentColor.current
                }
            )
        }
    }
}

/**
 * 달력 그리드 컴포넌트 (Stateless)
 *
 * 월간 달력을 7열 그리드로 표시합니다. 빈 칸과 날짜 셀을 모두 처리합니다.
 * 날짜 클릭 시 해당 날짜가 선택되고 이벤트 핸들러가 호출됩니다.
 *
 * @param dates 표시할 날짜 목록. null 값은 빈 칸을 의미함
 * @param selectedDate 현재 선택된 날짜 (Non-nullable)
 * @param onDateClick 날짜 클릭 시 호출될 콜백 함수
 * @param datesWithSchedules 일정이 있는 날짜 집합
 * @param modifier 이 컴포넌트에 적용할 Modifier
 */
@Composable
fun CalendarGrid(
    dates: List<LocalDate?>,
    selectedDate: LocalDate,
    onDateClick: (LocalDate) -> Unit,
    datesWithSchedules: Set<LocalDate> = emptySet(),
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = Dimens.paddingMedium)
    ) {
        itemsIndexed(
            items = dates,
            key = { index, date -> date?.toEpochDay() ?: (-index - 1L) }
        ) { index, date: LocalDate? ->
            if (date != null) {
                DayCell(
                    date = date,
                    isSelected = date == selectedDate,
                    onClick = { onDateClick(date) },
                    hasSchedule = datesWithSchedules.contains(date)
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(Dimens.paddingSmall)
                )
            }
        }
    }
}

/**
 * 날짜 셀 컴포넌트 (Stateless)
 *
 * 일별 날짜를 원형 셀로 표시합니다. 선택 상태, 오늘 날짜, 주말 등에 따라 
 * 다른 스타일을 적용합니다. 일정이 있는 날짜는 하단에 작은 점으로 표시됩니다.
 *
 * @param date 표시할 날짜
 * @param isSelected 현재 선택된 날짜인지 여부
 * @param onClick 날짜 클릭 시 호출될 콜백
 * @param hasSchedule 이 날짜에 일정이 있는지 여부
 * @param modifier 이 컴포넌트에 적용할 Modifier
 */
@Composable
fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit,
    hasSchedule: Boolean = false,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isToday = date == today
    
    // 선택 애니메이션 (크기 변화) 다시 추가
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "Day Cell Scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // 날짜 셀
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .padding(Dimens.paddingSmall)
                .scale(scale) // 크기 변화 애니메이션 적용
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        else -> Color.Transparent
                    }
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // 날짜 텍스트
            Text(
                text = date.dayOfMonth.toString(),
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                    date.dayOfWeek == DayOfWeek.SUNDAY -> CalendarColors.sundayColor
                    date.dayOfWeek == DayOfWeek.SATURDAY -> CalendarColors.saturdayColor
                    else -> LocalContentColor.current
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        // 일정 표시 마커 - 바닥에 붙어있다가 일정이 생기면 생성되도록 변경
        if (hasSchedule) {
            Box(
                modifier = Modifier
                    .size(Dimens.markerSizeSmall)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        } else {
            // 일정이 없을 때는 높이만 확보하고 마커는 표시하지 않음
            Box(modifier = Modifier.size(0.dp))
        }
    }
}

/**
 * 선택된 날짜의 일정 목록 섹션 (Stateless)
 *
 * 선택된 날짜의 일정 목록을 표시하며, 로딩 상태와 빈 일정 상태도 처리합니다.
 * 24시간 보기 버튼을 통해 24시간 타임라인 화면으로 이동할 수 있습니다.
 *
 * @param uiState 현재 캘린더 UI 상태 (선택된 날짜의 일정 목록 포함)
 * @param onScheduleClick 일정 아이템 클릭 시 호출될 콜백, 일정 ID를 인자로 받음
 * @param onDateClick24Hour 24시간 보기 버튼 클릭 시 호출될 콜백, 날짜 객체를 인자로 받음
 */
@Composable
fun ScheduleSection(
    uiState: CalendarUiState,
    onScheduleClick: (String) -> Unit,
    onDateClick24Hour: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Dimens.paddingMedium)
    ) {
        // 일정 헤더 부분
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(
                    start = Dimens.paddingXLarge,
                    end = Dimens.paddingXLarge,
                    top = Dimens.paddingLarge,
                    bottom = Dimens.paddingLarge
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 날짜와 일정 텍스트
            Text(
                text = "${uiState.selectedDate.format(uiState.selectedDateFormatter)} 일정",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // 24시간 뷰로 이동 버튼
            Button(
                onClick = { onDateClick24Hour(uiState.selectedDate) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.height(Dimens.buttonHeightMedium)
            ) {
                Text("24시간 보기", style = MaterialTheme.typography.labelMedium)
            }
        }
        
        // 일정 목록 또는 상태 표시
        when {
            // 로딩 상태
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.sectionLargeHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.iconSizeLarge),
                            strokeWidth = Dimens.progressStrokeWidth
                        )
                        Spacer(modifier = Modifier.height(Dimens.paddingMedium))
                        Text(
                            "일정을 불러오는 중...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            
            // 빈 일정 상태
            uiState.schedulesForSelectedDate.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.sectionLargeHeight)
                        .padding(Dimens.paddingXLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 아이콘 회전 애니메이션 다시 추가
                        val iconRotation by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 1000),
                            label = "Empty State Icon Scale"
                        )
                        
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(Dimens.iconSizeXLarge)
                                .scale(iconRotation)
                        )
                        Spacer(modifier = Modifier.height(Dimens.paddingMedium))
                        Text(
                            "선택된 날짜에 일정이 없습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(Dimens.paddingLarge))
                        Text(
                            "새 일정을 추가하려면 우측 하단 버튼을 눌러주세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // 일정 목록 표시
            else -> {
                // 일정 개수에 따른 동적 높이 계산
                val itemCount = uiState.schedulesForSelectedDate.size
                val minHeight = Dimens.sectionMinHeight.value.toInt()
                val maxHeight = Dimens.sectionLargeHeight.value.toInt()
                val itemHeight = Dimens.listItemHeightMedium.value.toInt()
                val calculatedHeight = (itemCount * itemHeight).coerceIn(minHeight, maxHeight)
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = Dimens.sectionMinHeight,
                            max = calculatedHeight.dp
                        )
                        .padding(
                            horizontal = Dimens.paddingXLarge,
                            vertical = Dimens.paddingMedium
                        ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.paddingMedium)
                ) {
                    items(
                        items = uiState.schedulesForSelectedDate,
                        key = { it.id }
                    ) { schedule ->
                        ScheduleListItem(
                            schedule = schedule,
                            onClick = { onScheduleClick(schedule.id) }
                        )
                    }
                }
                
                // 일정이 많을 경우 힌트 표시
                if (itemCount > 4) {
                    Text(
                        text = "스크롤하여 더 많은 일정 보기",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.paddingSmall)
                    )
                }
            }
        }
    }
}

/**
 * 일정 목록 아이템 UI (Stateless)
 *
 * 개별 일정을 카드 형태로 표시합니다. 좌측에 색상 표시 막대, 중앙에 제목과 시간,
 * 우측에 상세보기 화살표를 배치합니다.
 *
 * @param schedule 표시할 일정 아이템 객체
 * @param onClick 아이템 클릭 시 호출될 콜백
 * @param modifier 이 컴포넌트에 적용할 Modifier
 */
@Composable
fun ScheduleListItem(
    schedule: ScheduleItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationSmall),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.paddingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. 일정 색상 표시 - 세로 막대 형태로 표현
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(36.dp)
                    .background(
                        color = Color(schedule.color),
                        shape = RoundedCornerShape(Dimens.cornerRadiusSmall)
                    )
            )
            
            // 2. 일정 내용 영역 (제목, 시간)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Dimens.paddingLarge)
            ) {
                // 일정 제목
                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 일정 시간 (시작~종료) - 있을 경우에만 표시
                if (schedule.startTime != null) {
                    val timeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
                    // 시작 시간과 종료 시간을 형식에 맞게 문자열로 변환
                    val timeText = schedule.startTime.format(timeFormatter) +
                            (schedule.endTime?.let { " - ${it.format(timeFormatter)}" } ?: "")
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = Dimens.paddingSmall)
                    ) {
                        // 시계 아이콘
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(Dimens.iconSizeSmall)
                        )
                        Spacer(modifier = Modifier.width(Dimens.paddingSmall))
                        // 시간 텍스트
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            
            // 3. 우측 화살표 아이콘 (상세보기 표시)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "일정 상세보기",
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                modifier = Modifier.size(Dimens.iconSizeMedium)
            )
        }
    }
} 