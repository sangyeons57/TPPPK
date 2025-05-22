package com.example.feature_project.roles.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.extension.getOptionalString
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.RolePermission
import com.example.domain.usecase.project.CreateRoleUseCase
import com.example.domain.usecase.project.DeleteRoleUseCase
import com.example.domain.usecase.project.GetRoleDetailsUseCase
import com.example.domain.usecase.project.UpdateRoleUseCase
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
    val originalRoleName: String = "",
    val originalPermissions: Map<RolePermission, Boolean> = emptyMap(),
    val originalIsDefault: Boolean = false, // Added
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false,
    val hasChanges: Boolean = false // ★ 변경 사항 유무 플래그 추가
)

// --- 이벤트 (기존 정의 사용, ShowDeleteConfirmation 추가) ---
sealed class EditRoleEvent {
    object NavigateBack : EditRoleEvent()
    data class ShowSnackbar(val message: String) : EditRoleEvent()
    object ClearFocus : EditRoleEvent()
    object ShowDeleteConfirmation : EditRoleEvent() // ★ 삭제 확인 다이얼로그 표시 이벤트
}

@HiltViewModel
class EditRoleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getRoleDetailsUseCase: GetRoleDetailsUseCase,
    private val createRoleUseCase: CreateRoleUseCase,
    private val updateRoleUseCase: UpdateRoleUseCase,
    private val deleteRoleUseCase: DeleteRoleUseCase
) : ViewModel() {

    private val projectId: String = savedStateHandle.getRequiredString(AppRoutes.Project.ARG_PROJECT_ID)
    // 역할 ID는 수정 시에만 전달되므로 옵셔널로 처리
    private val roleId: String? = savedStateHandle.getOptionalString(AppRoutes.Project.ARG_ROLE_ID)
    
    val isEditMode = roleId != null // 수정 모드 여부

    private val _uiState = MutableStateFlow(EditRoleUiState(roleId = roleId, isLoading = roleId != null)) // 수정 모드면 초기 로딩
    val uiState: StateFlow<EditRoleUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditRoleEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        if (roleId != null && projectId != null) {
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

            if (result.isSuccess) {
                val role = result.getOrThrow() // role is Role?
                if (role != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            roleName = role.name,
                            originalRoleName = role.name,
                            permissions = role.permissions,
                            originalPermissions = role.permissions,
                            originalIsDefault = role.isDefault, // Store original isDefault
                            hasChanges = false
                        )
                    }
                } else {
                    // Role not found
                    _uiState.update { it.copy(isLoading = false, error = "역할 정보를 찾을 수 없습니다.") }
                    _eventFlow.emit(EditRoleEvent.ShowSnackbar("역할 정보를 찾을 수 없습니다."))
                    _eventFlow.emit(EditRoleEvent.NavigateBack)
                }
            } else { // Failure case
                _uiState.update {
                    it.copy(isLoading = false, error = "역할 정보를 불러오지 못했습니다: ${result.exceptionOrNull()?.localizedMessage}")
                }
                _eventFlow.emit(EditRoleEvent.ShowSnackbar("역할 정보를 불러오지 못했습니다."))
                _eventFlow.emit(EditRoleEvent.NavigateBack)
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
                error = null, // 에러 초기화
                hasChanges = name != it.originalRoleName || it.permissions != it.originalPermissions // ★ 변경 여부 확인
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
                hasChanges = it.roleName != it.originalRoleName || newPermissions != it.originalPermissions // ★ 변경 여부 확인
            )
        }
    }

    /**
     * 저장 또는 생성 버튼 클릭 시 호출
     */
    fun saveRole() {
        val currentState = _uiState.value
        val currentName = currentState.roleName.trim()

        // 이름 유효성 검사
        if (currentName.isBlank()) {
            _uiState.update { it.copy(error = "역할 이름을 입력해주세요.") }
            return
        }

        // 수정 모드일 때 변경 사항 없으면 저장 안 함
        if (currentState.roleId != null && !currentState.hasChanges) {
            viewModelScope.launch {
                _eventFlow.emit(EditRoleEvent.ShowSnackbar("변경된 내용이 없습니다."))
                _eventFlow.emit(EditRoleEvent.NavigateBack)
            }
            return
        }

        if (currentState.isLoading) return // 로딩 중 중복 방지

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            _eventFlow.emit(EditRoleEvent.ClearFocus)

            val nameToSave = currentName
            val permissionsToSave = currentState.permissions

            // --- UseCase 호출 (생성 또는 수정) ---
            val result: Result<Any> = if (currentState.roleId == null) { // Result<Any> to handle different success types
                println("ViewModel: Creating role '$nameToSave' in project $projectId (UseCase)")
                // Assume new roles are not default by default, or add UI for this if needed
                createRoleUseCase(projectId, nameToSave, permissionsToSave, isDefault = false) // isDefault added
            } else {
                println("ViewModel: Updating role ${currentState.roleId} to '$nameToSave' (UseCase)")
                // Pass projectId. Pass originalIsDefault if not changing, or get from a UI element if editable.
                updateRoleUseCase(projectId, currentState.roleId, nameToSave, permissionsToSave, isDefault = currentState.originalIsDefault)
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
                val errorDetail = result.exceptionOrNull()?.localizedMessage
                _uiState.update { it.copy(isLoading = false, error = errorMessage + (errorDetail?.let { ": $it" } ?: "")) }
            }
        }
    }

    /**
     * 삭제 버튼 클릭 시 (삭제 확인 다이얼로그 표시 요청)
     */
    fun requestDeleteRoleConfirmation() {
        if (uiState.value.roleId != null && !uiState.value.isLoading) { // 수정 모드이고 로딩 중 아닐 때만
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
        if (roleIdToDelete == null || uiState.value.isLoading) return // 생성 모드이거나 로딩 중이면 무시

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Deleting role $roleIdToDelete (UseCase)")

            // --- UseCase 호출 ---
            val result = deleteRoleUseCase(projectId, roleIdToDelete) // Pass projectId

            if (result.isSuccess) {
                _eventFlow.emit(EditRoleEvent.ShowSnackbar("역할이 삭제되었습니다."))
                _uiState.update { it.copy(isLoading = false, deleteSuccess = true) } // 성공 및 네비게이션 트리거
            } else {
                val errorDetail = result.exceptionOrNull()?.message
                _eventFlow.emit(EditRoleEvent.ShowSnackbar("역할 삭제 실패" + (errorDetail?.let { ": $it" } ?: "")))
                _uiState.update { it.copy(isLoading = false) } // 로딩만 해제
            }
        }
    }
}