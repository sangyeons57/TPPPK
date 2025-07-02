package com.example.feature_add_role.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.Name
import com.example.domain.model.vo.permission.PermissionType
import com.example.domain.model.vo.role.RoleIsDefault
import com.example.domain.provider.project.ProjectRoleUseCaseProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 상태 ---
data class AddRoleUiState(
    val roleName: String = "",
    val isDefault: Boolean = false,
    val permissions: Map<PermissionType, Boolean> = PermissionType.defaultPermissions(false),
    val isLoading: Boolean = false,
    val error: String? = null
)

// --- 이벤트 ---
sealed class AddRoleEvent {
    data class ShowSnackbar(val message: String) : AddRoleEvent()
    object ClearFocus : AddRoleEvent()
    object NavigateBack : AddRoleEvent()
}

@HiltViewModel
class AddRoleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val projectRoleUseCaseProvider: ProjectRoleUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    private val projectId: String = savedStateHandle.getRequiredString(RouteArgs.PROJECT_ID)

    // Provider를 통해 생성된 UseCase 그룹
    private val projectRoleUseCases = 
        projectRoleUseCaseProvider.createForProject(DocumentId.from(projectId))

    private val _uiState = MutableStateFlow(AddRoleUiState())
    val uiState: StateFlow<AddRoleUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddRoleEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * 역할 이름 변경 시 호출
     */
    fun onRoleNameChange(name: String) {
        _uiState.update {
            it.copy(
                roleName = name,
                error = null
            )
        }
    }

    /**
     * 기본 역할 여부 변경 시 호출
     */
    fun onIsDefaultChange(isDefault: Boolean) {
        _uiState.update {
            it.copy(
                isDefault = isDefault,
                error = null
            )
        }
    }

    /**
     * 권한 스위치 변경 시 호출
     */
    fun onPermissionCheckedChange(permission: PermissionType, isChecked: Boolean) {
        _uiState.update {
            val newPermissions = it.permissions.toMutableMap().apply { 
                put(permission, isChecked) 
            }
            it.copy(
                permissions = newPermissions,
                error = null
            )
        }
    }

    /**
     * 역할 생성 버튼 클릭 시 호출
     */
    fun createRole() {
        val currentState = _uiState.value
        val trimmedName = currentState.roleName.trim()

        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(error = "역할 이름을 입력해주세요.") }
            return
        }

        if (currentState.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            _eventFlow.emit(AddRoleEvent.ClearFocus)

            val roleName = Name(trimmedName)
            val isDefault = RoleIsDefault(currentState.isDefault)
            val enabledPermissions = currentState.permissions
                .filterValues { it }
                .keys
                .toList()

            println("ViewModel: Creating role '$trimmedName' in project $projectId (UseCase)")
            println("ViewModel: Enabled permissions: $enabledPermissions")

            // UseCase 호출
            val result = projectRoleUseCases.createProjectRoleUseCase(
                roleName,
                isDefault
            )

            when (result) {
                is CustomResult.Success -> {
                    // TODO: 권한 설정을 위한 별도 UseCase 호출
                    // val roleId = result.data // 새로 생성된 역할 ID
                    // val permissionsResult = projectRoleUseCases.setRolePermissionsUseCase(
                    //     roleId, enabledPermissions
                    // )
                    
                    _eventFlow.emit(AddRoleEvent.ShowSnackbar("역할이 생성되었습니다."))
                    _uiState.update { it.copy(isLoading = false) }
                    
                    // 성공 시 뒤로가기
                    _eventFlow.emit(AddRoleEvent.NavigateBack)
                }
                is CustomResult.Failure -> {
                    val errorMessage = "역할 생성 실패: ${result.error}"
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    _eventFlow.emit(AddRoleEvent.ShowSnackbar(errorMessage))
                }
                else -> {
                    val errorMessage = "역할 생성 중 알 수 없는 오류가 발생했습니다."
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                    _eventFlow.emit(AddRoleEvent.ShowSnackbar(errorMessage))
                }
            }
        }
    }

    /**
     * 뒤로가기 네비게이션 처리
     */
    fun navigateBack() {
        navigationManger.navigateBack()
    }
}