package com.example.feature_schedule.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.util.DateTimeUtil
import com.example.core_ui.theme.*
import com.example.domain.model.Schedule
import com.example.domain.usecase.schedule.DeleteScheduleUseCase
import com.example.domain.usecase.schedule.GetSchedulesForDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import com.example.core_navigation.destination.AppRoutes
import com.example.domain.model.ScheduleItem24Hour
import java.time.Instant
import android.util.Log

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
        colorManager.setProjectDefaultType("project-special", ScheduleType.IMPORTANT)
    }

    /**
     * 지정된 날짜의 일정을 로드합니다.
     * 
     * @param date 로드할 일정 날짜
     */
    private fun loadSchedules(date: LocalDate) {
        viewModelScope.launch {
            _uiState.value = Calendar24HourUiState.Loading
            Log.d("CalendarVM", "loadSchedules($date) 호출됨. UI 상태: Loading")
            
            val result = getSchedulesForDateUseCase(date)
            Log.d("CalendarVM", "loadSchedules($date) - getSchedulesForDateUseCase 결과: isSuccess=${result.isSuccess}, data=${if (result.isSuccess) result.getOrNull() else result.exceptionOrNull()}")
            
            if (result.isSuccess) {
                val schedulesDomain = result.getOrThrow()
                Log.d("CalendarVM", "loadSchedules($date) - 성공. Domain schedules count: ${schedulesDomain.size}")
                val schedulesUi = schedulesDomain.map { schedule ->
                    // 기본 색상 계산
                    val color = colorManager.getColorForSchedule(schedule)
                    
                    // 시간 기반 그라데이션 효과를 위한 알파값 계산
                    val (startAlpha, endAlpha) = colorManager.calculateTimeBasedAlpha(
                        schedule.startTime,
                        schedule.endTime
                    )
                    
                    ScheduleItem24Hour(
                        id = schedule.id,
                        title = schedule.title,
                        startTime = schedule.startTime,
                        endTime = schedule.endTime,
                        color = color,
                        startColorAlpha = startAlpha,
                        endColorAlpha = endAlpha
                    )
                }
                _uiState.value = Calendar24HourUiState.Success(date, schedulesUi)
                Log.d("CalendarVM", "loadSchedules($date) - UI 상태: Success. UI schedules count: ${schedulesUi.size}, 데이터: $schedulesUi")
            } else {
                _uiState.value = Calendar24HourUiState.Error("스케줄 로딩 실패: ${result.exceptionOrNull()?.message ?: "알 수 없는 오류"}")
                Log.e("CalendarVM", "loadSchedules($date) - UI 상태: Error", result.exceptionOrNull())
            }
        }
    }

    /**
     * 고대비 모드 설정을 변경합니다.
     * 
     * @param enabled 고대비 모드 활성화 여부
     */
    fun setHighContrastMode(enabled: Boolean) {
        colorManager.setHighContrastMode(enabled)
        // 설정이 변경되었으므로 현재 날짜의 일정 다시 로드
        val currentDate = (uiState.value as? Calendar24HourUiState.Success)?.selectedDate 
            ?: LocalDate.of(year, month, day)
        loadSchedules(currentDate)
    }

    /**
     * 그라데이션 효과 설정을 변경합니다.
     * 
     * @param enabled 그라데이션 효과 활성화 여부
     */
    fun setGradientEffect(enabled: Boolean) {
        colorManager.setGradientEffect(enabled)
        // 설정이 변경되었으므로 현재 날짜의 일정 다시 로드
        val currentDate = (uiState.value as? Calendar24HourUiState.Success)?.selectedDate 
            ?: LocalDate.of(year, month, day)
        loadSchedules(currentDate)
    }

    /**
     * 일정을 삭제합니다.
     * 
     * @param scheduleId 삭제할 일정 ID
     */
    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            val result = deleteScheduleUseCase(scheduleId)
            
            if (result.isSuccess) {
                // 성공 시, 현재 상태가 Success이면 해당 스케줄 제거 후 UI 업데이트
                val currentState = _uiState.value
                if (currentState is Calendar24HourUiState.Success) {
                    _uiState.value = currentState.copy(
                        schedules = currentState.schedules.filterNot { it.id == scheduleId }
                    )
                    _eventFlow.emit(Calendar24HourEvent.ShowSnackbar("일정이 삭제되었습니다."))
                }
            } else {
                _eventFlow.emit(Calendar24HourEvent.ShowSnackbar("일정 삭제 실패: ${result.exceptionOrNull()?.message ?: "알 수 없는 오류"}"))
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

    fun refreshSchedulesForCurrentDate() {
        val currentUiState = _uiState.value
        val dateToReload = if (currentUiState is Calendar24HourUiState.Success) {
            currentUiState.selectedDate
        } else {
            // Fallback to the initial date if current state is not Success or selectedDate is null
            LocalDate.of(year, month, day)
        }
        // Ensure dateToReload is not null before calling loadSchedules
        // Although in this logic, it should always be non-null due to LocalDate.of fallback
        dateToReload?.let { loadSchedules(it) }
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
    
    // 프로젝트 ID를 일정 타입에 매핑 (커스터마이징용)
    private val projectTypeMap = mutableMapOf<String, ScheduleType>()
    
    // 고대비 모드 설정
    private var isHighContrastMode = false
    
    // 그라데이션 효과 설정
    private var useGradientEffect = true
    
    // 하루 시간대 표현 (오전, 오후, 저녁, 밤)
    private val morningStart = LocalTime.of(6, 0)
    private val afternoonStart = LocalTime.of(12, 0)
    private val eveningStart = LocalTime.of(18, 0)
    private val nightStart = LocalTime.of(22, 0)
    
    /**
     * 고대비 모드 설정
     * 
     * @param enabled 고대비 모드 활성화 여부
     */
    fun setHighContrastMode(enabled: Boolean) {
        isHighContrastMode = enabled
    }
    
    /**
     * 그라데이션 효과 설정
     * 
     * @param enabled 그라데이션 효과 활성화 여부
     */
    fun setGradientEffect(enabled: Boolean) {
        useGradientEffect = enabled
    }
    
    /**
     * 특정 프로젝트의 기본 일정 타입 설정
     * 
     * @param projectId 프로젝트 ID
     * @param type 설정할 일정 타입
     */
    fun setProjectDefaultType(projectId: String, type: ScheduleType) {
        projectTypeMap[projectId] = type
    }
    
    /**
     * 일정의 타입에 따른 색상 반환
     * 
     * @param schedule 색상을 얻을 일정
     * @return ARGB 색상값 (ULong)
     */
    fun getColorForSchedule(schedule: Schedule): ULong {
        // 프로젝트 ID가 null이면 기본 타입(개인) 사용
        val projectId = schedule.projectId ?: ""
        
        // 이미 매핑된 프로젝트 ID가 있으면 해당 색상 반환
        if (projectColorMap.containsKey(projectId)) {
            return projectColorMap[projectId]!!
        }
        
        // 프로젝트 기본 타입이 설정되어 있으면 해당 타입 사용
        val type = if (projectId.isNotEmpty() && projectTypeMap.containsKey(projectId)) {
            projectTypeMap[projectId]!!
        } else {
            // 설정이 없으면 일정 내용에서 타입 유추
            inferScheduleType(schedule)
        }
        
        // 타입에 따라 색상 할당 (고대비 모드 여부에 따라 다른 색상 세트 사용)
        val color = if (isHighContrastMode) {
            getHighContrastColor(type)
        } else {
            getStandardColor(type)
        }
        
        // 프로젝트 ID에 색상 매핑 저장 (빈 문자열이 아닌 경우에만)
        if (projectId.isNotEmpty()) {
            projectColorMap[projectId] = color
        }
        
        return color
    }
    
    /**
     * 시간대에 따른 알파값 계산
     * 
     * @param time 시간 (LocalTime)
     * @return 해당 시간에 적용할 알파값 (0.6f ~ 1.0f)
     */
    fun getAlphaForTime(time: LocalTime): Float {
        // 시간대별 기본 알파값 (낮 - 밝게, 밤 - 어둡게)
        return when {
            // 새벽 (어두움 -> 밝아짐)
            time.isBefore(morningStart) -> {
                // 0시에 가장 어둡고(0.6f), 아침에 가까울수록 밝아짐
                val progress = time.toSecondOfDay().toFloat() / morningStart.toSecondOfDay()
                0.6f + (0.4f * progress)
            }
            // 오전 (밝음)
            time.isBefore(afternoonStart) -> {
                // 오전은 일관되게 밝음 (1.0f)
                1.0f
            }
            // 오후 (약간 어두워짐)
            time.isBefore(eveningStart) -> {
                // 오후는 약간 어두움 (0.9f)
                0.9f
            }
            // 저녁 (점점 더 어두워짐)
            time.isBefore(nightStart) -> {
                // 저녁에서 밤으로 갈수록 어두워짐
                val totalSeconds = (nightStart.toSecondOfDay() - eveningStart.toSecondOfDay()).toFloat()
                val progress = (time.toSecondOfDay() - eveningStart.toSecondOfDay()) / totalSeconds
                0.9f - (0.2f * progress)
            }
            // 밤 (가장 어두움)
            else -> {
                // 밤은 가장 어두움 (0.7f)
                0.7f
            }
        }
    }
    
    /**
     * 일정 시작 및 종료 시간에 따른 알파값 쌍 계산 (Instant 버전)
     * 
     * @param startInstant 일정 시작 시간 (Instant)
     * @param endInstant 일정 종료 시간 (Instant)
     * @return 시작 및 종료 시간에 대한 알파값 쌍 (그라데이션 효과가 비활성화되면 둘 다 1.0f)
     */
    fun calculateTimeBasedAlpha(startInstant: Instant, endInstant: Instant): Pair<Float, Float> {
        if (!useGradientEffect) {
            return Pair(1.0f, 1.0f)
        }
        
        // Instant를 LocalTime으로 변환
        val startAlpha = getAlphaForTime(DateTimeUtil.toLocalTime(startInstant) ?: LocalTime.NOON)
        val endAlpha = getAlphaForTime( DateTimeUtil.toLocalTime(endInstant) ?: LocalTime.NOON )

        return Pair(startAlpha, endAlpha)
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
    private fun inferScheduleType(schedule: Schedule): ScheduleType {
        val titleLower = schedule.title.lowercase()
        val contentLower = schedule.content?.lowercase() ?: ""
        
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