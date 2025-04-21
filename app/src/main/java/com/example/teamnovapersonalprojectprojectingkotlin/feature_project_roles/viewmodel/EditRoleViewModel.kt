package com.example.teamnovapersonalprojectprojectingkotlin.feature_project_roles.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.RolePermission // ★ Domain 모델 Import
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.ProjectRoleRepository // ★ Domain Repository Import
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay // TODO: 실제 구현 시 제거
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random // TODO: 실제 구현 시 제거

// --- RolePermission enum 정의는 domain/model/RolePermission.kt 로 이동 ---

// --- UI 상태 (기존 정의 사용, hasChanges 추가) ---
data class EditRoleUiState(
    val roleId: String? = null,
    val roleName: String = "",
    val permissions: Map<RolePermission, Boolean> = RolePermission.values().associateWith { false },
    val originalRoleName: String = "",
    val originalPermissions: Map<RolePermission, Boolean> = emptyMap(),
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

// --- Repository 인터페이스 정의는 domain/repository/ProjectRoleRepository.kt 로 이동 ---

@HiltViewModel
class EditRoleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val projectRoleRepository: ProjectRoleRepository // ★ Domain Repository 주입
) : ViewModel() {

    // 네비게이션 인자 가져오기 (실제 키 이름 확인 필요)
    private val projectId: String = savedStateHandle["projectId"] ?: error("projectId가 필요합니다.")
    private val roleId: String? = savedStateHandle["roleId"] // 수정 모드일 경우 역할 ID

    private val _uiState = MutableStateFlow(EditRoleUiState(roleId = roleId, isLoading = roleId != null)) // 수정 모드면 초기 로딩
    val uiState: StateFlow<EditRoleUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditRoleEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        if (roleId != null) {
            loadRoleDetails(roleId)
        }
    }

    /**
     * 수정 모드 시 역할 상세 정보 로드
     */
    private fun loadRoleDetails(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading details for role $id")

            // --- Repository 호출 ---
            val result = projectRoleRepository.getRoleDetails(id) // ★ Repository 사용

            if (result.isSuccess) {
                val (loadedName, loadedPermissions) = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        roleName = loadedName,
                        originalRoleName = loadedName,
                        permissions = loadedPermissions,
                        originalPermissions = loadedPermissions,
                        hasChanges = false // 초기 로드 시 변경 없음
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = "역할 정보를 불러오지 못했습니다: ${result.exceptionOrNull()?.message}")
                }
                _eventFlow.emit(EditRoleEvent.ShowSnackbar("역할 정보를 불러오지 못했습니다."))
                _eventFlow.emit(EditRoleEvent.NavigateBack) // 로드 실패 시 뒤로가기
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

            // Repository 호출 (생성 또는 수정)
            val result = if (currentState.roleId == null) {
                println("ViewModel: Creating role '$nameToSave' in project $projectId")
                projectRoleRepository.createRole(projectId, nameToSave, permissionsToSave) // ★ 생성 호출
            } else {
                println("ViewModel: Updating role ${currentState.roleId} to '$nameToSave'")
                projectRoleRepository.updateRole(currentState.roleId, nameToSave, permissionsToSave) // ★ 수정 호출
            }

            if (result.isSuccess) {
                val message = if (currentState.roleId == null) "역할이 생성되었습니다." else "역할이 수정되었습니다."
                _eventFlow.emit(EditRoleEvent.ShowSnackbar(message))
                _uiState.update { it.copy(isLoading = false, saveSuccess = true) } // 성공 및 네비게이션 트리거
            } else {
                val errorMessage = if (currentState.roleId == null) "역할 생성 실패" else "역할 수정 실패"
                val errorDetail = result.exceptionOrNull()?.message
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
            println("ViewModel: Deleting role $roleIdToDelete")

            val result = projectRoleRepository.deleteRole(roleIdToDelete) // ★ 삭제 호출

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