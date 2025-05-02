package com.example.feature_profile.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.data.repository.FakeUserRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import com.example.domain.model.User
import com.example.domain.model.UserStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * ChangeStatusViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 ChangeStatusViewModel의 기능을 검증합니다.
 * FakeUserRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class ChangeStatusViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: ChangeStatusViewModel

    // Fake Repository
    private lateinit var fakeUserRepository: FakeUserRepository
    
    // SavedStateHandle Mock
    private lateinit var savedStateHandle: SavedStateHandle

    // 테스트 데이터
    private val testUserId = "test-user-123"
    private val testUser = User(
        userId = testUserId,
        email = "test@example.com",
        name = "Test User",
        profileImageUrl = null,
        statusMessage = "테스트 중",
        status = UserStatus.ONLINE.name
    )

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // SavedStateHandle Mock 설정
        savedStateHandle = mock(SavedStateHandle::class.java)
        
        // Fake Repository 초기화
        fakeUserRepository = FakeUserRepository()
        
        // 테스트 사용자 설정
        fakeUserRepository.addUser(testUser)
        fakeUserRepository.setCurrentUserId(testUserId)
    }

    /**
     * 초기 상태 로딩 성공 테스트
     */
    @Test
    fun `초기화 시 현재 사용자 상태를 성공적으로 로드해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 설정된 테스트 환경 (setup에서 설정됨)

        // When: ViewModel 초기화 (loadCurrentStatus 호출)
        viewModel = ChangeStatusViewModel(savedStateHandle, fakeUserRepository)
        
        // Then: 로드된 상태 정보 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isLoading)
        assertEquals(UserStatus.ONLINE, uiState.currentStatus)
        assertEquals(UserStatus.ONLINE, uiState.selectedStatus) // 초기 선택 상태는 현재 상태와 동일
        assertNull(uiState.error)
    }

    /**
     * 초기 상태 로딩 실패 테스트
     */
    @Test
    fun `상태 로딩 중 오류 발생 시 에러 상태로 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러를 시뮬레이션하도록 설정
        fakeUserRepository.setShouldSimulateError(true)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<ChangeStatusEvent>()
        
        // When: ViewModel 초기화 (loadCurrentStatus 호출)
        viewModel = ChangeStatusViewModel(savedStateHandle, fakeUserRepository)
        
        // 이벤트 수집 시작
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // Then: 에러 상태 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isLoading)
        assertNull(uiState.currentStatus)
        assertNull(uiState.selectedStatus)
        assertNotNull(uiState.error)
        assertTrue(uiState.error!!.contains("현재 상태를 불러오지 못했습니다"))
        
        // 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is ChangeStatusEvent.ShowSnackbar)
        assertTrue((event as ChangeStatusEvent.ShowSnackbar).message.contains("현재 상태를 불러오지 못했습니다"))
    }

    /**
     * 상태 선택 테스트
     */
    @Test
    fun `상태를 선택하면 SelectedStatus가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = ChangeStatusViewModel(savedStateHandle, fakeUserRepository)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertEquals(UserStatus.ONLINE, initialState.selectedStatus)
        
        // When: 새 상태 선택
        viewModel.onStatusSelected(UserStatus.AWAY)
        
        // Then: selectedStatus 업데이트 확인
        val updatedState = viewModel.uiState.getValue()
        assertEquals(UserStatus.AWAY, updatedState.selectedStatus)
        // 다른 상태는 변경되지 않음
        assertEquals(UserStatus.ONLINE, updatedState.currentStatus)
        assertFalse(updatedState.isLoading)
        assertFalse(updatedState.isUpdating)
    }

    /**
     * 상태 업데이트 성공 테스트
     */
    @Test
    fun `상태 업데이트 성공 시 현재 상태가 업데이트되고 성공 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel 및 이벤트 수집기
        viewModel = ChangeStatusViewModel(savedStateHandle, fakeUserRepository)
        
        val eventCollector = EventCollector<ChangeStatusEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // 상태 변경
        viewModel.onStatusSelected(UserStatus.DO_NOT_DISTURB)
        
        // When: 상태 업데이트
        viewModel.updateStatus()
        
        // Then: UI 상태 업데이트 확인
        val updatedState = viewModel.uiState.getValue()
        assertEquals(UserStatus.DO_NOT_DISTURB, updatedState.currentStatus)
        assertEquals(UserStatus.DO_NOT_DISTURB, updatedState.selectedStatus)
        assertFalse(updatedState.isUpdating)
        assertTrue(updatedState.updateSuccess)
        
        // 이벤트 확인
        assertTrue(eventCollector.events.size >= 2)
        val snackbarEvent = eventCollector.events.find { it is ChangeStatusEvent.ShowSnackbar && 
            (it as ChangeStatusEvent.ShowSnackbar).message.contains("상태가") && 
            (it as ChangeStatusEvent.ShowSnackbar).message.contains("변경되었습니다") }
        assertNotNull(snackbarEvent)
        
        // 다이얼로그 닫기 이벤트 확인
        val dismissEvent = eventCollector.events.find { it is ChangeStatusEvent.DismissDialog }
        assertNotNull(dismissEvent)
    }

    /**
     * 상태 업데이트 실패 테스트
     */
    @Test
    fun `상태 업데이트 실패 시 에러 메시지가 표시되고 현재 상태는 변경되지 않아야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel 및 이벤트 수집기
        viewModel = ChangeStatusViewModel(savedStateHandle, fakeUserRepository)
        
        // 상태 변경
        viewModel.onStatusSelected(UserStatus.AWAY)
        
        // 에러 설정
        fakeUserRepository.setShouldSimulateError(true)
        
        val eventCollector = EventCollector<ChangeStatusEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 상태 업데이트
        viewModel.updateStatus()
        
        // Then: UI 상태 확인
        val updatedState = viewModel.uiState.getValue()
        assertEquals(UserStatus.ONLINE, updatedState.currentStatus) // 원래 상태 유지
        assertEquals(UserStatus.AWAY, updatedState.selectedStatus) // 선택된 상태는 유지
        assertFalse(updatedState.isUpdating)
        assertFalse(updatedState.updateSuccess)
        assertNotNull(updatedState.error)
        
        // 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val errorEvents = eventCollector.events.filter { it is ChangeStatusEvent.ShowSnackbar && 
            (it as ChangeStatusEvent.ShowSnackbar).message.contains("상태 변경 실패") }
        assertTrue(errorEvents.isNotEmpty())
    }

    /**
     * 동일한 상태로 업데이트 시도 테스트
     */
    @Test
    fun `현재 상태와 동일한 상태로 업데이트 시도 시 적절한 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel 및 이벤트 수집기
        viewModel = ChangeStatusViewModel(savedStateHandle, fakeUserRepository)
        
        val eventCollector = EventCollector<ChangeStatusEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 현재 상태와 동일한 상태로 업데이트 시도
        viewModel.onStatusSelected(UserStatus.ONLINE) // 현재 상태와 동일
        viewModel.updateStatus()
        
        // Then: 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val snackbarEvent = eventCollector.events.first()
        assertTrue(snackbarEvent is ChangeStatusEvent.ShowSnackbar)
        assertEquals("현재 상태와 동일합니다.", (snackbarEvent as ChangeStatusEvent.ShowSnackbar).message)
    }

    /**
     * 상태 선택 없이 업데이트 시도 테스트
     */
    @Test
    fun `상태 선택 없이 업데이트 시도 시 적절한 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 상태 선택이 null인 ViewModel
        viewModel = ChangeStatusViewModel(savedStateHandle, fakeUserRepository)
        
        // selectedStatus를 null로 설정 (private 필드이므로 직접 변경 불가, 대신 반영 방법을 찾아야 함)
        // 여기서는 초기화 시 에러가 있는 경우를 가정하여 테스트
        fakeUserRepository.setShouldSimulateError(true)
        viewModel = ChangeStatusViewModel(savedStateHandle, fakeUserRepository)
        
        // 에러 시뮬레이션 해제 (업데이트는 성공하게)
        fakeUserRepository.setShouldSimulateError(false)
        
        val eventCollector = EventCollector<ChangeStatusEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 상태 업데이트 시도
        viewModel.updateStatus()
        
        // Then: 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val snackbarEvent = eventCollector.events.first()
        assertTrue(snackbarEvent is ChangeStatusEvent.ShowSnackbar)
        assertEquals("상태를 선택해주세요.", (snackbarEvent as ChangeStatusEvent.ShowSnackbar).message)
    }
} 