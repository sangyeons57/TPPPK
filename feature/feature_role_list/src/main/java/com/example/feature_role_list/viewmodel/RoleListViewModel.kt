package com.example.feature_role_list.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.AddRoleRoute
import com.example.core_navigation.core.EditRoleRoute
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.base.Role
import com.example.domain.model.data.project.RolePermission
import com.example.domain.vo.DocumentId
import com.example.domain.vo.Name
import com.example.domain.vo.UserId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_usecase.provider.project.ProjectMemberUseCaseProvider
import com.example.domain_usecase.provider.project.ProjectRoleUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- 데이터 모델 ---
data class RoleItem(
    val id: DocumentId, // 역할 ID
    val name: Name // 역할 이름
    // 필요 시 추가 속성 (색상, 권한 수 등)
)

// --- UI 상태 ---
data class RoleListUiState(
    val projectId: String = "",
    val roles: List<RoleItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val canManageRoles: Boolean = false
)

// --- 이벤트 ---
sealed class RoleListEvent {
    data class ShowDeleteRoleConfirmDialog(val roleItem: RoleItem) : RoleListEvent() // Added
    data class ShowSnackbar(val message: String) : RoleListEvent()
}


@HiltViewModel
class RoleListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRoleUseCaseProvider: ProjectRoleUseCaseProvider,
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider,
    private val navigationManger: NavigationManger,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val projectId: String = savedStateHandle.getRequiredString(RouteArgs.PROJECT_ID)

    // Create UseCase groups via provider
    private val projectRoleUseCases = projectRoleUseCaseProvider.createForProject(DocumentId.from(projectId))
    private val projectMemberUseCases =
        projectMemberUseCaseProvider.createForProject(DocumentId.from(projectId))

    private val _uiState = MutableStateFlow(RoleListUiState(projectId = projectId, isLoading = true))
    val uiState: StateFlow<RoleListUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<RoleListEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        _uiState.update { it.copy(projectId = projectId) } // Ensure projectId is set
        // Observe project roles
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            projectRoleUseCases.getProjectRolesUseCase(DocumentId.from(projectId))
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "역할 로드 실패: ${e.localizedMessage}")
                    }
                }
                .collect { result ->
                    when (result) {
                        is CustomResult.Success -> {
                            // 🚨 추가 안전장치: 시스템 역할 제외 (UseCase에서 이미 필터링되지만 2중 보안)
                            val filteredRoles = result.data.filter { role ->
                                !Role.isSystemRole(role.id.value)
                            }
                            
                            val roleItems = filteredRoles.map { domainRole ->
                                RoleItem(
                                    id = domainRole.id,
                                    name = domainRole.name
                                )
                            }
                            _uiState.update {
                                it.copy(isLoading = false, roles = roleItems, error = null)
                            }
                        }
                        is CustomResult.Failure -> {
                            _uiState.update {
                                it.copy(isLoading = false, error = "역할 로드 실패: ${result.error.localizedMessage}")
                            }
                        }
                        is CustomResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        is CustomResult.Initial -> {
                            // Initial state, keep loading
                        }
                        is CustomResult.Progress -> {
                            // Progress state, maintain loading
                        }
                    }
                }
        }

        // Determine capability: OWNER shortcut via provider, else ROLE_EDIT permission
        viewModelScope.launch {
            // 1) OWNER check via provider helper (using only projectId)
            when (val ownerRes =
                projectMemberUseCases.isCurrentUserOwnerUseCase(DocumentId.from(projectId))) {
                is CustomResult.Success -> {
                    if (ownerRes.data) {
                        _uiState.update { it.copy(canManageRoles = true) }
                        return@launch
                    }
                }

                else -> { /* ignore and fallback */
                }
            }

            // 2) Fallback to ROLE_EDIT permission
            val session = authRepository.getCurrentUserSession()
            if (session is CustomResult.Success) {
                when (val hasRoleEdit = projectMemberUseCases.hasProjectPermissionUseCase.invoke(
                    DocumentId.from(projectId),
                    UserId.from(session.data.userId.value),
                    RolePermission.ROLE_EDIT
                )) {
                    is CustomResult.Success -> _uiState.update { it.copy(canManageRoles = hasRoleEdit.data) }
                    else -> _uiState.update { it.copy(canManageRoles = false) }
                }
            } else {
                _uiState.update { it.copy(canManageRoles = false) }
            }
        }
    }

    fun navigateBack() {
        navigationManger.navigateBack()
    }
    /**
     * 역할 추가 버튼 클릭 시 호출
     */
    fun onAddRoleClick() {
        val state = uiState.value
        if (!state.canManageRoles) {
            viewModelScope.launch {
                _eventFlow.emit(RoleListEvent.ShowSnackbar("역할을 생성할 수 없습니다. 역할 수정 권한이 필요합니다."))
            }
            return
        }
        navigationManger.navigateTo(
            AddRoleRoute(state.projectId)
        )
    }

    /**
     * 역할 아이템 클릭 시 호출
     */
    fun onRoleClick(roleId: DocumentId) {
        val state = uiState.value
        if (!state.canManageRoles) {
            viewModelScope.launch {
                _eventFlow.emit(RoleListEvent.ShowSnackbar("역할을 수정할 수 없습니다. 역할 수정 권한이 필요합니다."))
            }
            return
        }
        navigationManger.navigateTo(
            EditRoleRoute(state.projectId, roleId.value)
        )
    }

    fun requestDeleteRole(roleItem: RoleItem) {
        val state = uiState.value
        if (!state.canManageRoles) {
            viewModelScope.launch {
                _eventFlow.emit(RoleListEvent.ShowSnackbar("역할을 삭제할 수 없습니다. 역할 수정 권한이 필요합니다."))
            }
            return
        }
        viewModelScope.launch {
            _eventFlow.emit(RoleListEvent.ShowDeleteRoleConfirmDialog(roleItem))
        }
    }

    fun confirmDeleteRole(roleId: DocumentId) {
        val state = uiState.value
        if (!state.canManageRoles) {
            viewModelScope.launch {
                _eventFlow.emit(RoleListEvent.ShowSnackbar("역할을 삭제할 수 없습니다. 역할 수정 권한이 필요합니다."))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = projectRoleUseCases.deleteRoleUseCase(roleId)
                when (result) {
                    is CustomResult.Success -> {
                        _eventFlow.emit(RoleListEvent.ShowSnackbar("역할이 삭제되었습니다."))
                        // Refresh the role list after deletion
                        refreshRoles()
                    }
                    is CustomResult.Failure -> {
                        _eventFlow.emit(RoleListEvent.ShowSnackbar("역할 삭제 실패: ${result.error.localizedMessage}"))
                    }
                    else -> {
                        _eventFlow.emit(RoleListEvent.ShowSnackbar("알 수 없는 오류가 발생했습니다."))
                    }
                }
            } catch (e: Exception) {
                _eventFlow.emit(RoleListEvent.ShowSnackbar("역할 삭제 중 오류 발생: ${e.localizedMessage}"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun refreshRoles() {
        projectRoleUseCases.getProjectRolesUseCase(DocumentId.from(projectId))
            .catch { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = "역할 로드 실패: ${e.localizedMessage}")
                }
            }
            .collect { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val roleItems = result.data.map { domainRole ->
                            RoleItem(
                                id = domainRole.id,
                                name = domainRole.name
                            )
                        }
                        _uiState.update {
                            it.copy(isLoading = false, roles = roleItems, error = null)
                        }
                    }
                    is CustomResult.Failure -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = "역할 로드 실패: ${result.error.localizedMessage}")
                        }
                    }
                    else -> {
                        // Handle other states if needed
                    }
                }
            }
    }
}
