package com.example.feature_auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.data.repository.FakeAuthRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.domain.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * SplashViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 SplashViewModel의 기능을 검증합니다.
 * FakeAuthRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class SplashViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: SplashViewModel

    // Fake Repository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    
    // SavedStateHandle Mock
    private lateinit var savedStateHandle: SavedStateHandle

    // 테스트 데이터
    private val testUser = User(
        userId = "test_user_id",
        email = "test@example.com",
        name = "Test User",
        profileImageUrl = null,
        statusMessage = null
    )

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // SavedStateHandle Mock 설정
        savedStateHandle = mock(SavedStateHandle::class.java)
        
        // Fake Repository 초기화
        fakeAuthRepository = FakeAuthRepository()
    }

    /**
     * 로그인 상태일 때 메인 화면으로 이동 테스트
     */
    @Test
    fun `로그인된 상태일 때 메인 화면으로 이동해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 로그인 상태 설정
        // 로그인된 유저 추가
        fakeAuthRepository.addUser(testUser, "password123")
        fakeAuthRepository.setCurrentUserId(testUser.userId)

        // 이벤트 수집기 설정
        val eventCollector = EventCollector<SplashEvent>()
        
        // When: ViewModel 초기화 (이 때 자동으로 checkNextDestination 실행됨)
        viewModel = SplashViewModel(savedStateHandle, fakeAuthRepository)
        
        // EventCollector는 ViewModel 생성 후에 연결해야 함
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // Then: 메인 화면 이동 이벤트 발생 확인
        coroutinesTestRule.advanceTimeBy(1600) // 1.5초 지연 이후 이벤트 발생
        
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is SplashEvent.NavigateToMain)
    }

    /**
     * 로그아웃 상태일 때 로그인 화면으로 이동 테스트
     */
    @Test
    fun `로그아웃된 상태일 때 로그인 화면으로 이동해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 로그아웃 상태 설정 (현재 유저 ID를 null로 설정)
        // 기본 상태는 로그아웃 상태이므로 특별한 설정 필요 없음

        // 이벤트 수집기 설정
        val eventCollector = EventCollector<SplashEvent>()
        
        // When: ViewModel 초기화 (이 때 자동으로 checkNextDestination 실행됨)
        viewModel = SplashViewModel(savedStateHandle, fakeAuthRepository)
        
        // EventCollector는 ViewModel 생성 후에 연결해야 함
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // Then: 로그인 화면 이동 이벤트 발생 확인
        coroutinesTestRule.advanceTimeBy(1600) // 1.5초 지연 이후 이벤트 발생
        
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is SplashEvent.NavigateToLogin)
    }

    /**
     * 에러 발생 시 로그인 화면으로 이동 테스트
     */
    @Test
    fun `로그인 상태 확인 중 오류 발생 시 로그인 화면으로 이동해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 에러 상태 설정
        fakeAuthRepository.setShouldSimulateError(true)

        // 이벤트 수집기 설정
        val eventCollector = EventCollector<SplashEvent>()
        
        // When: ViewModel 초기화 (이 때 자동으로 checkNextDestination 실행됨)
        viewModel = SplashViewModel(savedStateHandle, fakeAuthRepository)
        
        // EventCollector는 ViewModel 생성 후에 연결해야 함
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // Then: 로그인 화면 이동 이벤트 발생 확인
        coroutinesTestRule.advanceTimeBy(1600) // 1.5초 지연 이후 이벤트 발생
        
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is SplashEvent.NavigateToLogin)
    }
} 