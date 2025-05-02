package com.example.feature_schedule.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.data.repository.FakeProjectRepository
import com.example.data.repository.FakeScheduleRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import com.example.domain.model.Project
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
import java.util.UUID

/**
 * AddScheduleViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 AddScheduleViewModel의 기능을 검증합니다.
 * FakeScheduleRepository와 FakeProjectRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class AddScheduleViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: AddScheduleViewModel

    // Fake Repository
    private lateinit var fakeScheduleRepository: FakeScheduleRepository
    private lateinit var fakeProjectRepository: FakeProjectRepository
    
    // SavedStateHandle Mock
    private lateinit var savedStateHandle: SavedStateHandle

    // 테스트 데이터
    private val testDate = LocalDate.of(2023, 5, 15)
    private val testYear = testDate.year
    private val testMonth = testDate.monthValue
    private val testDay = testDate.dayOfMonth

    private val testProjects = listOf(
        Project(
            id = "project-1",
            name = "개인 프로젝트",
            description = "개인 일정 관리용 프로젝트",
            imageUrl = null,
            memberCount = 1,
            isPublic = false
        ),
        Project(
            id = "project-2",
            name = "스터디 그룹",
            description = "스터디 일정 관리용 프로젝트",
            imageUrl = null,
            memberCount = 5,
            isPublic = true
        ),
        Project(
            id = "project-3",
            name = "회사 업무",
            description = "업무 일정 관리용 프로젝트",
            imageUrl = null,
            memberCount = 10,
            isPublic = false
        )
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
        fakeProjectRepository = FakeProjectRepository()
        
        // 테스트 프로젝트 추가
        testProjects.forEach { fakeProjectRepository.addProject(it) }
    }

    /**
     * 초기 상태에서 선택된 날짜가 올바르게 설정되는지 테스트
     */
    @Test
    fun `초기화 시 SavedStateHandle에서 전달된 날짜가 설정되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 설정된 테스트 환경 (setup에서 설정됨)
        
        // When: ViewModel 초기화
        viewModel = AddScheduleViewModel(savedStateHandle, fakeProjectRepository, fakeScheduleRepository)
        
        // Then: 초기 상태 확인
        val uiState = viewModel.uiState.getValue()
        assertEquals(testDate, uiState.selectedDate)
    }

    /**
     * 사용 가능한 프로젝트 목록이 로드되는지 테스트
     */
    @Test
    fun `초기화 시 사용 가능한 프로젝트 목록이 로드되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 설정된 테스트 환경 (setup에서 설정됨)
        
        // When: ViewModel 초기화
        viewModel = AddScheduleViewModel(savedStateHandle, fakeProjectRepository, fakeScheduleRepository)
        
        // Then: 프로젝트 목록 로드 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.availableProjects.isEmpty())
        assertEquals(testProjects.size, uiState.availableProjects.size)
        
        // 프로젝트 이름과 ID 확인
        testProjects.forEachIndexed { index, project ->
            val uiProject = uiState.availableProjects[index]
            assertEquals(project.id, uiProject.id)
            assertEquals(project.name, uiProject.name)
        }
    }

    /**
     * 프로젝트 선택 테스트
     */
    @Test
    fun `프로젝트 선택 시 선택된 프로젝트가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = AddScheduleViewModel(savedStateHandle, fakeProjectRepository, fakeScheduleRepository)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertNull(initialState.selectedProject)
        
        // When: 프로젝트 선택
        val projectToSelect = initialState.availableProjects[1] // 두 번째 프로젝트 선택
        viewModel.onProjectSelected(projectToSelect)
        
        // Then: 선택된 프로젝트 확인
        val updatedState = viewModel.uiState.getValue()
        assertNotNull(updatedState.selectedProject)
        assertEquals(projectToSelect.id, updatedState.selectedProject?.id)
        assertEquals(projectToSelect.name, updatedState.selectedProject?.name)
    }

    /**
     * 제목 입력 테스트
     */
    @Test
    fun `제목 입력 시 UI 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = AddScheduleViewModel(savedStateHandle, fakeProjectRepository, fakeScheduleRepository)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertTrue(initialState.scheduleTitle.isEmpty())
        
        // When: 제목 입력
        val title = "테스트 일정 제목"
        viewModel.onTitleChange(title)
        
        // Then: 제목 업데이트 확인
        val updatedState = viewModel.uiState.getValue()
        assertEquals(title, updatedState.scheduleTitle)
    }

    /**
     * 내용 입력 테스트
     */
    @Test
    fun `내용 입력 시 UI 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = AddScheduleViewModel(savedStateHandle, fakeProjectRepository, fakeScheduleRepository)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertTrue(initialState.scheduleContent.isEmpty())
        
        // When: 내용 입력
        val content = "테스트 일정 내용"
        viewModel.onContentChange(content)
        
        // Then: 내용 업데이트 확인
        val updatedState = viewModel.uiState.getValue()
        assertEquals(content, updatedState.scheduleContent)
    }

    /**
     * 시간 선택 테스트
     */
    @Test
    fun `시작 및 종료 시간 선택 시 UI 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = AddScheduleViewModel(savedStateHandle, fakeProjectRepository, fakeScheduleRepository)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertNull(initialState.startTime)
        assertNull(initialState.endTime)
        
        // When: 시작 시간 선택
        val startHour = 10
        val startMinute = 0
        viewModel.onStartTimeSelected(startHour, startMinute)
        
        // Then: 시작 시간 업데이트 확인
        var updatedState = viewModel.uiState.getValue()
        assertNotNull(updatedState.startTime)
        assertEquals(LocalTime.of(startHour, startMinute), updatedState.startTime)
        
        // When: 종료 시간 선택
        val endHour = 11
        val endMinute = 30
        viewModel.onEndTimeSelected(endHour, endMinute)
        
        // Then: 종료 시간 업데이트 확인
        updatedState = viewModel.uiState.getValue()
        assertNotNull(updatedState.endTime)
        assertEquals(LocalTime.of(endHour, endMinute), updatedState.endTime)
        assertTrue(updatedState.isTimeValid) // 시간 범위 유효성 확인
    }

    /**
     * 시간 유효성 검사 테스트 (종료 시간이 시작 시간 이전인 경우)
     */
    @Test
    fun `종료 시간이 시작 시간보다 이전인 경우 유효성 오류가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = AddScheduleViewModel(savedStateHandle, fakeProjectRepository, fakeScheduleRepository)
        
        // When: 시작 시간을 종료 시간보다 늦게 설정
        viewModel.onStartTimeSelected(12, 0) // 12:00
        viewModel.onEndTimeSelected(11, 0) // 11:00 (시작 시간보다 이전)
        
        // Then: 시간 유효성 검사 오류 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isTimeValid)
        assertNotNull(uiState.timeError)
    }

    /**
     * 일정 저장 성공 테스트
     */
    @Test
    fun `유효한 입력으로 일정 저장 시 성공해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel과 유효한 입력값
        viewModel = AddScheduleViewModel(savedStateHandle, fakeProjectRepository, fakeScheduleRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<AddScheduleEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // 필수 정보 입력
        val projectToSelect = viewModel.uiState.getValue().availableProjects[0]
        viewModel.onProjectSelected(projectToSelect)
        viewModel.onTitleChange("테스트 일정")
        viewModel.onContentChange("테스트 일정 내용")
        viewModel.onStartTimeSelected(10, 0)
        viewModel.onEndTimeSelected(11, 30)
        
        // When: 저장 버튼 클릭
        viewModel.onSaveClick()
        
        // Then: 저장 성공 확인
        val finalState = viewModel.uiState.getValue()
        assertTrue(finalState.saveSuccess)
        assertFalse(finalState.isLoading)
        
        // 스낵바 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is AddScheduleEvent.ShowSnackbar)
        assertTrue((event as AddScheduleEvent.ShowSnackbar).message.contains("추가되었습니다"))
        
        // 저장된 일정 확인
        val date = testDate
        val schedules = fakeScheduleRepository.getSchedulesForDate(date).getOrNull()
        assertNotNull(schedules)
        assertFalse(schedules!!.isEmpty())
        
        // 저장된 일정 내용 확인
        val savedSchedule = schedules.first()
        assertEquals("테스트 일정", savedSchedule.title)
        assertEquals("테스트 일정 내용", savedSchedule.content)
        assertEquals(LocalTime.of(10, 0), savedSchedule.startTime.toLocalTime())
        assertEquals(LocalTime.of(11, 30), savedSchedule.endTime.toLocalTime())
        assertEquals(projectToSelect.id, savedSchedule.projectId)
    }

    /**
     * 필수 정보 누락 시 유효성 검사 테스트
     */
    @Test
    fun `필수 정보 누락 시 유효성 오류가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = AddScheduleViewModel(savedStateHandle, fakeProjectRepository, fakeScheduleRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<AddScheduleEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 제목을 입력하지 않고 저장 버튼 클릭
        viewModel.onSaveClick()
        
        // Then: 유효성 오류 확인
        val uiState = viewModel.uiState.getValue()
        assertNotNull(uiState.titleError) // 제목 오류
        assertFalse(uiState.saveSuccess)
        
        // 스낵바 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is AddScheduleEvent.ShowSnackbar)
        assertTrue((event as AddScheduleEvent.ShowSnackbar).message.contains("입력값을 확인해주세요"))
    }

    /**
     * 프로젝트 목록 로드 실패 테스트
     */
    @Test
    fun `프로젝트 목록 로드 실패 시 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러를 시뮬레이션하도록 설정
        fakeProjectRepository.setShouldSimulateError(true)
        
        // When: ViewModel 초기화
        viewModel = AddScheduleViewModel(savedStateHandle, fakeProjectRepository, fakeScheduleRepository)
        
        // Then: 에러 메시지 확인
        val uiState = viewModel.uiState.getValue()
        assertNotNull(uiState.errorMessage)
        assertTrue(uiState.errorMessage!!.contains("프로젝트 목록 로드 실패"))
    }

    /**
     * 일정 저장 실패 테스트
     */
    @Test
    fun `일정 저장 실패 시 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel과 저장 시 에러 설정
        viewModel = AddScheduleViewModel(savedStateHandle, fakeProjectRepository, fakeScheduleRepository)
        
        // 필수 정보 입력
        val projectToSelect = viewModel.uiState.getValue().availableProjects[0]
        viewModel.onProjectSelected(projectToSelect)
        viewModel.onTitleChange("테스트 일정")
        viewModel.onStartTimeSelected(10, 0)
        viewModel.onEndTimeSelected(11, 30)
        
        // 저장 시 에러 발생하도록 설정
        fakeScheduleRepository.setShouldSimulateError(true)
        
        // When: 저장 버튼 클릭
        viewModel.onSaveClick()
        
        // Then: 에러 메시지 확인
        val uiState = viewModel.uiState.getValue()
        assertNotNull(uiState.errorMessage)
        assertTrue(uiState.errorMessage!!.contains("일정 저장 실패"))
        assertFalse(uiState.saveSuccess)
    }
} 