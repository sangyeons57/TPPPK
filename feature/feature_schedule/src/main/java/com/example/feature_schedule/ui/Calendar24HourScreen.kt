package com.example.feature_schedule.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer // Added import
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core_common.util.DateTimeUtil
import com.example.core_navigation.core.AppNavigator
import com.example.core_navigation.destination.AppRoutes // Keep this if other AppRoutes are used
import com.example.core_navigation.core.NavigationCommand
import com.example.core_navigation.extension.REFRESH_SCHEDULE_LIST_KEY
import com.example.core_navigation.extension.ObserveNavigationResult // Add this
import com.example.core_ui.components.buttons.DebouncedBackButton
import com.example.core_ui.theme.Dimens
import com.example.core_ui.theme.ScheduleColor1
import com.example.core_ui.theme.ScheduleColor3
import com.example.core_ui.theme.ScheduleColor4
import com.example.core_ui.theme.TeamnovaPersonalProjectProjectingKotlinTheme
import com.example.domain.model.ScheduleItem24Hour
import com.example.feature_schedule.viewmodel.Calendar24HourEvent
import com.example.feature_schedule.viewmodel.Calendar24HourUiState
import com.example.feature_schedule.viewmodel.Calendar24HourViewModel
import com.example.feature_schedule.util.SCHEDULE_DATA_CHANGED_RESULT_KEY // Added import
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Calendar24HourScreen: 특정 날짜의 24시간 스케줄 표시 (Stateful)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Calendar24HourScreen(
    appNavigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: Calendar24HourViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentUiState = uiState
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditDialog by remember { mutableStateOf<String?>(null) } // 편집 다이얼로그 표시 상태
    val navController = appNavigator.getNavController() // Get NavController

    // 애니메이션 상태
    var addButtonScale by remember { mutableStateOf(1f) }
    val addButtonScaleAnim by animateFloatAsState(
        targetValue = addButtonScale,
        animationSpec = tween(150),
        label = "Add Button Scale"
    )
    
    // 로딩 상태 애니메이션
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(currentUiState) {
        if (currentUiState is Calendar24HourUiState.Success) {
            delay(100) // 약간의 지연 후 콘텐츠 표시
            contentVisible = true
        }
    }

    // 이벤트 처리
    // Result listener from AddScheduleScreen or ScheduleDetailScreen
    DisposableEffect(navController) {
        val savedStateHandle = navController?.currentBackStackEntry?.savedStateHandle
        val liveData = savedStateHandle?.getLiveData<Boolean>(SCHEDULE_DATA_CHANGED_RESULT_KEY)

        val observer = Observer<Boolean> { result ->
            if (result == true) {
                viewModel.refreshSchedulesForCurrentDate()
                savedStateHandle.remove<Boolean>(SCHEDULE_DATA_CHANGED_RESULT_KEY)
            }
        }

        liveData?.observeForever(observer)

        onDispose {
            liveData?.removeObserver(observer)
        }
    }

    // Event handling from ViewModel
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is Calendar24HourEvent.NavigateBack -> appNavigator.navigateBack()
                is Calendar24HourEvent.NavigateToAddSchedule -> {
                    // 일정 추가 버튼 애니메이션
                    addButtonScale = 0.8f
                    delay(150)
                    
                    if (currentUiState is Calendar24HourUiState.Success) {
                        val date = currentUiState.selectedDate
                        if (date != null) {
                            appNavigator.navigate(
                                NavigationCommand.NavigateToRoute.fromRoute(
                                    AppRoutes.Main.Calendar.addSchedule(date.year, date.monthValue, date.dayOfMonth)
                                )
                            )
                        }
                    }
                    
                    delay(50)
                    addButtonScale = 1f
                }
                is Calendar24HourEvent.NavigateToScheduleDetail -> appNavigator.navigate(
                    NavigationCommand.NavigateToRoute.fromRoute(AppRoutes.Main.Calendar.scheduleDetail(event.scheduleId))
                )
                is Calendar24HourEvent.ShowScheduleEditDialog -> showEditDialog = event.scheduleId
                is Calendar24HourEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    ObserveNavigationResult<Boolean>(
        appNavigator = appNavigator,
        resultKey = REFRESH_SCHEDULE_LIST_KEY
    ) { needsRefresh ->
        if (needsRefresh == true) { // Explicitly check for true
            viewModel.refreshSchedules()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    AnimatedContent(
                        targetState = when (currentUiState) {
                            is Calendar24HourUiState.Success ->
                                currentUiState.selectedDate?.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")) ?: "날짜 정보 없음"
                            is Calendar24HourUiState.Loading -> "로딩 중..."
                            is Calendar24HourUiState.Error -> "오류"
                        },
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                        },
                        label = "Top App Bar Title Animation"
                    ) { text ->
                        Text(text)
                    }
                },
                navigationIcon = {
                    DebouncedBackButton(onClick = {
                        viewModel.onBackClick()
                    })
                },
                actions = {
                    IconButton(
                        onClick = viewModel::onAddScheduleClick
                    ) {
                        Icon(
                            Icons.Filled.Add, 
                            contentDescription = "일정 추가",
                            modifier = Modifier.scale(addButtonScaleAnim)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (currentUiState) {
            is Calendar24HourUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(Dimens.paddingXLarge))
                        Text("일정 로딩 중...", modifier = Modifier.alpha(0.7f))
                    }
                }
            }
            is Calendar24HourUiState.Error -> {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(500)) + 
                            slideInVertically(
                                animationSpec = tween(500, easing = LinearOutSlowInEasing),
                                initialOffsetY = { it / 2 }
                            )
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Card(
                            modifier = Modifier
                                .padding(Dimens.paddingXLarge)
                                .widthIn(max = 300.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(Dimens.paddingXLarge),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "오류 발생", 
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(Dimens.paddingMedium))
                                Text(
                                    currentUiState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(Dimens.paddingXLarge))
                                Button(
                                    onClick = { viewModel.onBackClick() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("돌아가기")
                                }
                            }
                        }
                    }
                }
            }
            is Calendar24HourUiState.Success -> {
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn(animationSpec = tween(500)) + 
                            expandVertically(
                                animationSpec = tween(500),
                                expandFrom = Alignment.Top
                            )
                ) {
                    Calendar24HourContent(
                        modifier = Modifier.padding(paddingValues),
                        schedules = currentUiState.schedules,
                        onScheduleClick = viewModel::onScheduleClick,
                        onScheduleLongClick = viewModel::onScheduleLongClick
                    )
                }
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
    val scrollState = rememberScrollState()
    
    // MaterialTheme 색상 미리 추출 (Canvas 내에서 사용할 수 없기 때문)
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // 스케줄 블록 탭 감지를 위한 맵
    val scheduleBlocksMap = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    
    // 스케줄 선택 애니메이션 상태
    var selectedScheduleId by remember { mutableStateOf<String?>(null) }
    var scheduleSelectionActive by remember { mutableStateOf(false) }
    
    LaunchedEffect(selectedScheduleId) {
        if (selectedScheduleId != null) {
            scheduleSelectionActive = true
            delay(200) // 애니메이션 후 초기화
            scheduleSelectionActive = false
            delay(50)
            selectedScheduleId = null
        }
    }
    
    // 현재 시간 표시 애니메이션
    val currentTimeOpacity by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "Current Time Indicator"
    )

    Box(modifier = modifier.verticalScroll(scrollState)) {
        // 현재 시간으로 자동 스크롤
        LaunchedEffect(Unit) {
            val currentHour = LocalTime.now().hour
            val scrollToPosition = with(density) { (currentHour * hourHeight.toPx()).toInt() }
            scrollState.scrollTo(scrollToPosition.coerceIn(0, scrollState.maxValue))
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            // 스케줄 블록 클릭 감지
                            scheduleBlocksMap.forEach { (id, rect) ->
                                if (rect.contains(offset)) {
                                    selectedScheduleId = id
                                    onScheduleClick(id)
                                    return@detectTapGestures
                                }
                            }
                        },
                        onLongPress = { offset ->
                            // 스케줄 블록 길게 누르기 감지
                            scheduleBlocksMap.forEach { (id, rect) ->
                                if (rect.contains(offset)) {
                                    selectedScheduleId = id
                                    onScheduleLongClick(id)
                                    return@detectTapGestures
                                }
                            }
                        }
                    )
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
            ) {
                // DrawScope에서는 with(density)를 사용할 수 없으므로 밖에서 필요한 값을 계산
                val spToPx = with(density) { 14.sp.toPx() }
                
                drawTimeline(hourHeight, textMeasurer, density)
                
                // 현재 시간 표시선
                val now = LocalTime.now()
                val currentTimePosition = now.toSecondOfDay() / 3600f * with(density) { hourHeight.toPx() }
                val timelineStartPadding = with(density) { 60.dp.toPx() }
                
                drawLine(
                    color = primaryColor.copy(alpha = currentTimeOpacity),
                    start = Offset(timelineStartPadding, currentTimePosition),
                    end = Offset(size.width, currentTimePosition),
                    strokeWidth = with(density) { 2.dp.toPx() }
                )
                
                // 현재 시간 원형 마커
                drawCircle(
                    color = primaryColor.copy(alpha = currentTimeOpacity),
                    radius = with(density) { 4.dp.toPx() },
                    center = Offset(timelineStartPadding, currentTimePosition)
                )
                
                // 스케줄 블록 그리기
                scheduleBlocksMap.clear() // 맵 초기화
                drawSchedules(
                    schedules = schedules,
                    hourHeight = hourHeight,
                    textMeasurer = textMeasurer,
                    selectedId = selectedScheduleId,
                    selectionActive = scheduleSelectionActive,
                    scheduleBlocksMapUpdater = { id, rect -> scheduleBlocksMap[id] = rect },
                    density = density
                )
            }
        }
    }
}

// 시간선 및 시간 텍스트 그리기
fun DrawScope.drawTimeline(hourHeight: Dp, textMeasurer: TextMeasurer, density: androidx.compose.ui.unit.Density) {
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val timelineStartPadding = with(density) { 60.dp.toPx() } // 시간 텍스트를 위한 시작 패딩

    for (hour in 0..24) {
        val y = hour * hourHeightPx
        drawLine(
            color = Color.LightGray,
            start = Offset(timelineStartPadding, y),
            end = Offset(size.width, y),
            strokeWidth = with(density) { 1.dp.toPx() }
        )
        if (hour < 24) {
            val timeText = LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("HH:mm"))
            val textLayoutResult = textMeasurer.measure(
                text = AnnotatedString(timeText),
                style = TextStyle(fontSize = 12.sp, color = Color.Gray)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    timelineStartPadding - textLayoutResult.size.width - with(density) { 8.dp.toPx() }, 
                    y - textLayoutResult.size.height / 2
                )
            )
        }
    }
    // 시간축 세로선
    drawLine(
        color = Color.Gray,
        start = Offset(timelineStartPadding, 0f),
        end = Offset(timelineStartPadding, hourHeightPx * 24),
        strokeWidth = with(density) { 2.dp.toPx() }
    )
}

// 스케줄 블록 그리기
fun DrawScope.drawSchedules(
    schedules: List<ScheduleItem24Hour>,
    hourHeight: Dp,
    textMeasurer: TextMeasurer,
    selectedId: String? = null,
    selectionActive: Boolean = false,
    scheduleBlocksMapUpdater: (String, androidx.compose.ui.geometry.Rect) -> Unit,
    density: androidx.compose.ui.unit.Density
) {
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val timelineStartPadding = with(density) { 60.dp.toPx() }
    val schedulePadding = with(density) { Dimens.paddingSmall.toPx() }
    
    // 알파 값 기반 애니메이션 계산
    val selectionAlpha = if (selectionActive) 0.7f else 1.0f

    schedules.forEach { schedule ->
        val startY = DateTimeUtil.getSecondOfDayFromInstant(schedule.startTime) / 3600f * hourHeightPx
        val endY = DateTimeUtil.getSecondOfDayFromInstant(schedule.endTime) / 3600f * hourHeightPx
        val scheduleHeight = endY - startY
        val scheduleWidth = size.width - timelineStartPadding - schedulePadding * 2

        // 애니메이션 효과 계산
        val isSelected = schedule.id == selectedId
        val scale = if (isSelected && selectionActive) 1.02f else 1f
        val alpha = if (isSelected && selectionActive) selectionAlpha else 1f
        val shadowElevation = if (isSelected && selectionActive) 
                              with(density) { Dimens.elevationLarge.toPx() } 
                              else with(density) { Dimens.elevationSmall.toPx() }
        
        // 스케일 적용
        val scaledWidth = scheduleWidth * scale
        val scaledHeight = scheduleHeight * scale
        val offsetX = (scheduleWidth - scaledWidth) / 2
        val offsetY = (scheduleHeight - scaledHeight) / 2
        
        val rectTopLeft = Offset(
            timelineStartPadding + schedulePadding + offsetX,
            startY + offsetY
        )
        val rectSize = androidx.compose.ui.geometry.Size(scaledWidth, scaledHeight)
        val rect = androidx.compose.ui.geometry.Rect(rectTopLeft, rectSize)
        
        // 클릭 인식을 위한 히트 영역 기록
        scheduleBlocksMapUpdater(schedule.id, rect)
        
        // 선택된 항목은 그림자 효과 추가
        if (isSelected && selectionActive) {
            // 그림자 효과 (단순화)
            drawRect(
                color = Color.Black.copy(alpha = 0.2f),
                topLeft = Offset(
                    rectTopLeft.x + with(density) { Dimens.paddingSmall.toPx() },
                    rectTopLeft.y + with(density) { Dimens.paddingSmall.toPx() }
                ),
                size = rectSize,
                alpha = alpha
            )
        }

        // 스케줄 블록 배경
        val scheduleColor = Color(schedule.color)
        drawRect(
            color = scheduleColor,
            topLeft = rectTopLeft,
            size = rectSize,
            alpha = alpha
        )
        
        // 스케줄 테두리
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = rectTopLeft,
            size = rectSize,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = with(density) { Dimens.borderWidth.toPx() }
            ),
            alpha = alpha
        )

        // 스케줄 텍스트
        val textColor = if (isDarkColor(scheduleColor)) Color.White else Color.Black
        val textPadding = with(density) { Dimens.paddingSmall.toPx() }
        
        // 텍스트 계산에 필요한 sp to px 변환
        val spToPx = with(density) { 14.sp.toPx() }
        
        val textLayoutResult = textMeasurer.measure(
            text = AnnotatedString(schedule.title),
            style = TextStyle(fontSize = 14.sp, color = textColor),
            constraints = androidx.compose.ui.unit.Constraints(
                maxWidth = scaledWidth.toInt() - (2 * textPadding).toInt() // 텍스트 좌우 패딩 고려
            ),
            maxLines = (scaledHeight / (spToPx * 1.2f)).toInt().coerceAtLeast(1), // 높이에 따라 최대 줄 수 계산
            overflow = TextOverflow.Ellipsis
        )
        
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(rectTopLeft.x + textPadding, rectTopLeft.y + textPadding),
            alpha = alpha
        )
    }
}

// 색상의 명암 판단 (어두운 배경색이면 밝은 텍스트, 밝은 배경색이면 어두운 텍스트 사용)
fun isDarkColor(color: Color): Boolean {
    // RGB 값으로 명암 판단 (간단한 휘도 계산)
    val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return luminance < 0.5
}

// 일정 수정/삭제 다이얼로그
@Composable
fun ScheduleEditDialog(
    scheduleId: String,
    onDismiss: () -> Unit,
    onDeleteClick: (String) -> Unit,
    onEditClick: (String) -> Unit
) {
    var deleteButtonPressed by remember { mutableStateOf(false) }
    var editButtonPressed by remember { mutableStateOf(false) }
    
    val deleteScale by animateFloatAsState(
        targetValue = if (deleteButtonPressed) 0.9f else 1f,
        animationSpec = tween(150),
        label = "Delete Button Scale"
    )
    
    val editScale by animateFloatAsState(
        targetValue = if (editButtonPressed) 0.9f else 1f,
        animationSpec = tween(150),
        label = "Edit Button Scale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("일정 편집") },
        text = { Text("이 일정을 수정하거나 삭제하시겠습니까?") },
        confirmButton = {
            TextButton(
                onClick = { 
                    editButtonPressed = true
                    onEditClick(scheduleId) 
                },
                modifier = Modifier.scale(editScale)
            ) {
                Text("수정")
            }
            
            LaunchedEffect(editButtonPressed) {
                if (editButtonPressed) {
                    delay(150)
                    editButtonPressed = false
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = { 
                        deleteButtonPressed = true
                        onDeleteClick(scheduleId) 
                    },
                    modifier = Modifier.scale(deleteScale)
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
                
                LaunchedEffect(deleteButtonPressed) {
                    if (deleteButtonPressed) {
                        delay(150)
                        deleteButtonPressed = false
                    }
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
    // 오늘 날짜와 시간을 기준으로 Instant 생성
    val today = LocalDate.now()
    val sampleSchedules = listOf(
        ScheduleItem24Hour(
            "1", 
            "팀 회의", 
            DateTimeUtil.toInstant(LocalTime.of(10, 0))!!, // 오전 10시
            DateTimeUtil.toInstant(LocalTime.of(11, 30))!!, // 오전 11시 30분
            ScheduleColor4.value
        ),
        ScheduleItem24Hour(
            "2", 
            "점심 약속", 
            DateTimeUtil.toInstant(LocalTime.of(12, 30))!!, // 오후 12시 30분 
            DateTimeUtil.toInstant(LocalTime.of(13, 30))!!, // 오후 1시 30분
            ScheduleColor1.value
        ),
        ScheduleItem24Hour(
            "3", 
            "개인 프로젝트", 
            DateTimeUtil.toInstant(LocalTime.of(15, 0))!!, // 오후 3시
            DateTimeUtil.toInstant(LocalTime.of(17, 0))!!, // 오후 5시
            ScheduleColor3.value
        )
    )

    val previewState = Calendar24HourUiState.Success(today, sampleSchedules)

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