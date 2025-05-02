package com.example.feature_schedule.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.data.repository.FakeScheduleRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import com.example.domain.model.Schedule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Calendar24HourViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 Calendar24HourViewModel의 기능을 검증합니다.
 * FakeScheduleRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class Calendar24HourViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: Calendar24HourViewModel

    // Fake Repository
    private lateinit var fakeScheduleRepository: FakeScheduleRepository
    
    // SavedStateHandle Mock
    private lateinit var savedStateHandle: SavedStateHandle

    // 테스트 데이터
    private val testDate = LocalDate.of(2023, 5, 15)
    private val testYear = testDate.year
    private val testMonth = testDate.monthValue
    private val testDay = testDate.dayOfMonth

    private val testSchedule1 = Schedule(
        id = "schedule-1",
        projectId = "project-1",
        title = "미팅 일정 1",
        content = "중요 미팅 내용 1",
        startTime = LocalDateTime.of(testDate, LocalTime.of(10, 0)),
        endTime = LocalDateTime.of(testDate, LocalTime.of(11, 30)),
        attendees = listOf("user-1"),
        isAllDay = false
    )

    private val testSchedule2 = Schedule(
        id = "schedule-2",
        projectId = "project-2",
        title = "개인 휴가",
        content = "개인 휴식 시간",
        startTime = LocalDateTime.of(testDate, LocalTime.of(14, 0)),
        endTime = LocalDateTime.of(testDate, LocalTime.of(15, 30)),
        attendees = listOf("user-1", "user-2"),
        isAllDay = false
    )

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // SavedStateHandle Mock 설정
        savedStateHandle = mock(SavedStateHandle::class.java)
        
        // 날짜 매개변수 설정
        `when`(savedStateHandle.get<Int>("year")).thenReturn(testYear)
        `when`(savedStateHandle.get<Int>("month")).thenReturn(testMonth)
        `when`(savedStateHandle.get<Int>("day")).thenReturn(testDay)
        
        // Fake Repository 초기화
        fakeScheduleRepository = FakeScheduleRepository()
        
        // 테스트 일정 추가
        fakeScheduleRepository.addScheduleData(testSchedule1)
        fakeScheduleRepository.addScheduleData(testSchedule2)
    }

    /**
     * 초기 데이터 로딩 성공 테스트
     */
    @Test
    fun `초기화 시 선택된 날짜의 일정을 성공적으로 로드해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 설정된 테스트 환경 (setup에서 설정됨)
        
        // When: ViewModel 초기화 (init에서 loadSchedules 호출)
        viewModel = Calendar24HourViewModel(savedStateHandle, fakeScheduleRepository)
        
        // Then: 로드된 일정 확인
        val uiState = viewModel.uiState.getValue()
        assertTrue(uiState is Calendar24HourUiState.Success)
        
        val successState = uiState as Calendar24HourUiState.Success
        assertEquals(testDate, successState.selectedDate)
        assertEquals(2, successState.schedules.size)
        
        // 로드된 일정 내용 확인
        val schedule1 = successState.schedules.find { it.id == testSchedule1.id }
        val schedule2 = successState.schedules.find { it.id == testSchedule2.id }
        
        assertNotNull(schedule1)
        assertNotNull(schedule2)
        
        assertEquals(testSchedule1.title, schedule1?.title)
        assertEquals(LocalTime.of(10, 0), schedule1?.startTime)
        assertEquals(LocalTime.of(11, 30), schedule1?.endTime)
        
        assertEquals(testSchedule2.title, schedule2?.title)
        assertEquals(LocalTime.of(14, 0), schedule2?.startTime)
        assertEquals(LocalTime.of(15, 30), schedule2?.endTime)
    }

    /**
     * 초기 데이터 로딩 실패 테스트
     */
    @Test
    fun `일정 로딩 중 오류 발생 시 에러 상태로 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러를 시뮬레이션하도록 설정
        fakeScheduleRepository.setShouldSimulateError(true)
        
        // When: ViewModel 초기화 (init에서 loadSchedules 호출)
        viewModel = Calendar24HourViewModel(savedStateHandle, fakeScheduleRepository)
        
        // Then: 에러 상태 확인
        val uiState = viewModel.uiState.getValue()
        assertTrue(uiState is Calendar24HourUiState.Error)
        
        val errorState = uiState as Calendar24HourUiState.Error
        assertNotNull(errorState.message)
        assertTrue(errorState.message.isNotEmpty())
    }

    /**
     * 일정 삭제 성공 테스트
     */
    @Test
    fun `일정 삭제 성공 시 UI 상태에서 제거되고 성공 스낵바가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = Calendar24HourViewModel(savedStateHandle, fakeScheduleRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<Calendar24HourEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue() as Calendar24HourUiState.Success
        assertEquals(2, initialState.schedules.size)
        
        // When: 일정 삭제
        viewModel.deleteSchedule("schedule-1")
        
        // Then: UI 상태 업데이트 확인
        val updatedState = viewModel.uiState.getValue() as Calendar24HourUiState.Success
        assertEquals(1, updatedState.schedules.size)
        assertNull(updatedState.schedules.find { it.id == "schedule-1" })
        assertNotNull(updatedState.schedules.find { it.id == "schedule-2" })
        
        // 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is Calendar24HourEvent.ShowSnackbar)
        assertTrue((event as Calendar24HourEvent.ShowSnackbar).message.contains("삭제되었습니다"))
    }

    /**
     * 일정 삭제 실패 테스트
     */
    @Test
    fun `일정 삭제 실패 시 에러 스낵바가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = Calendar24HourViewModel(savedStateHandle, fakeScheduleRepository)
        
        // 에러 설정
        fakeScheduleRepository.setShouldSimulateError(true)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<Calendar24HourEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 일정 삭제
        viewModel.deleteSchedule("schedule-1")
        
        // Then: 에러 스낵바 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is Calendar24HourEvent.ShowSnackbar)
        assertTrue((event as Calendar24HourEvent.ShowSnackbar).message.contains("삭제 실패"))
    }

    /**
     * 뒤로 가기 이벤트 테스트
     */
    @Test
    fun `뒤로 가기 버튼 클릭 시 NavigateBack 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = Calendar24HourViewModel(savedStateHandle, fakeScheduleRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<Calendar24HourEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 뒤로 가기 버튼 클릭
        viewModel.onBackClick()
        
        // Then: NavigateBack 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is Calendar24HourEvent.NavigateBack)
    }

    /**
     * 일정 추가 네비게이션 이벤트 테스트
     */
    @Test
    fun `일정 추가 버튼 클릭 시 NavigateToAddSchedule 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = Calendar24HourViewModel(savedStateHandle, fakeScheduleRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<Calendar24HourEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 일정 추가 버튼 클릭
        viewModel.onAddScheduleClick()
        
        // Then: NavigateToAddSchedule 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is Calendar24HourEvent.NavigateToAddSchedule)
    }

    /**
     * 일정 클릭 네비게이션 이벤트 테스트
     */
    @Test
    fun `일정 클릭 시 NavigateToScheduleDetail 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = Calendar24HourViewModel(savedStateHandle, fakeScheduleRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<Calendar24HourEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 일정 클릭
        val scheduleId = "schedule-1"
        viewModel.onScheduleClick(scheduleId)
        
        // Then: NavigateToScheduleDetail 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is Calendar24HourEvent.NavigateToScheduleDetail)
        assertEquals(scheduleId, (event as Calendar24HourEvent.NavigateToScheduleDetail).scheduleId)
    }

    /**
     * 일정 롱클릭 이벤트 테스트
     */
    @Test
    fun `일정 롱클릭 시 ShowScheduleEditDialog 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = Calendar24HourViewModel(savedStateHandle, fakeScheduleRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<Calendar24HourEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 일정 롱클릭
        val scheduleId = "schedule-2"
        viewModel.onScheduleLongClick(scheduleId)
        
        // Then: ShowScheduleEditDialog 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is Calendar24HourEvent.ShowScheduleEditDialog)
        assertEquals(scheduleId, (event as Calendar24HourEvent.ShowScheduleEditDialog).scheduleId)
    }

    /**
     * 고대비 모드 테스트
     */
    @Test
    fun `고대비 모드 활성화 시 일정 색상이 변경되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = Calendar24HourViewModel(savedStateHandle, fakeScheduleRepository)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue() as Calendar24HourUiState.Success
        val initialSchedule1 = initialState.schedules.find { it.id == "schedule-1" }
        val initialColorForSchedule1 = initialSchedule1?.color
        
        // When: 고대비 모드 활성화
        viewModel.setHighContrastMode(true)
        
        // Then: 색상 변경 확인
        val updatedState = viewModel.uiState.getValue() as Calendar24HourUiState.Success
        val updatedSchedule1 = updatedState.schedules.find { it.id == "schedule-1" }
        val updatedColorForSchedule1 = updatedSchedule1?.color
        
        // 색상이 변경되었는지 확인
        assertNotNull(initialColorForSchedule1)
        assertNotNull(updatedColorForSchedule1)
        assertNotEquals(initialColorForSchedule1.toString(), updatedColorForSchedule1.toString())
    }

    /**
     * 그라데이션 효과 테스트
     */
    @Test
    fun `그라데이션 효과 설정 변경 시 알파값이 변경되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel (기본적으로 그라데이션 활성화 상태)
        viewModel = Calendar24HourViewModel(savedStateHandle, fakeScheduleRepository)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue() as Calendar24HourUiState.Success
        val initialSchedule = initialState.schedules.find { it.id == "schedule-1" }
        
        // 초기 상태에서는 시간에 따른 다양한 알파값을 가질 수 있음
        val initialStartAlpha = initialSchedule?.startColorAlpha
        val initialEndAlpha = initialSchedule?.endColorAlpha
        
        // When: 그라데이션 효과 비활성화
        viewModel.setGradientEffect(false)
        
        // Then: 알파값이 변경되었는지 확인
        val updatedState = viewModel.uiState.getValue() as Calendar24HourUiState.Success
        val updatedSchedule = updatedState.schedules.find { it.id == "schedule-1" }
        
        // 그라데이션 효과 비활성화 시 시작과 끝 알파값이 모두 1.0f
        assertNotNull(updatedSchedule)
        assertEquals(1.0f, updatedSchedule?.startColorAlpha)
        assertEquals(1.0f, updatedSchedule?.endColorAlpha)
    }
} 