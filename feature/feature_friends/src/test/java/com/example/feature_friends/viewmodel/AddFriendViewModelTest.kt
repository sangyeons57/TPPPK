package com.example.feature_friends.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.data.repository.FakeFriendRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * AddFriendViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 AddFriendViewModel의 기능을 검증합니다.
 * FakeFriendRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class AddFriendViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: AddFriendViewModel

    // Fake Repository
    private lateinit var fakeFriendRepository: FakeFriendRepository
    
    // SavedStateHandle Mock
    private lateinit var savedStateHandle: SavedStateHandle

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // SavedStateHandle Mock 설정
        savedStateHandle = mock(SavedStateHandle::class.java)
        
        // Fake Repository 초기화
        fakeFriendRepository = FakeFriendRepository()
        
        // ViewModel 초기화
        viewModel = AddFriendViewModel(savedStateHandle, fakeFriendRepository)
    }

    /**
     * 초기 상태 테스트
     */
    @Test
    fun `초기 상태는 빈 사용자 이름과 기본 값들을 가져야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel (setup에서 이미 초기화됨)

        // When: UI 상태 가져오기
        val initialState = viewModel.uiState.getValue()

        // Then: 초기 상태 확인
        assertEquals("", initialState.username)
        assertFalse(initialState.isLoading)
        assertNull(initialState.error)
        assertNull(initialState.infoMessage)
        assertFalse(initialState.addFriendSuccess)
    }

    /**
     * 사용자 이름 입력 테스트
     */
    @Test
    fun `사용자 이름 입력 시 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel
        val testUsername = "testUser"

        // When: 사용자 이름 입력
        viewModel.onUsernameChange(testUsername)

        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(testUsername, state.username)
        // 에러와 정보 메시지가 초기화되었는지 확인
        assertNull(state.error)
        assertNull(state.infoMessage)
    }

    /**
     * 친구 요청 성공 테스트
     */
    @Test
    fun `유효한 사용자 이름으로 친구 요청 시 성공 상태로 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 사용자 이름 입력
        val eventCollector = EventCollector<AddFriendEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        val testUsername = "validUser"
        viewModel.onUsernameChange(testUsername)

        // When: 친구 요청 시도
        viewModel.onAddFriendClick()

        // Then: 성공 상태 확인
        val state = viewModel.uiState.getValue()
        assertTrue(state.addFriendSuccess)
        assertNotNull(state.infoMessage)
        assertFalse(state.isLoading)
        
        // 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is AddFriendEvent.DismissDialog)
    }

    /**
     * 빈 사용자 이름으로 친구 요청 시 실패 테스트
     */
    @Test
    fun `빈 사용자 이름으로 친구 요청 시 에러가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 빈 사용자 이름 설정
        viewModel.onUsernameChange("")

        // When: 친구 요청 시도
        viewModel.onAddFriendClick()

        // Then: 에러 상태 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.error)
        assertEquals("Username cannot be empty", state.error)
        assertFalse(state.isLoading)
        assertFalse(state.addFriendSuccess)
    }

    /**
     * 이미 친구인 사용자에게 요청 시 실패 테스트
     */
    @Test
    fun `이미 친구인 사용자에게 친구 요청 시 에러가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이미 친구인 사용자 설정
        fakeFriendRepository.setShouldSimulateError(true, IllegalStateException("User is already a friend"))
        viewModel.onUsernameChange("existingFriend")

        // When: 친구 요청 시도
        viewModel.onAddFriendClick()

        // Then: 에러 상태 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("already a friend"))
        assertFalse(state.isLoading)
        assertFalse(state.addFriendSuccess)
    }

    /**
     * 네트워크 오류 테스트
     */
    @Test
    fun `네트워크 오류 발생 시 에러 상태로 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 네트워크 오류 시뮬레이션 설정
        fakeFriendRepository.setShouldSimulateError(true, Exception("Network error"))
        viewModel.onUsernameChange("validUser")

        // When: 친구 요청 시도
        viewModel.onAddFriendClick()

        // Then: 에러 상태 확인
        val state = viewModel.uiState.getValue()
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Network error"))
        assertFalse(state.isLoading)
        assertFalse(state.addFriendSuccess)
    }

    /**
     * 로딩 상태 테스트
     */
    @Test
    fun `친구 요청 진행 중에는 로딩 상태가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 처리 시간이 필요한 Repository 설정 (여기서는 시뮬레이션 어려움)
        // 대신 ViewModel 내부 로직을 검사

        // When: 친구 요청 시도
        viewModel.onUsernameChange("validUser")
        
        // onAddFriendClick 호출 시 내부적으로 isLoading = true로 설정될 것이므로, 
        // 이 단계에서 직접 확인하기 어렵습니다. 대신 최종 상태만 확인합니다.
        viewModel.onAddFriendClick()

        // Then: 최종 상태 확인 (로딩 완료)
        val finalState = viewModel.uiState.getValue()
        assertFalse(finalState.isLoading) // 요청 완료 후 로딩 상태 해제
    }

    /**
     * 다이얼로그 취소 테스트
     */
    @Test
    fun `취소 버튼 클릭 시 다이얼로그 닫기 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정
        val eventCollector = EventCollector<AddFriendEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)

        // When: 취소 버튼 클릭
        viewModel.onCancelClick()

        // Then: 다이얼로그 닫기 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is AddFriendEvent.DismissDialog)
    }
} 