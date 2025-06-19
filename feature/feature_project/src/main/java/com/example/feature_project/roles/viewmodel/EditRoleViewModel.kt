package com.example.feature_project.roles.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.extension.getOptionalString
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.project.RolePermission // Corrected import
import com.example.domain.usecase.project.DeleteRoleUseCase
import com.example.domain.usecase.project.role.CreateRoleUseCase
import com.example.domain.usecase.project.role.GetRoleDetailsUseCase
import com.example.domain.usecase.project.role.UpdateRoleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- RolePermission enum 정의는 domain/model/RolePermission.kt 로 이동 ---

// --- UI 상태 (기존 정의 사용, hasChanges 추가) ---
data class EditRoleUiState(
    val roleId: String? = null,
    val roleName: String = "",
    val permissions: Map<RolePermission, Boolean> = RolePermission.entries.associateWith { false },
    val isDefault: Boolean = false,
    val originalRoleName: String = "",
    val originalPermissions: Map<RolePermission, Boolean> = emptyMap(),
    val originalIsDefault: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val hasChanges: Boolean = false
)

// --- 이벤트 (기존 정의 사용, ShowDeleteConfirmation 추가) ---
sealed class EditRoleEvent {
    object NavigateBack : EditRoleEvent()
    data class ShowSnackbar(val message: String) : EditRoleEvent()
    object ClearFocus : EditRoleEvent()
    object ShowDeleteConfirmation : EditRoleEvent()
}

@HiltViewModel
class EditRoleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getRoleDetailsUseCase: GetRoleDetailsUseCase,
    private val createRoleUseCase: CreateRoleUseCase,
    private val updateRoleUseCase: UpdateRoleUseCase,
    private val deleteRoleUseCase: DeleteRoleUseCase,
) : ViewModel() {

    private val projectId: String = savedStateHandle.getRequiredString(AppRoutes.Project.ARG_PROJECT_ID)
    // 역할 ID는 수정 시에만 전달되므로 옵셔널로 처리
    private val roleId: String? = savedStateHandle.getOptionalString(AppRoutes.Project.ARG_ROLE_ID)
    
    val isEditMode = roleId != null

    private val _uiState = MutableStateFlow(EditRoleUiState(roleId = roleId, isLoading = roleId != null))
    val uiState: StateFlow<EditRoleUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditRoleEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        if (roleId != null) {
            loadRoleDetails(projectId, roleId)
        }
    }

    /**
     * 수정 모드 시 역할 상세 정보 로드
     */
    private fun loadRoleDetails(projectId: String, roleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading details for role $roleId (UseCase)")

            // --- UseCase 호출 ---
            val result = getRoleDetailsUseCase(projectId, roleId) // UseCase returns Result<Role?>

            when (result) {
                is CustomResult.Success -> {
                    val role = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            roleName = role.name,
                            originalRoleName = role.name,
                            isDefault = role.isDefault,             // isDefault 로드
                            originalIsDefault = role.isDefault,     // originalIsDefault 로드
                            // Permissions will be loaded separately
                            // TODO: Call GetRolePermissionsUseCase(projectId, roleId) here
                            // TODO: and update it.permissions and it.originalPermissions
                            hasChanges = false
                        )
                    }
                    // Example of how you might call GetRolePermissionsUseCase and update state:
                    /*
                    viewModelScope.launch {
                        val permissionsResult = getRolePermissionsUseCase(projectId, roleId)
                        if (permissionsResult is CustomResult.Success) {
                            _uiState.update {
                                it.copy(
                                    permissions = permissionsResult.data.associateBy { it.permission } // Assuming data is List<RolePermissionDetail>
                                        .mapValues { it.value.isEnabled }, // Adjust based on actual data structure
                                    originalPermissions = permissionsResult.data.associateBy { it.permission }
                                        .mapValues { it.value.isEnabled }
                                )
                            }
                        } else if (permissionsResult is CustomResult.Failure) {
                            // Handle permission loading failure
                             _uiState.update { it.copy(error = "권한 정보를 불러오지 못했습니다: ${permissionsResult.error}") }
                        }
                    }
                    */
                }
                is CustomResult.Failure -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = "역할 정보를 불러오지 못했습니다: ${result.error}")
                    }
                    _eventFlow.emit(EditRoleEvent.ShowSnackbar("역할 정보를 불러오지 못했습니다."))
                    _eventFlow.emit(EditRoleEvent.NavigateBack)
                }
                else -> {
                    // Role not found
                    _uiState.update { it.copy(isLoading = false, error = "역할 정보를 찾을 수 없습니다.") }
                    _eventFlow.emit(EditRoleEvent.ShowSnackbar("역할 정보를 찾을 수 없습니다."))
                    _eventFlow.emit(EditRoleEvent.NavigateBack)
                }
            }
        }
    }

    /**
     * 역할 이름 변경 시 호출
     */
    fun onRoleNameChange(name: String) {
        _uiState.update {
            it.copy(
                roleName = name,
                error = null,
                hasChanges = name != it.originalRoleName || 
                             it.permissions != it.originalPermissions || 
                             it.isDefault != it.originalIsDefault // isDefault 변경 감지 추가
            )
        }
    }

    /**
     * 권한 스위치 변경 시 호출
     */
    fun onPermissionCheckedChange(permission: RolePermission, isChecked: Boolean) {
        _uiState.update {
            val newPermissions = it.permissions.toMutableMap().apply { put(permission, isChecked) }
            it.copy(
                permissions = newPermissions,
                hasChanges = it.roleName != it.originalRoleName || 
                             newPermissions != it.originalPermissions ||
                             it.isDefault != it.originalIsDefault // isDefault 변경 감지 추가
            )
        }
    }
    
    /**
     * 기본 역할 여부 스위치 변경 시 호출
     */
    fun onIsDefaultChange(isDefault: Boolean) {
        _uiState.update {
            it.copy(
                isDefault = isDefault,
                hasChanges = it.roleName != it.originalRoleName || 
                             it.permissions != it.originalPermissions ||
                             isDefault != it.originalIsDefault
            )
        }
    }

    /**
     * 저장 또는 생성 버튼 클릭 시 호출
     */
    fun saveRole() {
        val currentState = _uiState.value
        val currentName = currentState.roleName.trim()

        if (currentName.isBlank()) {
            _uiState.update { it.copy(error = "역할 이름을 입력해주세요.") }
            return
        }

        if (currentState.roleId != null && !currentState.hasChanges) {
            viewModelScope.launch {
                _eventFlow.emit(EditRoleEvent.ShowSnackbar("변경된 내용이 없습니다."))
                _eventFlow.emit(EditRoleEvent.NavigateBack)
            }
            return
        }

        if (currentState.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            _eventFlow.emit(EditRoleEvent.ClearFocus)

            val nameToSave = currentName
            val permissionsListToSave = currentState.permissions.filterValues { it }.keys.toList()
            val isDefaultToSave = currentState.isDefault

            val result = if (currentState.roleId == null) {
                println("ViewModel: Creating role '$nameToSave' in project $projectId (UseCase)")
                // Permissions are saved separately after role creation
                createRoleUseCase(projectId, nameToSave, isDefaultToSave)
                // TODO: After successful role creation, get the new roleId from the result
                // TODO: Then call a new SetRolePermissionsUseCase(projectId, newRoleId, permissionsListToSave)
            } else {
                println("ViewModel: Updating role ${currentState.roleId} to '$nameToSave' (UseCase)")
                updateRoleUseCase(projectId, currentState.roleId, nameToSave, isDefaultToSave)
            }

            if (result.isSuccess) {
                val message = if (currentState.roleId == null) {
                    // val newRoleId = (result as Result.Success<String>).value // If you need the new ID
                    "역할이 생성되었습니다."
                } else {
                    "역할이 수정되었습니다."
                }
                _eventFlow.emit(EditRoleEvent.ShowSnackbar(message))
                _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
            } else {
                val errorMessage = if (currentState.roleId == null) "역할 생성 실패" else "역할 수정 실패"
                val errorDetail = (result as CustomResult.Failure).error
                _uiState.update { it.copy(isLoading = false, error = errorMessage + (errorDetail?.let { ": $it" } ?: "")) }
            }
        }
    }

    /**
     * 삭제 버튼 클릭 시 (삭제 확인 다이얼로그 표시 요청)
     */
    fun requestDeleteRoleConfirmation() {
        if (uiState.value.roleId != null && !uiState.value.isLoading) {
            viewModelScope.launch {
                _eventFlow.emit(EditRoleEvent.ShowDeleteConfirmation)
            }
        }
    }

    /**
     * 삭제 확인 다이얼로그에서 '삭제' 버튼 클릭 시
     */
    fun confirmDeleteRole() {
        val roleIdToDelete = uiState.value.roleId
        if (roleIdToDelete == null || uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Deleting role $roleIdToDelete (UseCase)")

            // --- UseCase 호출 ---
            val result = deleteRoleUseCase(projectId, roleIdToDelete) // Pass projectId

            if (result.isSuccess) {
                _eventFlow.emit(EditRoleEvent.ShowSnackbar("역할이 삭제되었습니다."))
                _uiState.update { it.copy(isLoading = false, deleteSuccess = true) }
            } else {
                val errorDetail = (result as CustomResult.Failure).error
                _eventFlow.emit(EditRoleEvent.ShowSnackbar("역할 삭제 실패" + (errorDetail?.let { ": $it" } ?: "")))
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}