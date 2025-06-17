package com.example.feature_schedule.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.core_ui.theme.*
import com.example.domain.model.base.Schedule
import com.example.domain.usecase.schedule.DeleteScheduleUseCase
import com.example.domain.usecase.schedule.GetSchedulesForDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import com.example.core_navigation.destination.AppRoutes
import java.time.Instant
import android.util.Log


/**
 * 24시간 캘린더 화면에서 사용하는 일정 아이템 데이터 클래스
 * 일정 ID, 제목, 시작 시간, 종료 시간, 색상을 포함합니다.
 */
data class ScheduleItem24Hour(
    val id: String,
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val color: ULong
)

// --- UI 상태 ---
sealed interface Calendar24HourUiState {
    object Loading : Calendar24HourUiState
    data class Success(
        val selectedDate: LocalDate?, // 초기 로딩 시 null일 수 있음
        val schedules: List<ScheduleItem24Hour> = emptyList()
    ) : Calendar24HourUiState
    data class Error(val message: String) : Calendar24HourUiState
}

// --- 이벤트 ---
sealed class Calendar24HourEvent {
    object NavigateBack : Calendar24HourEvent()
    object NavigateToAddSchedule : Calendar24HourEvent()
    data class NavigateToScheduleDetail(val scheduleId: String) : Calendar24HourEvent()
    data class ShowScheduleEditDialog(val scheduleId: String) : Calendar24HourEvent()
    data class ShowSnackbar(val message: String) : Calendar24HourEvent()
}

/**
 * 24시간 캘린더 화면을 위한 ViewModel
 * 
 * 특정 날짜의 일정을 로드하고 관리하며, 일정 관련 작업(조회, 삭제, 이동)을 처리합니다.
 * SavedStateHandle을 통해 year, month, day 파라미터를 전달받아 해당 날짜의 일정을 로드합니다.
 */
@HiltViewModel
class Calendar24HourViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSchedulesForDateUseCase: GetSchedulesForDateUseCase,
    private val deleteScheduleUseCase: DeleteScheduleUseCase
) : ViewModel() {

    // --- 날짜 파라미터 ---
    private val year: Int = savedStateHandle.get<Int>(AppRoutes.Main.Calendar.ARG_YEAR) ?: LocalDate.now().year
    private val month: Int = savedStateHandle.get<Int>(AppRoutes.Main.Calendar.ARG_MONTH) ?: LocalDate.now().monthValue
    private val day: Int = savedStateHandle.get<Int>(AppRoutes.Main.Calendar.ARG_DAY) ?: LocalDate.now().dayOfMonth
    
    // --- 색상 관리자 ---
    private val colorManager = ScheduleColorManager()

    // --- 상태 및 이벤트 Flow ---
    private val _uiState = MutableStateFlow<Calendar24HourUiState>(Calendar24HourUiState.Loading)
    val uiState: StateFlow<Calendar24HourUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<Calendar24HourEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        Log.d("CalendarVM", "ViewModel initialized. Year: $year, Month: $month, Day: $day")
        loadSchedules(LocalDate.of(year, month, day))
        
        // 특정 프로젝트 ID에 대한 기본 타입 설정 예시
        // 실제로는 저장된 사용자 설정에서 로드해야 함
        colorManager.projectDefaultTypes["project-special"] = ScheduleType.IMPORTANT
    }

    /**
     * 지정된 날짜의 일정을 로드합니다.
     * 
     * @param date 로드할 일정 날짜
     */
    internal fun loadSchedules(date: LocalDate) { 
        viewModelScope.launch {
            _uiState.value = Calendar24HourUiState.Loading
            Log.d("CalendarVM", "loadSchedules($date) 호출됨. UI 상태: Loading")
            
            getSchedulesForDateUseCase(date).collect { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val schedulesDomain = result.data
                        
                        // 도메인 모델을 UI 모델로 변환
                        val schedules = schedulesDomain.map { schedule ->
                            // 일정 타입 추론
                            val scheduleType = colorManager.inferScheduleType(schedule)
                            
                            // 일정 타입에 따른 색상 할당
                            val scheduleColor = colorManager.getColor(scheduleType)
                            
                            // UI 모델로 변환
                            ScheduleItem24Hour(
                                id = schedule.id.value,
                                title = schedule.title.value,
                                startTime = schedule.startTime,
                                endTime = schedule.endTime,
                                color = scheduleColor
                            )
                        }
                        
                        _uiState.value = Calendar24HourUiState.Success(date, schedules)
                        Log.d("CalendarVM", "loadSchedules($date) 성공. 일정 개수: ${schedules.size}")
                    }
                    is CustomResult.Failure -> {
                        val errorMessage = "Failed to load schedules: ${result.error.message ?: "Unknown error"}"
                        _uiState.value = Calendar24HourUiState.Error(errorMessage)
                        Log.e("CalendarVM", "loadSchedules($date) 실패: ${result.error.message}")
                        _eventFlow.emit(Calendar24HourEvent.ShowSnackbar(errorMessage))
                    }
                    else -> {
                        // 로딩 상태 등 무시
                    }
                }
            }
        }
    }

    /**
     * 고대비 모드 설정을 변경합니다.
     * 
     * @param enabled 고대비 모드 활성화 여부
     */
    fun setHighContrastMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                // 실제로는 사용자 설정에 저장해야 함
                colorManager.highContrastMode = enabled
                refreshSchedules() // 변경된 설정 적용을 위해 새로고침
            } catch (e: Exception) {
                _eventFlow.emit(Calendar24HourEvent.ShowSnackbar("설정 변경 중 오류가 발생했습니다."))
            }
        }
    }

    /**
     * 그라데이션 효과 설정을 변경합니다.
     * 
     * @param enabled 그라데이션 효과 활성화 여부
     */
    fun setGradientEffect(enabled: Boolean) {
        viewModelScope.launch {
            try {
                // 실제로는 사용자 설정에 저장해야 함
                colorManager.gradientEffect = enabled
                refreshSchedules() // 변경된 설정 적용을 위해 새로고침
            } catch (e: Exception) {
                _eventFlow.emit(Calendar24HourEvent.ShowSnackbar("설정 변경 중 오류가 발생했습니다."))
            }
        }
    }

    /**
     * 일정을 삭제합니다.
     * 
     * @param scheduleId 삭제할 일정 ID
     */
    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is Calendar24HourUiState.Success) {
                // 현재 화면에서 해당 일정 제거
                val updatedSchedules = currentState.schedules.filterNot { it.id == scheduleId }
                _uiState.value = Calendar24HourUiState.Success(currentState.selectedDate, updatedSchedules)
                
                // 서버에서 삭제
                val result = deleteScheduleUseCase(scheduleId)
                when (result) {
                    is CustomResult.Success -> {
                        _eventFlow.emit(Calendar24HourEvent.ShowSnackbar("일정이 삭제되었습니다."))
                    }
                    is CustomResult.Failure -> {
                        // 삭제 실패 시 원복
                        _uiState.value = currentState
                        _eventFlow.emit(Calendar24HourEvent.ShowSnackbar("일정 삭제 실패: ${result.error.message ?: "알 수 없는 오류"}"))
                    }
                    else -> {
                        // 로딩 상태 등 무시
                    }
                }
            }
        }
    }

    /**
     * 뒤로가기 버튼 클릭 처리
     */
    fun onBackClick() {
        viewModelScope.launch { _eventFlow.emit(Calendar24HourEvent.NavigateBack) }
    }

    /**
     * 일정 추가 버튼 클릭 처리
     */
    fun onAddScheduleClick() {
        viewModelScope.launch { _eventFlow.emit(Calendar24HourEvent.NavigateToAddSchedule) }
    }

    /**
     * 일정 항목 클릭 처리
     * 
     * @param scheduleId 클릭한 일정 ID
     */
    fun onScheduleClick(scheduleId: String) {
        viewModelScope.launch { _eventFlow.emit(Calendar24HourEvent.NavigateToScheduleDetail(scheduleId)) }
    }

    /**
     * 일정 항목 롱클릭 처리
     * 
     * @param scheduleId 롱클릭한 일정 ID
     */
    fun onScheduleLongClick(scheduleId: String) {
        viewModelScope.launch { _eventFlow.emit(Calendar24HourEvent.ShowScheduleEditDialog(scheduleId)) }
    }

    /**
     * 현재 선택된 날짜의 일정을 새로고침합니다.
     * 화면을 아래로 당겨 새로고침(pull-to-refresh)하거나, 다른 화면에서 데이터 변경 후
     * 돌아왔을 때 호출될 수 있습니다.
     */
    fun refreshSchedules() {
        viewModelScope.launch {
            val currentSuccessState = _uiState.value as? Calendar24HourUiState.Success
            val dateToRefresh = currentSuccessState?.selectedDate
            if (dateToRefresh != null) {
                Log.d("CalendarVM", "refreshSchedules() 호출됨. 현재 선택된 날짜: $dateToRefresh")
                loadSchedules(dateToRefresh)
            } else {
                // 현재 상태가 Success가 아니거나 selectedDate가 없는 경우,
                // 초기 날짜로 다시 로드 시도
                Log.d("CalendarVM", "refreshSchedules() 호출됨. 현재 선택된 날짜 없음. 초기 날짜로 로드 시도.")
                loadSchedules(LocalDate.of(year, month, day))
            }
        }
    }
}

/**
 * 일정 타입 열거형
 */
enum class ScheduleType {
    PERSONAL,   // 개인 일정
    WORK,       // 업무 일정
    PROJECT,    // 프로젝트 일정
    MEETING,    // 미팅 일정
    OTHER,      // 기타 일정
    IMPORTANT,  // 중요 일정
    DEADLINE    // 마감 일정
}

/**
 * 프로젝트 ID와 일정 타입을 기반으로 색상을 관리하는 헬퍼 클래스
 */
class ScheduleColorManager {
    // 프로젝트 ID를 색상에 매핑 (일관성 유지용)
    private val projectColorMap = mutableMapOf<String, ULong>()
    
    // 프로젝트별 기본 타입 설정
    val projectDefaultTypes = mutableMapOf<String, ScheduleType>()
    
    // 고대비 모드 활성화 여부
    var highContrastMode = false
    
    // 그라데이션 효과 활성화 여부
    var gradientEffect = true
    
    // 하루 시간대 표현 (오전, 오후, 저녁, 밤)
    private val morningStart = LocalTime.of(6, 0)
    private val afternoonStart = LocalTime.of(12, 0)
    private val eveningStart = LocalTime.of(18, 0)
    private val nightStart = LocalTime.of(22, 0)
    
    /**
     * 일정에 대한 색상을 결정합니다.
     * 
     * @param schedule 색상을 결정할 일정 객체
     * @return 색상 값 (ULong)
     */
    fun getColorForSchedule(schedule: Schedule): ULong {
        // 프로젝트 ID가 있고, 해당 프로젝트에 기본 타입이 지정되어 있는 경우
        val projectId = schedule.projectId
        if (projectId != null) {
            if (projectDefaultTypes.containsKey(projectId.value)) {
                val type = projectDefaultTypes[projectId.value]!!
                return if (highContrastMode) getHighContrastColor(type) else getColor(type)
            }
        }
        
        // 타입을 추론해서 색상 결정
        val inferredType = inferScheduleType(schedule)
        return if (highContrastMode) getHighContrastColor(inferredType) else getColor(inferredType)
    }
    
    /**
     * 시각적 사용성을 위해 일정의 시작/종료 시간에 따라 알파 값 계산
     * (아침에 시작하는 일정은 밝게, 밤에 시작하는 일정은 어둠게 표시)
     * 
     * @param startTime 일정 시작 시간
     * @param endTime 일정 종료 시간
     * @return 시작 알파와 종료 알파 값 쌍
     */
    fun calculateTimeBasedAlpha(
        startTime: Instant,
        endTime: Instant
    ): Pair<Float, Float> {
        if (!gradientEffect) {
            return Pair(1.0f, 1.0f)
        }
        
        // 현재 시간대에 따라 알파 값 조정 (24시간 일정은 아침에 밝고 밤에 어둡게)
        val startLocalTime = DateTimeUtil.toLocalTime(startTime) ?: LocalTime.NOON
        val endLocalTime = DateTimeUtil.toLocalTime(endTime) ?: LocalTime.NOON
        
        val startAlpha = getAlphaForTime(startLocalTime)
        val endAlpha = getAlphaForTime(endLocalTime)
        
        return Pair(startAlpha, endAlpha)
    }
    
    /**
     * 시간에 따른 알파값 계산
     * 
     * @param time 시간
     * @return 계산된 알파값 (0.7f ~ 1.0f)
     */
    private fun getAlphaForTime(time: LocalTime): Float {
        return when {
            time.isBefore(morningStart) -> 0.7f  // 새벽
            time.isBefore(afternoonStart) -> 1.0f  // 오전
            time.isBefore(eveningStart) -> 0.9f  // 오후
            time.isBefore(nightStart) -> 0.8f  // 저녁
            else -> 0.7f  // 밤
        }
    }
    
    /**
     * 일정 타입에 따른 기본 색상 반환
     * 
     * @param type 일정 타입
     * @return ARGB 색상값 (ULong)
     */
    private fun getStandardColor(type: ScheduleType): ULong {
        return when (type) {
            ScheduleType.PERSONAL -> ScheduleColor1.value
            ScheduleType.WORK -> ScheduleColor2.value
            ScheduleType.PROJECT -> ScheduleColor3.value
            ScheduleType.MEETING -> ScheduleColor4.value
            ScheduleType.OTHER -> ScheduleColor5.value
            ScheduleType.IMPORTANT -> ScheduleColor6.value
            ScheduleType.DEADLINE -> ScheduleColor7.value
        }
    }
    /**
     * 일정 타입에 따른 색상 반환
     * 
     * @param type 일정 타입
     * @return ARGB 색상값 (ULong)
     */
    fun getColor(type: ScheduleType): ULong {
        return when (type) {
            ScheduleType.IMPORTANT -> ScheduleColor1.value
            ScheduleType.DEADLINE -> ScheduleColor2.value
            ScheduleType.MEETING -> ScheduleColor3.value
            ScheduleType.WORK -> ScheduleColor4.value
            ScheduleType.PROJECT -> ScheduleColor5.value
            ScheduleType.PERSONAL -> ScheduleColor6.value
            ScheduleType.OTHER -> ScheduleColor7.value
        }
    }
    
    /**
     * 일정 타입에 따른 고대비 색상 반환
     * 
     * @param type 일정 타입
     * @return 고대비 ARGB 색상값 (ULong)
     */
    private fun getHighContrastColor(type: ScheduleType): ULong {
        // 고대비 모드용 색상은 더 선명하고 대비가 높은 색상으로 설정
        return when (type) {
            ScheduleType.PERSONAL -> ScheduleHighContrastColor1.value
            ScheduleType.WORK -> ScheduleHighContrastColor2.value
            ScheduleType.PROJECT -> ScheduleHighContrastColor3.value
            ScheduleType.MEETING -> ScheduleHighContrastColor4.value
            ScheduleType.OTHER -> ScheduleHighContrastColor5.value
            ScheduleType.IMPORTANT -> ScheduleHighContrastColor6.value
            ScheduleType.DEADLINE -> ScheduleHighContrastColor7.value
        }
    }
    
    /**
     * 일정의 제목과 내용을 분석하여 일정 타입을 유추합니다.
     * 
     * @param schedule 분석할 일정
     * @return 유추된 일정 타입
     */
    fun inferScheduleType(schedule: Schedule): ScheduleType {
        val titleLower = schedule.title.value.lowercase()
        val contentLower = schedule.content.value.lowercase() ?: ""
        
        // 키워드 세트 정의 (더 많은 키워드 추가)
        val importantKeywords = setOf("긴급", "중요", "우선", "필수", "critical", "urgent", "important")
        val deadlineKeywords = setOf("마감", "기한", "데드라인", "deadline", "due", "종료", "마지막")
        val meetingKeywords = setOf("회의", "미팅", "미티", "meeting", "conference", "세미나", "토론", "발표")
        val workKeywords = setOf("업무", "작업", "태스크", "task", "work", "job", "리포트", "보고")
        val projectKeywords = setOf("프로젝트", "개발", "기획", "project", "dev", "개선", "구현", "연구")
        val personalKeywords = setOf("개인", "휴가", "휴식", "personal", "vacation", "rest", "여행", "취미")
        
        // 키워드 확인 함수
        fun containsAny(text: String, keywords: Set<String>): Boolean {
            return keywords.any { text.contains(it) }
        }
        
        // 제목이나 내용에 특정 키워드가 포함된 경우 해당 타입으로 분류
        return when {
            // 중요 일정 (긴급, 중요, 우선 등의 키워드)
            containsAny(titleLower, importantKeywords) || containsAny(contentLower, importantKeywords) -> 
                ScheduleType.IMPORTANT
                
            // 마감 일정 (마감, 데드라인, 기한 등의 키워드)
            containsAny(titleLower, deadlineKeywords) || containsAny(contentLower, deadlineKeywords) ->
                ScheduleType.DEADLINE
                
            // 미팅 일정 (회의, 미팅, 미티 등의 키워드)
            containsAny(titleLower, meetingKeywords) || containsAny(contentLower, meetingKeywords) ->
                ScheduleType.MEETING
                
            // 업무 일정 (업무, 작업, 태스크 등의 키워드)
            containsAny(titleLower, workKeywords) || containsAny(contentLower, workKeywords) ->
                ScheduleType.WORK
                
            // 프로젝트 일정 (프로젝트, 개발, 기획 등의 키워드)
            containsAny(titleLower, projectKeywords) || containsAny(contentLower, projectKeywords) ->
                ScheduleType.PROJECT
                
            // 개인 일정 (개인, 휴가, 휴식 등의 키워드)
            containsAny(titleLower, personalKeywords) || containsAny(contentLower, personalKeywords) ->
                ScheduleType.PERSONAL
                
            // 기타 일정 (위 카테고리에 해당하지 않는 경우)
            else -> ScheduleType.OTHER
        }
    }
}