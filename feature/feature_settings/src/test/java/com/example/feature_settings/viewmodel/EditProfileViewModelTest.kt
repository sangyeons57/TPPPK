package com.example.feature_settings.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
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
import org.mockito.Mockito.mock

/**
 * EditProfileViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 EditProfileViewModel의 기능을 검증합니다.
 * FakeUserRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class EditProfileViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: EditProfileViewModel
    
    // SavedStateHandle Mock
    private lateinit var savedStateHandle: SavedStateHandle
    
    // Fake Repository
    private lateinit var fakeUserRepository: FakeUserRepository
    
    // 테스트 데이터
    private val testUser = User(
        userId = "user-1",
        name = "홍길동",
        email = "hong@example.com",
        profileImageUrl = "https://example.com/profile.jpg",
        status = "온라인",
        statusMessage = "안녕하세요!",
        friendCount = 5
    )
    
    // Mock URI를 위한 클래스
    private class TestUri(private val path: String) : Uri() {
        override fun toString(): String = "content://test/$path"
    }

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
        
        // ViewModel 초기화
        viewModel = EditProfileViewModel(savedStateHandle, fakeUserRepository)
    }

    /**
     * 프로필 로딩 테스트
     */
    @Test
    fun `초기화 시 사용자 프로필이 로드되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel (setup에서 생성됨)
        
        // Then: 프로필 로드 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isLoading)
        assertNotNull(uiState.user)
        assertEquals(testUser.userId, uiState.user?.userId)
        assertEquals(testUser.name, uiState.user?.name)
        assertEquals(testUser.profileImageUrl, uiState.user?.profileImageUrl)
    }

    /**
     * 프로필 로딩 실패 테스트
     */
    @Test
    fun `프로필 로드 실패 시 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 오류 시뮬레이션 설정
        fakeUserRepository.clearUsers() // 사용자 데이터 제거
        fakeUserRepository.setShouldSimulateError(true)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<EditProfileEvent>()
        
        // When: ViewModel 초기화
        viewModel = EditProfileViewModel(savedStateHandle, fakeUserRepository)
        
        // 이벤트 수집 시작
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // Then: 에러 상태 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isLoading)
        assertNull(uiState.user)
        assertNotNull(uiState.error)
        
        // 스낵바 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is EditProfileEvent.ShowSnackbar)
        assertTrue((event as EditProfileEvent.ShowSnackbar).message.contains("프로필 정보를 불러오지 못했습니다"))
    }

    /**
     * 이미지 선택 버튼 클릭 테스트
     */
    @Test
    fun `이미지 선택 버튼 클릭 시 이미지 선택기 요청 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<EditProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 이미지 선택 버튼 클릭
        viewModel.onSelectImageClick()
        
        // Then: 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is EditProfileEvent.RequestImagePicker)
    }

    /**
     * 이미지 선택 완료 테스트
     */
    @Test
    fun `이미지 선택 완료 시 이미지 업로드가 시작되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<EditProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 이미지 선택
        val testUri = TestUri("test_image.jpg")
        viewModel.onImagePicked(testUri)
        
        // Then: 업로드 시작 확인
        var uiState = viewModel.uiState.getValue()
        assertTrue(uiState.isUploading)
        
        // 업로드 중 스낵바 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        assertTrue(eventCollector.events.any { 
            it is EditProfileEvent.ShowSnackbar && 
            it.message.contains("업로드 중") 
        })
        
        // 업로드 완료 확인 (비동기 완료 시뮬레이션)
        coroutinesTestRule.testCoroutineDispatcher.advanceTimeBy(1000)
        
        uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isUploading)
        assertNull(uiState.selectedImageUri)
        
        // 업로드 성공 스낵바 이벤트 확인
        assertTrue(eventCollector.events.any { 
            it is EditProfileEvent.ShowSnackbar && 
            it.message.contains("업데이트되었습니다") 
        })
    }

    /**
     * 이미지 업로드 실패 테스트
     */
    @Test
    fun `이미지 업로드 실패 시 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러 시뮬레이션 설정
        fakeUserRepository.setShouldSimulateError(true)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<EditProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 이미지 선택
        val testUri = TestUri("test_image.jpg")
        viewModel.onImagePicked(testUri)
        
        // 업로드 완료 대기
        coroutinesTestRule.testCoroutineDispatcher.advanceTimeBy(1000)
        
        // Then: 업로드 실패 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isUploading)
        assertNull(uiState.selectedImageUri)
        assertNotNull(uiState.error)
        
        // 업로드 실패 스낵바 이벤트 확인
        assertTrue(eventCollector.events.any { 
            it is EditProfileEvent.ShowSnackbar && 
            it.message.contains("업로드 실패") 
        })
    }

    /**
     * 이미지 제거 버튼 클릭 테스트
     */
    @Test
    fun `이미지 제거 버튼 클릭 시 제거 확인 요청 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel (프로필 이미지가 있는 상태)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<EditProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 이미지 제거 버튼 클릭
        viewModel.onRemoveImageClick()
        
        // Then: 제거 확인 요청 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is EditProfileEvent.RequestProfileImageRemoveConfirm)
    }

    /**
     * 이미지 제거 확인 테스트
     */
    @Test
    fun `이미지 제거 확인 시 프로필 이미지가 제거되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<EditProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 이미지 제거 확인
        viewModel.confirmRemoveProfileImage()
        
        // 업로드 중 스낵바 이벤트 확인
        assertTrue(eventCollector.events.any { 
            it is EditProfileEvent.ShowSnackbar && 
            it.message.contains("제거 중") 
        })
        
        // 비동기 완료 대기
        coroutinesTestRule.testCoroutineDispatcher.advanceTimeBy(1000)
        
        // Then: 이미지 제거 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isUploading)
        assertNull(uiState.user?.profileImageUrl)
        assertTrue(uiState.updateSuccess)
        
        // 제거 성공 스낵바 이벤트 확인
        assertTrue(eventCollector.events.any { 
            it is EditProfileEvent.ShowSnackbar && 
            it.message.contains("제거되었습니다") 
        })
    }

    /**
     * 이미지 제거 실패 테스트
     */
    @Test
    fun `이미지 제거 실패 시 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러 시뮬레이션 설정
        fakeUserRepository.setShouldSimulateError(true)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<EditProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 이미지 제거 확인
        viewModel.confirmRemoveProfileImage()
        
        // 비동기 완료 대기
        coroutinesTestRule.testCoroutineDispatcher.advanceTimeBy(1000)
        
        // Then: 제거 실패 확인
        val uiState = viewModel.uiState.getValue()
        assertFalse(uiState.isUploading)
        assertNotNull(uiState.error)
        assertFalse(uiState.updateSuccess)
        
        // 제거 실패 스낵바 이벤트 확인
        assertTrue(eventCollector.events.any { 
            it is EditProfileEvent.ShowSnackbar && 
            it.message.contains("제거 실패") 
        })
    }

    /**
     * 프로필 이미지가 없을 때 제거 버튼 클릭 테스트
     */
    @Test
    fun `프로필 이미지가 없을 때 제거 버튼 클릭하면 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이미지가 없는 사용자로 설정
        val userWithoutImage = testUser.copy(profileImageUrl = null)
        fakeUserRepository.clearUsers()
        fakeUserRepository.addUser(userWithoutImage)
        
        // ViewModel 초기화
        viewModel = EditProfileViewModel(savedStateHandle, fakeUserRepository)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<EditProfileEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 이미지 제거 버튼 클릭
        viewModel.onRemoveImageClick()
        
        // Then: 에러 메시지 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is EditProfileEvent.ShowSnackbar)
        assertTrue((event as EditProfileEvent.ShowSnackbar).message.contains("제거할 프로필 이미지가 없습니다"))
    }
} 