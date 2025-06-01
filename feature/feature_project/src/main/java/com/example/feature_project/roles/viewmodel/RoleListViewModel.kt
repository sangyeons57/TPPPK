package com.example.feature_project.roles.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.usecase.project.role.DeleteProjectRoleUseCase // Added
import com.example.domain.usecase.project.role.GetProjectRolesUseCase // Added
import dagger.hilt.android.lifecycle.HiltViewModel
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
    data class ShowDeleteRoleConfirmDialog(val roleItem: RoleItem) : RoleListEvent() // Added
    data class ShowSnackbar(val message: String) : RoleListEvent()
}


@HiltViewModel
class RoleListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProjectRolesUseCase: GetProjectRolesUseCase, // Added
    private val deleteProjectRoleUseCase: DeleteProjectRoleUseCase // Added
) : ViewModel() {

    private val projectId: String = savedStateHandle.getRequiredString(AppRoutes.Project.ARG_PROJECT_ID)

    private val _uiState = MutableStateFlow(RoleListUiState(projectId = projectId, isLoading = true))
    val uiState: StateFlow<RoleListUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<RoleListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        _uiState.update { it.copy(projectId = projectId) } // Ensure projectId is set
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getProjectRolesUseCase(projectId)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "역할 로드 실패: ${e.localizedMessage}")
                    }
                }
                .collect { domainRoles ->
                    val roleItems = domainRoles.map { domainRole ->
                        RoleItem(id = domainRole.id ?: "", name = domainRole.name) // Handle nullable id
                    }
                    _uiState.update {
                        it.copy(isLoading = false, roles = roleItems, error = null)
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

    fun requestDeleteRole(roleItem: RoleItem) {
        viewModelScope.launch {
            _eventFlow.emit(RoleListEvent.ShowDeleteRoleConfirmDialog(roleItem))
        }
    }

    fun confirmDeleteRole(roleId: String) {
        viewModelScope.launch {
            // Optional: _uiState.update { it.copy(isLoading = true) } // Indicate loading for delete
            val result = deleteProjectRoleUseCase(projectId, roleId)
            when (result){
                is CustomResult.Success -> {
                    _eventFlow.emit(RoleListEvent.ShowSnackbar("역할이 삭제되었습니다."))
                }
                is CustomResult.Failure -> {
                    _eventFlow.emit(RoleListEvent.ShowSnackbar("역할 삭제 실패: ${result.error}"))
                }
                else -> {
                    _eventFlow.emit(RoleListEvent.ShowSnackbar("알 수 없는 오류가 발생했습니다."))
                }
            }
            // Optional: _uiState.update { it.copy(isLoading = false) }
        }
    }
}