package com.example.feature_project.members.viewmodel // 경로 확인!

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_navigation.destination.AppRoutes
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.ProjectMember
import com.example.domain.model.Role
// import com.example.domain.repository.ProjectMemberRepository // Remove Repo import
// import com.example.domain.repository.ProjectRoleRepository // Remove Repo import
import com.example.domain.usecase.project.GetProjectMemberDetailsUseCase // Import UseCase
import com.example.domain.usecase.project.UpdateMemberRolesUseCase // Import UseCase
import com.example.domain.usecase.project.role.GetProjectRolesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 모델 ---
// 역할 선택 목록에 사용될 데이터 클래스
data class RoleSelectionItem(
    val id: String,
    val name: String,
    var isSelected: Boolean // 선택 여부 (UI 상태)
)

// --- UI 상태 ---
data class EditMemberUiState(
    val memberInfo: ProjectMember? = null, // 멤버 기본 정보 (Domain 모델 직접 사용 가능)
    val availableRoles: List<RoleSelectionItem> = emptyList(), // 선택 가능한 전체 역할 목록 (UI 모델)
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

// --- 이벤트 ---
sealed class EditMemberEvent {
    object NavigateBack : EditMemberEvent()
    data class ShowSnackbar(val message: String) : EditMemberEvent()
}

// --- ViewModel ---
@HiltViewModel
class EditMemberViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getProjectMemberDetailsUseCase: GetProjectMemberDetailsUseCase, // Inject UseCase
    private val getProjectRolesUseCase: GetProjectRolesUseCase, // Inject UseCase
    private val updateMemberRolesUseCase: UpdateMemberRolesUseCase // Inject UseCase
) : ViewModel() {

    private val projectId: String = savedStateHandle.getRequiredString(AppRoutes.Project.ARG_PROJECT_ID)
    private val userId: String = savedStateHandle.getRequiredString(AppRoutes.Project.ARG_USER_ID)

    // 예시: 만약 멤버 편집 화면에서 특정 역할 ID를 옵션으로 받는다면?
    // private val optionalRoleId: String? = savedStateHandle.getOptionalString("optionalRoleId")

    private val _uiState = MutableStateFlow(EditMemberUiState(isLoading = true))
    val uiState: StateFlow<EditMemberUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<EditMemberEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // 초기 선택된 역할 ID 저장용 (변경 여부 확인)
    private var originalSelectedRoleIds: Set<String?> = emptySet()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 1. 멤버 정보 가져오기 (UseCase 사용)
            val memberResult = getProjectMemberDetailsUseCase(projectId, userId)

            // 2. 전체 역할 목록 가져오기 (UseCase 사용)
            val rolesResult = getProjectRolesUseCase(projectId)

            if (memberResult.isSuccess && rolesResult.isSuccess) {
                val member = memberResult.getOrThrow()
                val allRoles = rolesResult.getOrThrow()

                // 현재 멤버가 가진 역할 ID Set 생성 (ProjectMember의 roles 필드 사용)
                originalSelectedRoleIds = member?.roles?.map { it.id }?.filter { it != null }?.toSet().orEmpty() // Null-safe 처리

                // 전체 역할 목록을 UI 모델(RoleSelectionItem)로 변환하고, 현재 멤버의 역할 선택 상태 반영
                val roleSelectionItems = allRoles.map { role ->
                    RoleSelectionItem(
                        id = role.id ?: "", // Null-safe
                        name = role.name,
                        isSelected = role.id in originalSelectedRoleIds // 멤버가 가진 역할인지 확인
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        memberInfo = member,
                        availableRoles = roleSelectionItems
                    )
                }
            } else {
                val errorMsg = memberResult.exceptionOrNull()?.message ?: rolesResult.exceptionOrNull()?.message ?: "데이터 로드 실패"
                _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                _eventFlow.emit(EditMemberEvent.ShowSnackbar("정보를 불러오는 데 실패했습니다: $errorMsg"))
            }
        }
    }

    /** 역할 선택/해제 시 호출 */
    fun onRoleSelectionChanged(roleId: String, isSelected: Boolean) {
        _uiState.update { currentState ->
            val updatedRoles = currentState.availableRoles.map {
                if (it.id == roleId) it.copy(isSelected = isSelected) else it
            }
            currentState.copy(availableRoles = updatedRoles, error = null) // 에러 메시지 초기화
        }
    }

    /** 변경사항 저장 버튼 클릭 시 호출 */
    fun saveMemberRoles() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isSaving) return

        val currentSelectedRoleIds = currentState.availableRoles
            .filter { it.isSelected }
            .map { it.id }
            .toSet()

        if (currentSelectedRoleIds == originalSelectedRoleIds) {
            viewModelScope.launch { _eventFlow.emit(EditMemberEvent.ShowSnackbar("변경된 내용이 없습니다.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            // _eventFlow.emit(EditMemberEvent.ShowSnackbar("역할을 저장하는 중...")) // 스낵바 중복 표시 방지 위해 일단 주석 처리 (성공/실패 시 표시)

            // UseCase 호출
            val result = updateMemberRolesUseCase(projectId, userId, currentSelectedRoleIds.toList())

            if (result.isSuccess) {
                originalSelectedRoleIds = currentSelectedRoleIds
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                _eventFlow.emit(EditMemberEvent.ShowSnackbar("멤버 역할이 성공적으로 업데이트되었습니다."))
                _eventFlow.emit(EditMemberEvent.NavigateBack)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "알 수 없는 오류"
                _uiState.update { it.copy(isSaving = false, error = "역할 업데이트 실패: $errorMsg") }
                _eventFlow.emit(EditMemberEvent.ShowSnackbar("역할 업데이트에 실패했습니다."))
            }
        }
    }

    // --- 내부 Repository 및 데이터 클래스 정의 삭제됨 ---
}