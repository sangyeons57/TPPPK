package com.example.feature_project_detail.viewmodel

import com.example.data.repository.FakeProjectRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.domain.model.Project
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * AddProjectViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 AddProjectViewModel의 기능을 검증합니다.
 * FakeProjectRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class AddProjectViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: AddProjectViewModel

    // Fake Repository
    private lateinit var fakeProjectRepository: FakeProjectRepository

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
        // Fake Repository 초기화
        fakeProjectRepository = FakeProjectRepository()
        
        // 테스트 프로젝트 및 초대 코드 설정
        fakeProjectRepository.addProject(testProject)
        fakeProjectRepository.setProjectCode(testInviteCode, testProject.id)
        
        // ViewModel 초기화
        viewModel = AddProjectViewModel(fakeProjectRepository)
    }

    /**
     * 초기 상태 테스트
     */
    @Test
    fun `초기 상태는 프로젝트 참여 모드와 빈 입력 값을 가져야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel (setup에서 이미 초기화됨)

        // When: UI 상태 가져오기
        val initialState = viewModel.uiState.getValue()

        // Then: 초기 상태 확인
        assertEquals(AddProjectMode.JOIN, initialState.selectedMode)
        assertEquals(CreateProjectMode.OPEN, initialState.createMode)
        assertEquals("", initialState.joinCode)
        assertEquals("", initialState.projectName)
        assertEquals("", initialState.projectDescription)
        assertFalse(initialState.isLoading)
        assertNull(initialState.errorMessage)
        assertFalse(initialState.projectAddedSuccessfully)
    }

    /**
     * 모드 변경 테스트
     */
    @Test
    fun `모드 변경 시 상태가 업데이트되고 입력 필드가 초기화되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 일부 입력 값이 있는 상태
        viewModel.onJoinCodeChange("some-code")
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertEquals(AddProjectMode.JOIN, initialState.selectedMode)
        assertEquals("some-code", initialState.joinCode)
        
        // When: 모드 변경
        viewModel.onModeSelect(AddProjectMode.CREATE)
        
        // Then: 모드 변경 및 입력 값 초기화 확인
        val updatedState = viewModel.uiState.getValue()
        assertEquals(AddProjectMode.CREATE, updatedState.selectedMode)
        assertEquals("", updatedState.joinCode)
        assertEquals("", updatedState.projectName)
        assertEquals("", updatedState.projectDescription)
    }

    /**
     * 생성 모드 변경 테스트
     */
    @Test
    fun `프로젝트 생성 모드 변경 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 생성 모드 상태
        viewModel.onModeSelect(AddProjectMode.CREATE)
        
        // 초기 생성 모드 확인
        val initialState = viewModel.uiState.getValue()
        assertEquals(CreateProjectMode.OPEN, initialState.createMode)
        
        // When: 생성 모드 변경
        viewModel.onCreateModeSelect(CreateProjectMode.CLOSE)
        
        // Then: 생성 모드 변경 확인
        val updatedState = viewModel.uiState.getValue()
        assertEquals(CreateProjectMode.CLOSE, updatedState.createMode)
    }

    /**
     * 프로젝트 코드 입력 테스트
     */
    @Test
    fun `코드 입력 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        
        // When: 코드 입력
        viewModel.onJoinCodeChange(testInviteCode)
        
        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(testInviteCode, state.joinCode)
    }

    /**
     * 프로젝트 이름 입력 테스트
     */
    @Test
    fun `프로젝트 이름 입력 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 생성 모드의 ViewModel
        viewModel.onModeSelect(AddProjectMode.CREATE)
        
        // When: 프로젝트 이름 입력
        viewModel.onProjectNameChange("새 프로젝트")
        
        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals("새 프로젝트", state.projectName)
    }

    /**
     * 프로젝트 설명 입력 테스트
     */
    @Test
    fun `프로젝트 설명 입력 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 생성 모드의 ViewModel
        viewModel.onModeSelect(AddProjectMode.CREATE)
        
        // When: 프로젝트 설명 입력
        viewModel.onProjectDescriptionChange("프로젝트 설명입니다")
        
        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals("프로젝트 설명입니다", state.projectDescription)
    }

    /**
     * 빈 코드로 프로젝트 참여 시도 테스트
     */
    @Test
    fun `빈 코드로 프로젝트 참여 시도 시 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 빈 코드 상태의 ViewModel
        
        // When: 참여 버튼 클릭
        viewModel.onJoinProjectClick()
        
        // Then: 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.errorMessage)
        assertEquals("참여 코드를 입력해주세요.", state.errorMessage)
        assertFalse(state.isLoading)
    }

    /**
     * 유효한 코드로 프로젝트 참여 성공 테스트
     */
    @Test
    fun `유효한 코드로 프로젝트 참여 시 성공 상태로 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 유효한 코드가 입력된 ViewModel
        viewModel.onJoinCodeChange(testInviteCode)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<AddProjectEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 참여 버튼 클릭
        viewModel.onJoinProjectClick()
        
        // Then: 성공 상태 확인
        val state = viewModel.uiState.getValue()
        assertFalse(state.isLoading)
        assertTrue(state.projectAddedSuccessfully)
        assertNull(state.errorMessage)
        
        // 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is AddProjectEvent.ShowSnackbar)
        assertEquals("프로젝트에 참여했습니다!", (event as AddProjectEvent.ShowSnackbar).message)
    }

    /**
     * 프로젝트 참여 실패 테스트
     */
    @Test
    fun `프로젝트 참여 실패 시 에러 상태로 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 코드가 입력된 ViewModel과 에러를 시뮬레이션하도록 설정
        viewModel.onJoinCodeChange("invalid-code")
        fakeProjectRepository.setShouldSimulateError(true)
        
        // When: 참여 버튼 클릭
        viewModel.onJoinProjectClick()
        
        // Then: 에러 상태 확인
        val state = viewModel.uiState.getValue()
        assertFalse(state.isLoading)
        assertFalse(state.projectAddedSuccessfully)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("참여 코드 확인") || state.errorMessage!!.contains("참여 실패"))
    }

    /**
     * 빈 이름으로 프로젝트 생성 시도 테스트
     */
    @Test
    fun `빈 이름으로 프로젝트 생성 시도 시 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 생성 모드의 ViewModel
        viewModel.onModeSelect(AddProjectMode.CREATE)
        
        // When: 빈 이름으로 생성 버튼 클릭
        viewModel.onCreateProjectClick()
        
        // Then: 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.errorMessage)
        assertEquals("프로젝트 이름을 입력해주세요.", state.errorMessage)
        assertFalse(state.isLoading)
    }

    /**
     * 유효한 이름으로 프로젝트 생성 성공 테스트
     */
    @Test
    fun `유효한 이름으로 프로젝트 생성 시 성공 상태로 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이름이 입력된 생성 모드 ViewModel
        viewModel.onModeSelect(AddProjectMode.CREATE)
        viewModel.onProjectNameChange("새 프로젝트")
        viewModel.onProjectDescriptionChange("설명입니다")
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<AddProjectEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 생성 버튼 클릭
        viewModel.onCreateProjectClick()
        
        // Then: 성공 상태 확인
        val state = viewModel.uiState.getValue()
        assertFalse(state.isLoading)
        assertTrue(state.projectAddedSuccessfully)
        assertNull(state.errorMessage)
        
        // 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is AddProjectEvent.ShowSnackbar)
        assertEquals("프로젝트를 생성했습니다!", (event as AddProjectEvent.ShowSnackbar).message)
    }

    /**
     * 프로젝트 생성 실패 테스트
     */
    @Test
    fun `프로젝트 생성 실패 시 에러 상태로 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이름이 입력된 생성 모드 ViewModel과 에러를 시뮬레이션하도록 설정
        viewModel.onModeSelect(AddProjectMode.CREATE)
        viewModel.onProjectNameChange("새 프로젝트")
        fakeProjectRepository.setShouldSimulateError(true)
        
        // When: 생성 버튼 클릭
        viewModel.onCreateProjectClick()
        
        // Then: 에러 상태 확인
        val state = viewModel.uiState.getValue()
        assertFalse(state.isLoading)
        assertFalse(state.projectAddedSuccessfully)
        assertNotNull(state.errorMessage)
        assertEquals("프로젝트 생성 실패", state.errorMessage)
    }

    /**
     * 에러 메시지 표시 후 초기화 테스트
     */
    @Test
    fun `에러 메시지가 표시된 후 초기화되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러 메시지가 있는 상태
        viewModel.onJoinProjectClick() // 빈 코드로 참여 시도하여 에러 발생
        
        // 에러 상태 확인
        val errorState = viewModel.uiState.getValue()
        assertNotNull(errorState.errorMessage)
        
        // When: 에러 메시지 표시 후 초기화
        viewModel.errorMessageShown()
        
        // Then: 에러 메시지 초기화 확인
        val updatedState = viewModel.uiState.getValue()
        assertNull(updatedState.errorMessage)
    }
} 