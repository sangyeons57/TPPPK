package com.example.feature_search.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ui.search.MessageResult
import com.example.domain.model.ui.search.SearchResultItem
import com.example.domain.model.ui.search.SearchScope
import com.example.domain.model.ui.search.UserResult
import com.example.domain.provider.search.SearchUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 상태 ---
data class SearchUiState(
    val query: String = "",
    val selectedScope: SearchScope = SearchScope.ALL,
    val searchResults: List<SearchResultItem> = emptyList(), // ★ Domain 모델 직접 사용
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchPerformed: Boolean = false // 검색을 한 번이라도 수행했는지 여부
)

// --- 이벤트 ---
sealed class SearchEvent {
    data class NavigateToMessageDetail(val channelId: String, val messageId: String) : SearchEvent()
    data class NavigateToUserProfile(val userId: String) : SearchEvent()
    data class ShowSnackbar(val message: String) : SearchEvent() // ShowError에서 ShowSnackbar로 이름 변경
    data class NavigateToMessage(val channelId: String, val messageId: String) : SearchEvent() // 기존 코드와의 호환성 유지
}

// --- ViewModel ---
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val searchUseCaseProvider: SearchUseCaseProvider
) : ViewModel() {

    // Create UseCase groups via provider
    private val searchUseCases = searchUseCaseProvider.create()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SearchEvent>() // _events에서 _eventFlow로 변경
    val eventFlow = _eventFlow.asSharedFlow() // events에서 eventFlow로 변경

    // 검색 디바운스를 위한 Job
    private var searchJob: Job? = null

    /** 검색어 변경 시 호출 */
    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, error = null) }
        // 디바운싱: 입력이 멈춘 후 일정 시간(예: 500ms) 뒤에 검색 실행
        searchJob?.cancel() // 이전 검색 작업 취소
        if (newQuery.isNotBlank()) { // 검색어가 있을 때만 검색
            searchJob = viewModelScope.launch {
                delay(500L) // 500ms 대기
                performSearch()
            }
        } else {
            // 검색어가 비면 결과 초기화
            _uiState.update { it.copy(searchResults = emptyList(), searchPerformed = false) }
        }
    }
    
    /** 검색 범위 변경 시 호출 */
    fun onScopeChange(newScope: SearchScope) {
        if (newScope != _uiState.value.selectedScope) {
            _uiState.update { it.copy(selectedScope = newScope, error = null) }
            // 범위 변경 시 즉시 검색 실행 (만약 이미 검색어가 있는 경우)
            if (_uiState.value.query.isNotBlank() && _uiState.value.searchPerformed) {
                performSearch()
            }
        }
    }

    /** 검색 실행 (직접 호출 또는 디바운싱 후 호출) */
    fun performSearch() {
        val query = _uiState.value.query.trim()
        val scope = _uiState.value.selectedScope

        if (query.isBlank()) {
            // 검색어가 없으면 실행 안 함 (또는 에러 표시)
            // viewModelScope.launch { _eventFlow.emit(SearchEvent.ShowSnackbar("검색어를 입력해주세요.")) }
            return
        }
        if (_uiState.value.isLoading) return // 이미 로딩 중이면 중복 실행 방지

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, searchPerformed = true) }
            println("ViewModel: Performing search for '$query' in scope '$scope'")

            val result = searchUseCases.searchUseCase(query, scope) // UseCase 호출

            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, searchResults = result.getOrThrow()) }
            } else {
                val errorMsg = "검색 실패: ${result.exceptionOrNull()?.message}"
                _uiState.update { it.copy(isLoading = false, error = errorMsg, searchResults = emptyList()) }
                _eventFlow.emit(SearchEvent.ShowSnackbar(errorMsg))
            }
        }
    }

    /** 검색 결과 항목 클릭 시 */
    fun onResultItemClick(item: SearchResultItem) {
        viewModelScope.launch {
            when (item) {
                is MessageResult -> {
                    // 메시지 검색 결과 클릭 시 해당 메시지가 있는 채팅방으로 이동
                    _eventFlow.emit(SearchEvent.NavigateToMessage(item.channelId, item.messageId))
                }
                is UserResult -> {
                    // 사용자 검색 결과 클릭 시 해당 사용자 프로필로 이동
                    _eventFlow.emit(SearchEvent.NavigateToUserProfile(item.userId))
                }
                else -> {
                    // 다른 타입의 검색 결과 처리 (미구현)
                    _eventFlow.emit(SearchEvent.ShowSnackbar("지원되지 않는 검색 결과 유형입니다."))
                }
            }
        }
    }

    // --- 내부 정의들 (SearchScope, SearchResultItem, SearchRepository) 삭제됨 ---
}