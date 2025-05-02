package com.example.feature_main.viewmodel

import com.example.data.repository.FakeAuthRepository
import com.example.data.repository.FakeUserRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import com.example.domain.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import android.net.Uri
import org.mockito.Mockito.mock

/**
 * ProfileViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 ProfileViewModel의 기능을 검증합니다.
 * FakeUserRepository와 FakeAuthRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class ProfileViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: ProfileViewModel

    // Fake Repositories
    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository

    // 테스트 데이터
    private val testUser = User(
        userId = "test_user_id",
        email = "test@example.com",
        name = "Test User",
        profileImageUrl = "https://example.com/profile.jpg",
        statusMessage = "테스트 상태 메시지"
    )

    // Mock Uri
    private lateinit var mockUri: Uri

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // Fake Repository 초기화
        fakeUserRepository = FakeUserRepository()
        fakeAuthRepository = FakeAuthRepository()
        
        // Mock Uri 초기화
        mockUri = mock(Uri::class.java)
        
        // 테스트 사용자 데이터 설정
        fakeUserRepository.addUser(testUser)
        fakeUserRepository.setCurrentUserId(testUser.userId)
    }

    /**
     * 프로필 로딩 성공 테스트
     */
    @Test
    fun `초기화 시 사용자 프로필을 성공적으로 로드해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 설정된 테스트 환경 (setup에서 설정됨)

        // When: ViewModel 초기화 (init에서 loadUserProfile 호출)
        viewModel = ProfileViewModel(fakeUserRepository, fakeAuthRepository)
        
        // Then: 로드된 프로필 정보 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isLoading)
        assertNotNull(uiState.userProfile)
        assertEquals(testUser.userId, uiState.userProfile?.userId)
        assertEquals(testUser.name, uiState.userProfile?.name)
        assertEquals(testUser.email, uiState.userProfile?.email)
        assertEquals(testUser.profileImageUrl, uiState.userProfile?.profileImageUrl)
        // statusMessage 확인 (테스트 데이터에 설정된 값과 일치해야 함)
        assertEquals(testUser.statusMessage, uiState.userProfile?.statusMessage)
    }

    /**
     * 프로필 로딩 오류 테스트
     */
    @Test
    fun `프로필 로드 중 오류 발생 시 에러 상태로 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러를 시뮬레이션하도록 설정
        fakeUserRepository.setShouldSimulateError(true)
        
        // When: ViewModel 초기화 (init에서 loadUserProfile 호출)
        viewModel = ProfileViewModel(fakeUserRepository, fakeAuthRepository)
        
        // Then: 에러 상태 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isLoading)
        assertNull(uiState.userProfile)
        assertNotNull(uiState.errorMessage)
        assertEquals("프로필을 불러오는데 실패했습니다.", uiState.errorMessage)
    }

    /**
     * 설정 화면 이동 테스트
     */
    @Test
    fun `설정 버튼 클릭 시 설정 화면으로 이동 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = ProfileViewModel(fakeUserRepository, fakeAuthRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<ProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 설정 버튼 클릭
        viewModel.onSettingsClick()
        
        // Then: 설정 화면 이동 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is ProfileEvent.NavigateToSettings)
    }

    /**
     * 친구 화면 이동 테스트
     */
    @Test
    fun `친구 버튼 클릭 시 친구 화면으로 이동 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = ProfileViewModel(fakeUserRepository, fakeAuthRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<ProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 친구 버튼 클릭
        viewModel.onFriendsClick()
        
        // Then: 친구 화면 이동 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is ProfileEvent.NavigateToFriends)
    }

    /**
     * 상태 화면 이동 테스트
     */
    @Test
    fun `상태 버튼 클릭 시 상태 화면으로 이동 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = ProfileViewModel(fakeUserRepository, fakeAuthRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<ProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 상태 버튼 클릭
        viewModel.onStatusClick()
        
        // Then: 상태 화면 이동 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is ProfileEvent.NavigateToStatus)
    }

    /**
     * 프로필 이미지 변경 요청 테스트
     */
    @Test
    fun `프로필 이미지 변경 버튼 클릭 시 이미지 선택기 요청 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = ProfileViewModel(fakeUserRepository, fakeAuthRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<ProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 프로필 이미지 변경 버튼 클릭
        viewModel.onEditProfileImageClick()
        
        // Then: 이미지 선택기 요청 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is ProfileEvent.PickProfileImage)
    }

    /**
     * 상태 메시지 변경 요청 테스트
     */
    @Test
    fun `상태 메시지 변경 버튼 클릭 시 상태 메시지 변경 다이얼로그 요청 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = ProfileViewModel(fakeUserRepository, fakeAuthRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<ProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 상태 메시지 변경 버튼 클릭
        viewModel.onEditStatusClick()
        
        // Then: 상태 메시지 변경 다이얼로그 요청 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is ProfileEvent.ShowEditStatusDialog)
    }

    /**
     * 로그아웃 성공 테스트
     */
    @Test
    fun `로그아웃 버튼 클릭 시 로그아웃 완료 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = ProfileViewModel(fakeUserRepository, fakeAuthRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<ProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 로그아웃 버튼 클릭
        viewModel.onLogoutClick()
        
        // Then: 로그아웃 완료 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is ProfileEvent.LogoutCompleted)
    }

    /**
     * 로그아웃 실패 테스트
     */
    @Test
    fun `로그아웃 중 오류 발생 시 스낵바 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러를 시뮬레이션하도록 설정
        fakeAuthRepository.setShouldSimulateError(true)
        viewModel = ProfileViewModel(fakeUserRepository, fakeAuthRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<ProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 로그아웃 버튼 클릭
        viewModel.onLogoutClick()
        
        // Then: 스낵바 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is ProfileEvent.ShowSnackbar)
        assertEquals("로그아웃 실패", (event as ProfileEvent.ShowSnackbar).message)
    }

    /**
     * 상태 메시지 변경 테스트
     */
    @Test
    fun `상태 메시지 변경 시 업데이트된 상태가 반영되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = ProfileViewModel(fakeUserRepository, fakeAuthRepository)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertNotNull(initialState.userProfile)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<ProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 상태 메시지 변경
        val newStatusMessage = "새로운 상태 메시지"
        viewModel.changeStatusMessage(newStatusMessage)
        
        // Then: 업데이트된 상태 및 스낵바 이벤트 확인
        val updatedState = viewModel.uiState.getValue()
        assertEquals(newStatusMessage, updatedState.userProfile?.statusMessage)
        
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is ProfileEvent.ShowSnackbar)
    }

    /**
     * 프로필 이미지 변경 테스트
     */
    @Test
    fun `프로필 이미지 변경 시 업데이트된 이미지가 반영되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        viewModel = ProfileViewModel(fakeUserRepository, fakeAuthRepository)
        
        // 초기 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertNotNull(initialState.userProfile)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<ProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 프로필 이미지 변경
        viewModel.changeProfileImage(mockUri)
        
        // Then: 업데이트된 이미지 URL 및 스낵바 이벤트 확인
        val updatedState = viewModel.uiState.getValue()
        assertNotNull(updatedState.userProfile?.profileImageUrl)
        
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is ProfileEvent.ShowSnackbar)
    }

    /**
     * 오류 메시지 표시 후 초기화 테스트
     */
    @Test
    fun `에러 메시지가 표시된 후 초기화되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러 상태의 ViewModel
        fakeUserRepository.setShouldSimulateError(true)
        viewModel = ProfileViewModel(fakeUserRepository, fakeAuthRepository)
        
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