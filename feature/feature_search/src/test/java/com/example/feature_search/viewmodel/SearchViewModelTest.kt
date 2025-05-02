package com.example.feature_search.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.data.repository.FakeSearchRepository
import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import com.example.domain.model.MessageResult
import com.example.domain.model.SearchScope
import com.example.domain.model.UserResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import java.time.LocalDateTime

/**
 * SearchViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 SearchViewModel의 기능을 검증합니다.
 * FakeSearchRepository를 사용하여 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class SearchViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: SearchViewModel

    // Fake Repository
    private lateinit var fakeSearchRepository: FakeSearchRepository
    
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
        fakeSearchRepository = FakeSearchRepository()
        
        // 테스트 데이터 설정
        fakeSearchRepository.setupTestData()
        
        // ViewModel 초기화
        viewModel = SearchViewModel(savedStateHandle, fakeSearchRepository)
    }

    /**
     * 검색어 변경 시 UI 상태 업데이트 테스트
     */
    @Test
    fun `검색어 변경 시 UI 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 UI 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertTrue(initialState.query.isEmpty())
        
        // When: 검색어 변경
        val searchQuery = "테스트"
        viewModel.onQueryChange(searchQuery)
        
        // Then: UI 상태 업데이트 확인
        val updatedState = viewModel.uiState.getValue()
        assertEquals(searchQuery, updatedState.query)
    }

    /**
     * 검색 범위 변경 시 UI 상태 업데이트 테스트
     */
    @Test
    fun `검색 범위 변경 시 UI 상태가 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 UI 상태 확인
        val initialState = viewModel.uiState.getValue()
        assertEquals(SearchScope.ALL, initialState.selectedScope)
        
        // When: 검색 범위 변경
        viewModel.onScopeChange(SearchScope.MESSAGES)
        
        // Then: UI 상태 업데이트 확인
        val updatedState = viewModel.uiState.getValue()
        assertEquals(SearchScope.MESSAGES, updatedState.selectedScope)
    }

    /**
     * 성공적인 검색 테스트 (모든 범위)
     */
    @Test
    fun `유효한 검색어와 ALL 범위로 검색 시 모든 결과가 반환되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 검색어 설정
        viewModel.onQueryChange("김철수")
        
        // 디바운싱 효과 건너뛰기
        coroutinesTestRule.testCoroutineDispatcher.advanceTimeBy(600)
        
        // Then: 검색 결과 확인
        val state = viewModel.uiState.getValue()
        assertFalse(state.isLoading)
        assertTrue(state.searchPerformed)
        assertNull(state.error)
        
        // "김철수"는 메시지와 사용자 모두에서 발견되어야 함
        assertEquals(2, state.searchResults.size)
        
        // 결과에는 메시지와 사용자가 모두 포함되어야 함
        assertTrue(state.searchResults.any { it is MessageResult })
        assertTrue(state.searchResults.any { it is UserResult })
    }

    /**
     * 메시지 범위 검색 테스트
     */
    @Test
    fun `MESSAGES 범위로 검색 시 메시지 결과만 반환되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 검색 범위와 검색어 설정
        viewModel.onScopeChange(SearchScope.MESSAGES)
        viewModel.onQueryChange("테스트")
        
        // 디바운싱 효과 건너뛰기
        coroutinesTestRule.testCoroutineDispatcher.advanceTimeBy(600)
        
        // Then: 검색 결과 확인
        val state = viewModel.uiState.getValue()
        assertFalse(state.isLoading)
        assertTrue(state.searchPerformed)
        
        // 결과에는 메시지만 포함되어야 함
        assertTrue(state.searchResults.all { it is MessageResult })
        assertFalse(state.searchResults.isEmpty())
    }

    /**
     * 사용자 범위 검색 테스트
     */
    @Test
    fun `USERS 범위로 검색 시 사용자 결과만 반환되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 검색 범위와 검색어 설정
        viewModel.onScopeChange(SearchScope.USERS)
        viewModel.onQueryChange("홍길동")
        
        // 디바운싱 효과 건너뛰기
        coroutinesTestRule.testCoroutineDispatcher.advanceTimeBy(600)
        
        // Then: 검색 결과 확인
        val state = viewModel.uiState.getValue()
        assertFalse(state.isLoading)
        assertTrue(state.searchPerformed)
        
        // 결과에는 사용자만 포함되어야 함
        assertTrue(state.searchResults.all { it is UserResult })
        assertFalse(state.searchResults.isEmpty())
    }

    /**
     * 빈 검색어 테스트
     */
    @Test
    fun `빈 검색어로 검색 시 결과가 비어 있어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 빈 검색어 설정
        viewModel.onQueryChange("")
        
        // 디바운싱 효과 건너뛰기
        coroutinesTestRule.testCoroutineDispatcher.advanceTimeBy(600)
        
        // Then: 검색 결과 확인
        val state = viewModel.uiState.getValue()
        assertFalse(state.isLoading)
        assertFalse(state.searchPerformed) // 검색 수행되지 않음
        assertTrue(state.searchResults.isEmpty())
    }

    /**
     * 검색 실패 테스트
     */
    @Test
    fun `검색 중 오류 발생 시 에러 상태로 업데이트되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 에러 시뮬레이션 설정
        fakeSearchRepository.setShouldSimulateError(true)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<SearchEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 검색 실행
        viewModel.onQueryChange("검색어")
        
        // 디바운싱 효과 건너뛰기
        coroutinesTestRule.testCoroutineDispatcher.advanceTimeBy(600)
        
        // Then: 에러 상태 확인
        val state = viewModel.uiState.getValue()
        assertFalse(state.isLoading)
        assertTrue(state.searchPerformed)
        assertNotNull(state.error)
        assertTrue(state.searchResults.isEmpty())
        
        // 스낵바 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is SearchEvent.ShowSnackbar)
        assertTrue((event as SearchEvent.ShowSnackbar).message.contains("검색 실패"))
    }

    /**
     * 메시지 결과 클릭 테스트
     */
    @Test
    fun `메시지 결과 클릭 시 메시지 네비게이션 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 검색 수행 후 결과 가져오기
        viewModel.onQueryChange("프로젝트")
        
        // 디바운싱 효과 건너뛰기
        coroutinesTestRule.testCoroutineDispatcher.advanceTimeBy(600)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<SearchEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 메시지 결과 클릭
        val messageResult = viewModel.uiState.getValue().searchResults.first { it is MessageResult } as MessageResult
        viewModel.onResultClick(messageResult)
        
        // Then: 메시지 네비게이션 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is SearchEvent.NavigateToMessage)
        
        val navigateEvent = event as SearchEvent.NavigateToMessage
        assertEquals(messageResult.channelId, navigateEvent.channelId)
        assertEquals(messageResult.messageId, navigateEvent.messageId)
    }

    /**
     * 사용자 결과 클릭 테스트
     */
    @Test
    fun `사용자 결과 클릭 시 사용자 프로필 네비게이션 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 검색 수행 후 결과 가져오기
        viewModel.onScopeChange(SearchScope.USERS)
        viewModel.onQueryChange("홍길동")
        
        // 디바운싱 효과 건너뛰기
        coroutinesTestRule.testCoroutineDispatcher.advanceTimeBy(600)
        
        // 이벤트 수집기 설정
        val eventCollector = EventCollector<SearchEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        
        // When: 사용자 결과 클릭
        val userResult = viewModel.uiState.getValue().searchResults.first() as UserResult
        viewModel.onResultClick(userResult)
        
        // Then: 사용자 프로필 네비게이션 이벤트 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is SearchEvent.NavigateToUserProfile)
        
        val navigateEvent = event as SearchEvent.NavigateToUserProfile
        assertEquals(userResult.userId, navigateEvent.userId)
    }
} 