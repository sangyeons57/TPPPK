package com.example.feature_project_setting_screen.viewmodel.viewmodel

// Domain 계층에서 모델 및 리포지토리 인터페이스 임포트 (올바른 경로)
// import com.example.domain.repository.ProjectSettingRepository // Remove Repo import
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_common.result.exceptionOrNull
import com.example.core_navigation.destination.RouteArgs
import com.example.core_navigation.extension.getRequiredString
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.project.ProjectName
import com.example.domain.provider.project.CoreProjectUseCaseProvider
import com.example.domain.provider.project.ProjectAssetsUseCaseProvider
import com.example.domain.provider.project.ProjectChannelUseCaseProvider
import com.example.domain.provider.project.ProjectStructureUseCaseProvider
import com.example.feature_model.CategoryUiModel
import com.example.feature_model.ChannelUiModel
import com.example.core_navigation.core.NavigationManger
import com.example.core_navigation.core.EditCategoryRoute
import com.example.core_navigation.core.CreateCategoryRoute
import com.example.core_navigation.core.EditChannelRoute
import com.example.core_navigation.core.CreateChannelRoute
import com.example.core_navigation.core.MemberListRoute
import com.example.core_navigation.core.RoleListRoute
import com.example.core_ui.components.project.ProjectImageUpdateEventManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- UI 상태 ---
data class ProjectSettingUiState(
    val projectId: DocumentId,
    val projectName: ProjectName = ProjectName.EMPTY,
    val projectImageUrl: String? = null,
    val selectedImageUri: Uri? = null,
    val hasImageChanges: Boolean = false,
    val categories: List<CategoryUiModel> = emptyList(), // Changed to CategoryUiModel
    val isLoading: Boolean = false,
    val error: String? = null,
    val showRenameProjectDialog: Boolean = false,
    val showDeleteProjectDialog: Boolean = false,
    val showRemoveImageDialog: Boolean = false,
    val isRemovingImage: Boolean = false // 기본 프로젝트 프로필 설정 중인지 여부
)

// --- 이벤트 ---
sealed class ProjectSettingEvent {
    data class ShowSnackbar(val message: String) : ProjectSettingEvent()
    object RequestImagePick : ProjectSettingEvent()
    data class ShowDeleteCategoryConfirm(val category: CategoryUiModel) : ProjectSettingEvent()
    data class ShowDeleteChannelConfirm(val channel: ChannelUiModel) : ProjectSettingEvent()
}

@HiltViewModel
class ProjectSettingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManger: NavigationManger,
    private val coreProjectUseCaseProvider: CoreProjectUseCaseProvider,
    private val projectStructureUseCaseProvider: ProjectStructureUseCaseProvider,
    private val projectChannelUseCaseProvider: ProjectChannelUseCaseProvider,
    private val projectAssetsUseCaseProvider: ProjectAssetsUseCaseProvider,
    private val projectImageUpdateEventManager: ProjectImageUpdateEventManager
) : ViewModel() {

    val projectId: DocumentId = savedStateHandle.getRequiredString(RouteArgs.PROJECT_ID)
        .let { DocumentId.from((it)) }

    // Create UseCase groups via providers
    private val coreProjectUseCases = coreProjectUseCaseProvider.createForProject(projectId, UserId.EMPTY)
    private val projectStructureUseCases = projectStructureUseCaseProvider.createForProject(projectId)
    // Note: projectChannelUseCases는 특정 채널 작업 시 필요한 categoryId와 함께 동적으로 생성됨
    private val projectAssetsUseCases = projectAssetsUseCaseProvider.createForProject(projectId.value)

    private val _uiState = MutableStateFlow(ProjectSettingUiState(projectId = projectId, isLoading = true))
    val uiState: StateFlow<ProjectSettingUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ProjectSettingEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadProjectStructure()
    }

    private fun loadProjectStructure() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            println("ViewModel: Loading structure for project $projectId (UseCase)")

            // Load project details to get name and image URL
            when (val projectResult = coreProjectUseCases.getProjectDetailsStreamUseCase(projectId).first()) {
                is CustomResult.Success -> {
                    val project = projectResult.data
                    _uiState.update {
                        it.copy(
                            projectName = project.name,
                            projectImageUrl = project.imageUrl?.value
                        )
                    }
                }
                is CustomResult.Failure -> {
                    println("Failed to load project details: ${projectResult.error}")
                }
                else -> {
                    println("Unknown result for project details")
                }
            }

            // Load project structure (categories)
            when (val result = projectStructureUseCases.getProjectAllCategoriesUseCase().first()) {
                is CustomResult.Success -> {
                    val domainCategories = result.data // This is List<com.example.domain.model.base.Category>
                    val uiCategories = domainCategories.map { domainCategory ->
                        // TODO: Fetch actual channels for this domainCategory.id using another use case if needed.
                        // For now, placeholder:
                        val placeholderChannels = emptyList<ChannelUiModel>() // Empty list for now
                        CategoryUiModel(
                            id = domainCategory.id,
                            name = domainCategory.name,
                            channels = placeholderChannels
                        )
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            categories = uiCategories // Assign mapped UI models
                        )
                    }
                }

                is CustomResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "프로젝트 정보를 불러오지 못했습니다: ${result.error}"
                        )
                    }
                    _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 구조 로딩 실패")) // Notify user               }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    // --- 카테고리 관련 액션 ---
    fun requestEditCategory(categoryId: String) {
        navigationManger.navigateTo(
            EditCategoryRoute(projectId.value, categoryId)
        )
    }
    fun requestDeleteCategory(category: CategoryUiModel) { // Changed to CategoryUiModel
        viewModelScope.launch { _eventFlow.emit(
            ProjectSettingEvent.ShowDeleteCategoryConfirm(
                category
            )
        ) }
    }
    fun confirmDeleteCategory(category: CategoryUiModel) { // Changed to CategoryUiModel
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // Show loading
            // TODO: DeleteCategoryUseCase 호출
            println("Deleting Category: ${category.id} (UseCase)") // Used category.id
            val result = projectStructureUseCases.deleteCategoryUseCase(category.id) // Used category.id
            // delay(500) // Remove delay
            when (result) {
                is CustomResult.Success -> {
                    _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("카테고리가 삭제되었습니다."))
                    loadProjectStructure() // Refresh structure
                }
                is CustomResult.Failure -> {
                    _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("카테고리 삭제 실패: ${result.error.message}"))
                    _uiState.update { it.copy(isLoading = false) } // Hide loading on failure
                }
                else -> {
                    _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("카테고리 삭제 실패: 알수 없는 에러"))
                    _uiState.update { it.copy(isLoading = false) } // Hide loading on failure
                }
            }
            // isLoading will be turned off by loadProjectStructure on success
        }
    }
    fun requestCreateCategory() {
        navigationManger.navigateTo(
            CreateCategoryRoute(projectId.value)
        )
    }

    // --- 채널 관련 액션 ---
    fun requestEditChannel(channelId: String) {
        navigationManger.navigateTo(
            EditChannelRoute(projectId.value, channelId)
        )
    }
    fun requestDeleteChannel(channel: ChannelUiModel) { // Changed to ChannelUiModel
        viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowDeleteChannelConfirm(channel)) }
    }
    fun confirmDeleteChannel(channel: ChannelUiModel) { // Changed to ChannelUiModel
        /** 잠시 멈춰두기
        viewModelScope.launch {
             _uiState.update { it.copy(isLoading = true) } // Show loading
            // TODO: DeleteChannelUseCase 호출
            println("Deleting Channel: ${channel.id} (UseCase)") // Used channel.id
             val result = projectChannelUseCases.deleteChannelUseCase(projectId, channel.categoryId, channel.id) // Used channel.id
            // delay(500) // Remove delay
            when (result) {
                is CustomResult.Success -> {
                     _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("채널이 삭제되었습니다."))
                     loadProjectStructure() // Refresh structure
                }
                is CustomResult.Failure -> {
                     _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("채널 삭제 실패: ${result.error}"))
                     _uiState.update { it.copy(isLoading = false) } // Hide loading on failure
                 }
                else-> {
                    Log.e("ProjectSettingViewModel", "Unknown result type: $result")
                }
            }
             // isLoading will be turned off by loadProjectStructure on success
        }
        **/
    }
    fun requestCreateChannel(categoryId: String) {
        navigationManger.navigateTo(
            CreateChannelRoute(projectId.value, categoryId)
        )
    }

    // --- 멤버/역할 관리 네비게이션 ---
    fun requestManageMembers() {
        navigationManger.navigateTo(
            MemberListRoute(projectId.value)
        )
    }
    fun requestManageRoles() {
        navigationManger.navigateTo(
            RoleListRoute(projectId.value)
        )
    }

    // --- 프로젝트 이름 변경 ---
    fun requestRenameProject() {
        viewModelScope.launch {
            when (val result = coreProjectUseCases.getProjectDetailsStreamUseCase(projectId).first()){
                is CustomResult.Success -> {
                    _uiState.update { it.copy(projectName = result.data.name, showRenameProjectDialog = true) }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            projectName = ProjectName.EMPTY,
                            showRenameProjectDialog = false
                        )
                    }
                    _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 정보를 가지고 오는데 실패했습니다."))
                }

            }
        }
    }

    fun confirmRenameProject(newName: ProjectName) {
        dismiss()
        val trimmedNewName = newName.trim()
        if (trimmedNewName.isBlank()) {
            viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 이름은 비워둘 수 없습니다.")) }
            return
        }
        if (trimmedNewName == _uiState.value.projectName) {
             viewModelScope.launch { _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("현재 이름과 동일합니다.")) }
            return
        }

        viewModelScope.launch {
             _uiState.update { it.copy(isLoading = true) } // Show loading
            println("Renaming Project $projectId to '$trimmedNewName' (UseCase)")
            val result = coreProjectUseCases.renameProjectUseCase(projectId, trimmedNewName)
            if (result.isSuccess) {
                 // Update UI state directly for immediate feedback, structure reload might not be needed
                 _uiState.update { it.copy(projectName = trimmedNewName, isLoading = false) }
                 _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 이름이 변경되었습니다."))
            } else {
                 _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("이름 변경 실패: ${result.exceptionOrNull()?.message}"))
                 _uiState.update { it.copy(isLoading = false) } // Hide loading on failure
            }
        }
    }

    // --- 프로젝트 삭제 ---
    fun requestDeleteProject() {
        _uiState.update { it.copy(showDeleteProjectDialog = true) }
    }

    fun confirmDeleteProject() {
        dismiss() // 다이얼로그 닫기
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // Show loading
            Log.d("ProjectSettingViewModel", "Deleting Project $projectId (UseCase)")
            
            try {
                val result = coreProjectUseCases.deleteProjectUseCase(projectId)
                Log.d("ProjectSettingViewModel", "Delete result: $result")
                
                when (result) {
                    is CustomResult.Success -> {
                        Log.d("ProjectSettingViewModel", "Project deletion successful")
                        _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트가 삭제되었습니다."))
                        navigationManger.navigateBack() // Navigate back on success
                        // No need to turn off loading as we are navigating away
                    }
                    is CustomResult.Failure -> {
                        Log.e("ProjectSettingViewModel", "Project deletion failed: ${result.error.message}", result.error)
                        _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 삭제 실패: ${result.error.message}"))
                        _uiState.update { it.copy(isLoading = false) } // Hide loading on failure
                    }
                    is CustomResult.Loading -> {
                        Log.d("ProjectSettingViewModel", "Project deletion is loading...")
                        // Keep loading state
                    }
                    is CustomResult.Initial -> {
                        Log.w("ProjectSettingViewModel", "Project deletion returned Initial state")
                        _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 삭제 실패: 초기 상태"))
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is CustomResult.Progress -> {
                        Log.d("ProjectSettingViewModel", "Project deletion progress: ${result.progress}")
                        // Keep loading state and show progress if needed
                    }
                    else -> {
                        Log.e("ProjectSettingViewModel", "Project deletion returned unknown result type: $result")
                        _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 삭제 실패: 알 수 없는 오류"))
                        _uiState.update { it.copy(isLoading = false) } // Hide loading on failure
                    }
                }
            } catch (e: Exception) {
                Log.e("ProjectSettingViewModel", "Exception during project deletion", e)
                _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 삭제 실패: ${e.message}"))
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun dismiss() {
        _uiState.update { it.copy(showRenameProjectDialog = false, showDeleteProjectDialog = false) }
    }

    // --- 프로젝트 이미지 관련 ---
    fun onProjectImageClicked() {
        viewModelScope.launch {
            _eventFlow.emit(ProjectSettingEvent.RequestImagePick)
        }
    }

    fun handleImageSelection(uri: Uri?) {
        if (uri == null) {
            viewModelScope.launch {
                _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("이미지 선택이 취소되었습니다."))
            }
            return
        }

        // 이미지 유효성 검사
        val validationResult = validateImageUri(uri)
        if (!validationResult.isValid) {
            viewModelScope.launch {
                _eventFlow.emit(ProjectSettingEvent.ShowSnackbar(validationResult.errorMessage!!))
            }
            return
        }

        _uiState.update { currentState ->
            currentState.copy(
                selectedImageUri = uri,
                hasImageChanges = true,
                error = null
            )
        }
        
        viewModelScope.launch {
            _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("이미지가 선택되었습니다. 저장 버튼을 눌러 적용하세요."))
        }
    }

    private fun validateImageUri(uri: Uri): ValidationResult {
        // 여기서는 기본적인 검증만 수행
        // 실제 파일 크기와 형식 검증은 Firebase Storage Rules에서 처리됨
        
        val scheme = uri.scheme
        if (scheme != "content" && scheme != "file") {
            return ValidationResult(false, "지원되지 않는 이미지 형식입니다.")
        }

        return ValidationResult(true)
    }

    private data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    fun onSaveProjectImageClicked() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val selectedImageUri = currentState.selectedImageUri
            
            if (selectedImageUri == null) {
                _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("선택된 이미지가 없습니다."))
                return@launch
            }

            if (!currentState.hasImageChanges) {
                _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("변경된 이미지가 없습니다."))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val imageResult = projectAssetsUseCases.uploadProjectProfileImageUseCase(projectId, selectedImageUri)
                when (imageResult) {
                    is CustomResult.Success -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                // 이미지 업로드는 완료되었지만, Functions 가 Storage -> Firestore 업데이트를
                            // 완료하기 전까지는 기존 Storage 이미지를 그대로 보여줄 수 있다.
                            // 따라서 로컬에서 선택한 이미지 URI를 유지하여 사용자에게 즉시 적용된 것처럼 보여준다.
                            hasImageChanges = false // 더 이상 저장 버튼은 필요 없음
                        )
                        }
                        _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 이미지가 성공적으로 업데이트되었습니다."))
                        
                        // 전역 이벤트 발생으로 모든 화면들에 알림
                        projectImageUpdateEventManager.notifyProjectImageUpdated(projectId.value)
                        
                        // 프로젝트 정보 다시 로드하여 새 이미지 URL 가져오기
                        loadProjectStructure()
                    }
                    is CustomResult.Failure -> {
                        _uiState.update { it.copy(isLoading = false) }
                        val errorMessage = getHumanReadableErrorMessage(imageResult.error)
                        _eventFlow.emit(ProjectSettingEvent.ShowSnackbar(errorMessage))
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("이미지 업로드 중 알 수 없는 오류가 발생했습니다."))
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "이미지 업로드 중 오류가 발생했습니다",
                        isLoading = false
                    )
                }
                _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("이미지 업로드 실패: ${e.message}"))
            }
        }
    }

    private fun getHumanReadableErrorMessage(error: Exception): String {
        val errorMessage = error.message?.lowercase() ?: ""
        
        return when {
            errorMessage.contains("network") || errorMessage.contains("timeout") -> 
                "네트워크 연결을 확인해주세요."
            errorMessage.contains("permission") || errorMessage.contains("access") -> 
                "파일 접근 권한이 없습니다."
            errorMessage.contains("size") || errorMessage.contains("large") -> 
                "파일 크기가 너무 큽니다. 5MB 이하의 이미지를 선택해주세요."
            errorMessage.contains("format") || errorMessage.contains("invalid") -> 
                "지원되지 않는 이미지 형식입니다. JPG, PNG 파일을 선택해주세요."
            errorMessage.contains("storage") -> 
                "서버 저장 공간에 문제가 있습니다. 잠시 후 다시 시도해주세요."
            errorMessage.contains("quota") || errorMessage.contains("limit") -> 
                "업로드 한도에 도달했습니다. 잠시 후 다시 시도해주세요."
            else -> "이미지 업로드에 실패했습니다. 다시 시도해주세요."
        }
    }

    // --- 프로젝트 이미지 제거 관련 ---
    fun onRemoveProjectImageClicked() {
        _uiState.update { it.copy(showRemoveImageDialog = true) }
    }

    fun dismissRemoveImageDialog() {
        _uiState.update { it.copy(showRemoveImageDialog = false) }
    }

    fun confirmRemoveProjectImage() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    error = null,
                    showRemoveImageDialog = false
                ) 
            }

            try {
                val result = projectAssetsUseCases.removeProjectProfileImageUseCase(projectId)
                when (result) {
                    is CustomResult.Success -> {
                        _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 이미지가 제거되었습니다"))
                        
                        // 전역 이벤트 발생으로 모든 화면들에 알림
                        projectImageUpdateEventManager.notifyProjectImageUpdated(projectId.value)
                        
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                selectedImageUri = null,
                                hasImageChanges = false,
                                projectImageUrl = null
                            ) 
                        }
                        
                        // 프로젝트 정보 다시 로드하여 UI 갱신
                        loadProjectStructure()
                    }
                    is CustomResult.Failure -> {
                        _uiState.update { it.copy(isLoading = false) }
                        val errorMessage = getHumanReadableErrorMessage(result.error)
                        _eventFlow.emit(ProjectSettingEvent.ShowSnackbar(errorMessage))
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 이미지 제거 중 알 수 없는 오류가 발생했습니다."))
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "프로젝트 이미지 제거 중 오류가 발생했습니다",
                        isLoading = false
                    )
                }
                _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("프로젝트 이미지 제거 실패: ${e.message}"))
            }
        }
    }

    /**
     * 기본 프로젝트 프로필 사용 버튼 클릭 이벤트 처리 (프로젝트 이미지 제거)
     */
    fun onSetDefaultProjectProfileClicked() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isRemovingImage = true, 
                    error = null
                ) 
            }

            try {
                val result = projectAssetsUseCases.removeProjectProfileImageUseCase(projectId)
                when (result) {
                    is CustomResult.Success -> {
                        _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("기본 프로젝트 프로필로 설정되었습니다"))
                        
                        // 전역 이벤트 발생으로 모든 화면들에 알림
                        projectImageUpdateEventManager.notifyProjectImageUpdated(projectId.value)
                        
                        _uiState.update { 
                            it.copy(
                                isRemovingImage = false,
                                selectedImageUri = null,
                                hasImageChanges = false,
                                projectImageUrl = null
                            ) 
                        }
                        
                        // 프로젝트 정보 다시 로드하여 UI 갱신
                        loadProjectStructure()
                    }
                    is CustomResult.Failure -> {
                        _uiState.update { it.copy(isRemovingImage = false) }
                        val errorMessage = getHumanReadableErrorMessage(result.error)
                        _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("기본 프로젝트 프로필 설정 실패: $errorMessage"))
                    }
                    else -> {
                        _uiState.update { it.copy(isRemovingImage = false) }
                        _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("기본 프로젝트 프로필 설정 중 알 수 없는 오류가 발생했습니다."))
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "기본 프로젝트 프로필 설정 중 오류가 발생했습니다",
                        isRemovingImage = false
                    )
                }
                _eventFlow.emit(ProjectSettingEvent.ShowSnackbar("기본 프로젝트 프로필 설정 실패: ${e.message}"))
            }
        }
    }

    // UI 가 직접 뒤로가기를 요청할 때 호출
    fun navigateBack() {
        navigationManger.navigateBack()
    }
}