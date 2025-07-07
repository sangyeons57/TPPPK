package com.example.feature_project_detail.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.data.repository.FakeProjectRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.domain.model.Project
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * JoinProjectViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 JoinProjectViewModel의 기능을 검증합니다.
 * FakeProjectRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class JoinProjectViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: com.example.feature_join_project.JoinProjectViewModel

    // Fake Repository
    private lateinit var fakeProjectRepository: FakeProjectRepository
    
    // SavedStateHandle Mock
    private lateinit var savedStateHandle: SavedStateHandle

    // 테스트 데이터
    private val testProject = Project(
        id = "test-project-id",
        name = "테스트 프로젝트",
        description = "테스트용 프로젝트 설명",
        imageUrl = null,
        memberCount = 1,
        isPublic = true
    )

    private val testInviteCode = "test-code-123"

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // SavedStateHandle Mock 설정
        savedStateHandle = mock(SavedStateHandle::class.java)
        
        // Fake Repository 초기화
        fakeProjectRepository = FakeProjectRepository()
        
        // 테스트 프로젝트 및 초대 코드 설정
        fakeProjectRepository.addProject(testProject)
        fakeProjectRepository.setProjectCode(testInviteCode, testProject.id)
        
        // ViewModel 초기화
        viewModel = com.example.feature_join_project.JoinProjectViewModel(
            savedStateHandle,
            fakeProjectRepository
        )
    }

    /**
     * 초기 상태 테스트
     */
    @Test
    fun `초기화 시 기본 상태가 정확해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel (setup에서 이미 수행됨)
        
        // When: 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        
        // Then: 초기 상태 검증
        assertEquals("", initialState.inviteCodeOrLink)
        assertFalse(initialState.isLoading)
        assertNull(initialState.error)
    }

    /**
     * 코드 입력 변경 테스트
     */
    @Test
    fun `초대 코드 입력 시 UI 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        
        // When: 코드 입력 변경
        val testInput = "new-invite-code"
        viewModel.onCodeOrLinkChange(testInput)
        
        // Then: 상태 업데이트 확인
        val updatedState = viewModel.uiState.getValue()
        assertEquals(testInput, updatedState.inviteCodeOrLink)
        assertNull(updatedState.error)
    }

    /**
     * 오류 상태에서 코드 입력 변경 시 오류 초기화 테스트
     */
    @Test
    fun `오류 상태에서 코드 입력 변경 시 오류가 초기화되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 오류 상태의 ViewModel
        viewModel.joinProject() // 빈 코드로 시도하여 오류 발생
        val errorState = viewModel.uiState.getValue()
        assertNotNull(errorState.error)
        
        // When: 코드 입력 변경
        viewModel.onCodeOrLinkChange("new-code")
        
        // Then: 오류 초기화 확인
        val updatedState = viewModel.uiState.getValue()
        assertNull(updatedState.error)
    }

    /**
     * 빈 코드로 참여 시도 테스트
     */
    @Test
    fun `빈 초대 코드로 참여 시도 시 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel (빈 코드)
        
        // When: 빈 코드로 참여 시도
        viewModel.joinProject()
        
        // Then: 오류 메시지 확인
        val uiState = viewModel.uiState.getValue()
        assertNotNull(uiState.error)
        assertEquals("초대 링크 또는 코드를 입력해주세요.", uiState.error)
        assertFalse(uiState.isLoading)
    }

    /**
     * 유효한 코드로 프로젝트 참여 성공 테스트
     */
    @Test
    fun `유효한 초대 코드로 참여 시 성공 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel.onCodeOrLinkChange(testInviteCode)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<com.example.feature_join_project.JoinProjectEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 유효한 코드로 참여 시도
        viewModel.joinProject()
        
        // Then: 성공 이벤트 및 상태 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        
        // 이벤트 확인 (순서대로: ClearFocus, ShowSnackbar, JoinSuccess)
        assertEquals(3, eventCollector.events.size)
        assertTrue(eventCollector.events[0] is com.example.feature_join_project.JoinProjectEvent.ClearFocus)
        assertTrue(eventCollector.events[1] is com.example.feature_join_project.JoinProjectEvent.ShowSnackbar)
        assertTrue(eventCollector.events[2] is com.example.feature_join_project.JoinProjectEvent.JoinSuccess)
        
        // JoinSuccess 이벤트의 projectId가 올바른지 확인
        val successEvent = eventCollector.events[2] as com.example.feature_join_project.JoinProjectEvent.JoinSuccess
        assertEquals(testProject.id, successEvent.projectId)
    }

    /**
     * 유효하지 않은 코드로 프로젝트 참여 실패 테스트
     */
    @Test
    fun `유효하지 않은 초대 코드로 참여 시 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        val invalidCode = "invalid-code"
        viewModel.onCodeOrLinkChange(invalidCode)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<com.example.feature_join_project.JoinProjectEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 유효하지 않은 코드로 참여 시도
        viewModel.joinProject()
        
        // Then: 오류 상태 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isLoading)
        assertNotNull(uiState.error)
        assertTrue(uiState.error!!.contains("유효하지 않은 초대 코드 또는 링크입니다"))
        
        // ClearFocus 이벤트만 발생해야 함 (성공 이벤트는 발생하지 않음)
        assertEquals(1, eventCollector.events.size)
        assertTrue(eventCollector.events[0] is com.example.feature_join_project.JoinProjectEvent.ClearFocus)
    }

    /**
     * 리포지토리 오류 테스트
     */
    @Test
    fun `리포지토리 오류 발생 시 오류 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러를 시뮬레이션하도록 설정
        fakeProjectRepository.setShouldSimulateError(true)
        viewModel.onCodeOrLinkChange(testInviteCode)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<com.example.feature_join_project.JoinProjectEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 참여 시도
        viewModel.joinProject()
        
        // Then: 오류 상태 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isLoading)
        assertNotNull(uiState.error)
        assertTrue(uiState.error!!.contains("유효하지 않은 초대 코드 또는 링크입니다"))
        
        // ClearFocus 이벤트만 발생해야 함 (성공 이벤트는 발생하지 않음)
        assertEquals(1, eventCollector.events.size)
        assertTrue(eventCollector.events[0] is com.example.feature_join_project.JoinProjectEvent.ClearFocus)
    }

    /**
     * 로딩 중 상태에서 추가 참여 요청 방지 테스트
     */
    @Test
    fun `로딩 중에 추가 참여 요청을 방지해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 로딩 중 상태를 직접 설정
        viewModel.onCodeOrLinkChange(testInviteCode)
        
        // 로딩 상태를 직접 설정
        val privateUiState = viewModel.javaClass.getDeclaredField("_uiState")
        privateUiState.isAccessible = true
        val mutableStateFlow = privateUiState.get(viewModel) as MutableStateFlow<com.example.feature_join_project.JoinProjectUiState>
        mutableStateFlow.value = mutableStateFlow.value.copy(isLoading = true)
        
        // 설정된 로딩 상태 확인
        val loadingState = viewModel.uiState.getValue()
        assertTrue(loadingState.isLoading)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<com.example.feature_join_project.JoinProjectEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 로딩 중에 추가 참여 요청
        viewModel.joinProject()
        
        // Then: 아무 동작도 하지 않음 (이벤트 없음)
        assertEquals(0, eventCollector.events.size)
    }
} 