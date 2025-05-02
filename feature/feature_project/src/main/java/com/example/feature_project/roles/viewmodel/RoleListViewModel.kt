package com.example.feature_project.roles.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- 데이터 모델 ---
data class RoleItem(
    val id: String, // 역할 ID
    val name: String // 역할 이름
    // 필요 시 추가 속성 (색상, 권한 수 등)
)

// --- UI 상태 ---
data class RoleListUiState(
    val projectId: String = "",
    val roles: List<RoleItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// --- 이벤트 ---
sealed class RoleListEvent {
    object NavigateToAddRole : RoleListEvent() // 역할 추가 화면으로 이동
    data class NavigateToEditRole(val roleId: String) : RoleListEvent() // 역할 수정 화면으로 이동
    data class ShowSnackbar(val message: String) : RoleListEvent()
}


@HiltViewModel
class RoleListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    // TODO: private val repository: ProjectRoleRepository
) : ViewModel() {

    val projectId: String = savedStateHandle["projectId"] ?: error("projectId가 전달되지 않았습니다.")

    private val _uiState = MutableStateFlow(RoleListUiState(projectId = projectId, isLoading = true))
    val uiState: StateFlow<RoleListUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<RoleListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadRoles()
    }

    /**
     * 프로젝트 역할 목록 로드
     */
    private fun loadRoles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading roles for project $projectId")
            // --- TODO: 실제 역할 목록 로드 (repository.getRoles) ---
            delay(700) // 임시 딜레이
            val success = true
            // val result = repository.getRoles(projectId)
            // ---------------------------------------------------
            if (success /*result.isSuccess*/) {
                // 임시 데이터
                val roles = listOf(
                    RoleItem("r1", "관리자 (Owner)"),
                    RoleItem("r2", "운영진 (Moderator)"),
                    RoleItem("r3", "정회원 (Member)"),
                    RoleItem("r4", "특별 회원 (VIP)")
                )
                // val roles = result.getOrThrow()
                _uiState.update { it.copy(isLoading = false, roles = roles) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "역할 목록을 불러오지 못했습니다." // result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    /**
     * 역할 추가 버튼 클릭 시 호출
     */
    fun onAddRoleClick() {
        viewModelScope.launch {
            _eventFlow.emit(RoleListEvent.NavigateToAddRole)
        }
    }

    /**
     * 역할 아이템 클릭 시 호출
     */
    fun onRoleClick(roleId: String) {
        viewModelScope.launch {
            _eventFlow.emit(RoleListEvent.NavigateToEditRole(roleId))
        }
    }
}