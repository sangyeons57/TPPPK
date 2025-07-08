package com.example.feature_home.viewmodel

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.constants.Constants
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialogBuilder
import com.example.core_ui.components.bottom_sheet_dialog.BottomSheetDialogItem
import com.example.domain.model.base.Category
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.base.Project
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.ImageUrl
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.user.UserName
import com.example.domain.model.vo.project.ProjectStatus
import com.example.domain.provider.dm.DMUseCaseProvider
import com.example.domain.provider.dm.DMUseCases
import com.example.domain.provider.project.CoreProjectUseCaseProvider
import com.example.domain.provider.project.CoreProjectUseCases
import com.example.domain.provider.project.ProjectStructureUseCaseProvider
import com.example.domain.provider.project.ProjectStructureUseCases
import com.example.domain.provider.user.UserUseCaseProvider
import com.example.feature_home.model.CategoryUiModel
import com.example.feature_home.model.ChannelUiModel
import com.example.feature_home.model.DmUiModel
import com.example.feature_home.model.ProjectStructureUiState
import com.example.feature_home.model.ProjectUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// 상단 탭 상태
enum class TopSection {
    PROJECTS, DMS
}


// 홈 화면 UI 상태
data class HomeUiState(
    val selectedTopSection: TopSection = TopSection.DMS, // 기본 선택: DMS
    val projects: List<ProjectUiModel> = emptyList(), // Now using ProjectUiModel
    val dms: List<DmUiModel> = emptyList(), // Use DmUiModel
    val isLoading: Boolean = false,
    val errorMessage: String = "default",
    
    // 프로젝트 관련 상태
    val selectedProjectId: DocumentId? = null, // 선택된 프로젝트 ID
    val selectedDmId: String? = null, // 선택된 프로젝트 ID
    val projectName: String = "", // 선택된 프로젝트 이름
    val projectDescription: String? = null, // 선택된 프로젝트 설명
    val projectMembers: List<ProjectMember> = emptyList(), // 선택된 프로젝트 멤버
    
    // 카테고리 및 채널 관련 상태
    val projectStructure: ProjectStructureUiState = ProjectStructureUiState(),

    // 사용자 프로필 관련 상태
    val userInitial: String = "U", // 사용자 이니셜 (기본값: "U")
    val userProfileImageUrl: String? = null, // 사용자 프로필 이미지 URL
    
    // 표시 관련 상태
    val isDetailFullScreen: Boolean = false, // 전체 화면 모드 플래그

    // --- Bottom-sheet (long-press) dialog state ---
    val showBottomSheet: Boolean = false,
    val showBottomSheetItems: List<BottomSheetDialogItem> = emptyList(),

    val targetDMChannelForSheet: DmUiModel? = null,
    val targetCategoryForSheet: CategoryUiModel? = null,
    val targetChannelForSheet: ChannelUiModel? = null
)

// 프로젝트 멤버 정보
data class ProjectMember(
    val id: String,
    val name: String,
    val role: String
)

// 홈 화면 이벤트
sealed class HomeEvent {
    data class NavigateToProjectSettings(val projectId: DocumentId?) : HomeEvent() // 프로젝트 설정 화면
    data class NavigateToDmChat(val dmId: DocumentId) : HomeEvent()
    data class NavigateToChannel(val projectId: DocumentId, val channelId: String) :
        HomeEvent() // 채널 화면으로 이동
    object ShowAddProjectDialog : HomeEvent() // 또는 화면 이동
    object ShowAddFriendDialog : HomeEvent() // 또는 화면 이동
    object NavigateToAddProject : HomeEvent() // 프로젝트 추가 화면
    data class ShowSnackbar(val message: String) : HomeEvent()
    data class ShowAddProjectElementDialog(val projectId: DocumentId) :
        HomeEvent() // 프로젝트 구조 편집 다이얼로그 표시

    // --- New navigation events for long-press actions ---
    data class NavigateToEditCategory(val projectId: DocumentId, val categoryId: DocumentId) :
        HomeEvent()

    data class NavigateToEditChannel(
        val projectId: DocumentId,
        val categoryId: String,
        val channelId: String
    ) : HomeEvent()
    data class NavigateToReorderCategory(val projectId: String) : HomeEvent()
    data class NavigateToReorderChannel(val projectId: String, val categoryId: String) : HomeEvent()
    
    // --- Project deletion event ---
    data class ProjectDeleted(val projectId: DocumentId, val projectName: String) : HomeEvent()
}


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val coreProjectUseCaseProvider: CoreProjectUseCaseProvider,
    private val projectStructureUseCaseProvider: ProjectStructureUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider,
    private val dmUseCaseProvider: DMUseCaseProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<HomeEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // 카테고리 확장 상태 캐시 (프로젝트 ID -> 카테고리 ID -> 확장 상태)
    private val categoryExpandedStates = mutableMapOf<String, MutableMap<String, Boolean>>()
    
    // 선택된 채널 ID
    private var selectedChannelId: DocumentId? = null
    
    // Job management for Flow collectors to prevent memory leaks
    private var userStreamJob: Job? = null
    private var projectsStreamJob: Job? = null
    private var dmsStreamJob: Job? = null
    private var projectDetailsJob: Job? = null
    private var projectStructureJob: Job? = null
    
    // Provider를 통해 생성된 UseCase 그룹들
    private val userUseCases = userUseCaseProvider.createForUser()
    private lateinit var dmUseCases: DMUseCases
    private lateinit var coreProjectUseCases: CoreProjectUseCases
    private lateinit var projectStructureUseCases: ProjectStructureUseCases
    
    // 현재 사용자 ID
    private var currentUserId: UserId = UserId.EMPTY
        set(value) {
            Log.d("HomeViewModel", "Setting currentUserId: $value (previous: $field)")
            field = value
            if (value.isNotBlank()) {
                initializeUserSpecificUseCases(value)
                loadDataForUser()
            }
        }

    private fun initializeUserSpecificUseCases(userId: UserId) {
        dmUseCases = dmUseCaseProvider.createForUser(userId)
        coreProjectUseCases = coreProjectUseCaseProvider.createForCurrentUser()
    }

    init {
        Log.d("HomeViewModel", "HomeViewModel initialized")
        startUserStream()
    }
    
    private fun startUserStream() {
        userStreamJob?.cancel()
        userStreamJob = viewModelScope.launch {
            Log.d("HomeViewModel", "Starting to collect from getCurrentUserStreamUseCase")
            // Provider를 통한 UseCase 사용
            userUseCases.getCurrentUserStreamUseCase()
                .catch { exception ->
                    Log.e("HomeViewModel", "Failed to get current user", exception)
                    _uiState.update { it.copy(isLoading = false, errorMessage = "사용자 정보 로드 실패: ${exception.localizedMessage}") }
                }
                .collectLatest { result : CustomResult<User, Exception> ->
                    result.fold(
                        onSuccess = { user ->
                            Log.d("HomeViewModel", "User received from UseCase: ${user.id}")
                            currentUserId = UserId.from(user.id)

                            // 사용자 이니셜과 프로필 이미지 URL 업데이트
                            _uiState.update { state ->
                                state.copy(
                                    userInitial = user.name.value.firstOrNull()?.toString() ?: "U",
                                    userProfileImageUrl = user.profileImageUrl?.value
                                )
                            }
                        },
                        onFailure = { exception ->
                            Log.e("HomeViewModel", "Failed to get current user", exception)
                        },
                    )
                }
        }
    }

    /**
     * 현재 사용자에 대한 모든 데이터를 로드합니다.
     */
    private fun loadDataForUser() {
        Log.d("HomeViewModel", "loadDataForUser called with userId: $currentUserId")
        loadProjects()
        loadDms()
    }

    // Mapper function DMWrapper -> DmUiModel
    // DMWrapper 객체에서 DmUiModel에 필요한 정보를 직접 추출합니다.
    private fun toDmUiModel(dmWrapper: DMWrapper): DmUiModel {
        return DmUiModel(
            channelId = dmWrapper.id,
            partnerName = dmWrapper.otherUserName,
            partnerProfileImageUrl = dmWrapper.otherUserImageUrl,
        )
    }

    // 상단 탭 선택 시 호출
    fun onTopSectionSelect(section: TopSection) {
        if (_uiState.value.selectedTopSection == section && !_uiState.value.isLoading) return // 로딩 중이 아닐 때만 중복 선택 무시
        _uiState.update { it.copy(selectedTopSection = section, isLoading = true, errorMessage = "default") }
        loadDataForSelectedSection()
    }

    // 현재 선택된 탭에 맞는 데이터 로드 함수
    private fun loadDataForSelectedSection() {
        if (_uiState.value.selectedTopSection == TopSection.PROJECTS) {
            loadProjects() // 프로젝트는 currentUserId와 무관하게 로드 가능

        } else if (_uiState.value.selectedTopSection == TopSection.DMS) {
            loadDms() // currentUserId 체크는 이제 usecase 내부에서 처리
        }
    }

    // 프로젝트 데이터 로드
    private fun loadProjects() {
        Log.d("HomeViewModel", "loadProjects called")
        projectsStreamJob?.cancel()
        projectsStreamJob = viewModelScope.launch {
            try {
                if (::coreProjectUseCases.isInitialized) {
                    coreProjectUseCases.getUserParticipatingProjectsUseCase().collectLatest { result ->
                        when (result) {
                            is CustomResult.Loading -> {
                                _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
                            }
                            is CustomResult.Success -> {
                                val projectWrappers = result.data
                                // Assuming com.example.feature_main.ui.toProjectUiModel extension function exists or will be created
                                val mappedProjectUiModels = projectWrappers.map { it.toProjectUiModel() }
                                _uiState.update { state ->
                                    state.copy(
                                        projects = mappedProjectUiModels, // Update with mapped models
                                        isLoading = false,
                                        errorMessage = if (mappedProjectUiModels.isEmpty()) "프로젝트가 없습니다." else "default"
                                    )
                                }
                                Log.d("HomeViewModel", "Projects loaded: ${mappedProjectUiModels.size}")
                            }
                            is CustomResult.Failure -> {
                                // 권한 에러가 아닌 경우만 에러로 표시
                                val errorMessage = result.error.message
                                val isPermissionError = errorMessage?.contains("permission", ignoreCase = true) == true ||
                                        errorMessage?.contains("PERMISSION_DENIED", ignoreCase = true) == true

                                if (isPermissionError) {
                                    Log.w("HomeViewModel", "Permission error in project loading - user likely logged out, clearing projects")
                                    _uiState.update {
                                        it.copy(
                                            projects = emptyList(),
                                            isLoading = false,
                                            errorMessage = "default"
                                        )
                                    }
                                } else {
                                    Log.e("HomeViewModel", "Failed to load projects", result.error)
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            errorMessage = "프로젝트를 불러올 수 없습니다."
                                        )
                                    }
                                }
                            }
                            is CustomResult.Initial -> {
                                // Optionally handle Initial state, e.g., by showing loading
                                _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
                            }
                            is CustomResult.Progress -> {
                                // Handle progress if applicable, e.g. update a progress bar
                                // For now, we can treat it as loading
                                val progressValue = result.progress
                                Log.d("HomeViewModel", "Project loading progress: $progressValue%")
                                _uiState.update { it.copy(isLoading = true) } // Keep isLoading true during progress
                            }
                        }
                    }
                } else {
                    Log.w("HomeViewModel", "coreProjectUseCases not initialized yet")
                    _uiState.update {
                        it.copy(
                            projects = emptyList(),
                            isLoading = false,
                            errorMessage = "default"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Unexpected error in loadProjects", e)
                _uiState.update {
                    it.copy(
                        projects = emptyList(),
                        isLoading = false,
                        errorMessage = "default"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadDms() {
        dmsStreamJob?.cancel()
        dmsStreamJob = viewModelScope.launch {
            try {
                val currentUserResult = userUseCases.getCurrentUserStreamUseCase().first()
                when (currentUserResult) {
                    is CustomResult.Failure -> {
                        Log.w("HomeViewModel", "User not authenticated in loadDms - clearing DMs")
                        _uiState.update {
                            it.copy(
                                dms = emptyList(),
                                isLoading = false,
                                errorMessage = "default"
                            )
                        }
                        return@launch
                    }

                    is CustomResult.Success -> {
                        val currentUser = currentUserResult.data
                        if (currentUser.id.value.isEmpty()) {
                            Log.w("HomeViewModel", "Current user ID is empty - clearing DMs")
                            _uiState.update {
                                it.copy(
                                    dms = emptyList(),
                                    isLoading = false,
                                    errorMessage = "default"
                                )
                            }
                            return@launch
                        }

                        if (::dmUseCases.isInitialized) {
                            // 새로운 GetUserDmWrappersUseCase를 사용하여 실시간 업데이트 구현
                            dmUseCases.getUserDmWrappersUseCase()
                                .collectLatest { result: CustomResult<List<DMWrapper>, Exception> ->
                                    Log.d("HomeViewModel", "Received DM wrappers result: $result")
                                    when (result) {
                                        is CustomResult.Loading -> {
                                            _uiState.update {
                                                it.copy(
                                                    isLoading = true,
                                                    errorMessage = "default"
                                                )
                                            }
                                        }

                                        is CustomResult.Success -> {
                                            Log.d(
                                                "HomeViewModel",
                                                "Successfully fetched DM wrappers: ${result.data.size} wrappers."
                                            )
                                            // DMWrapper에서 DmUiModel로 직접 변환 (실시간 업데이트)
                                            val dmUiModels = result.data.map { dmWrapper ->
                                                toDmUiModel(dmWrapper)
                                            }
                                            _uiState.update { state ->
                                                state.copy(
                                                    dms = dmUiModels,
                                                    isLoading = false,
                                                    errorMessage = if (dmUiModels.isEmpty()) "DM이 없습니다." else "default"
                                                )
                                            }
                                            Log.d(
                                                "HomeViewModel",
                                                "DMs loaded and UI updated (real-time): ${dmUiModels.size}"
                                            )
                                        }

                                        is CustomResult.Failure -> {
                                            // 권한 에러가 아닌 경우만 에러로 표시
                                            val errorMessage = result.error.message
                                            val isPermissionError = errorMessage?.contains("permission", ignoreCase = true) == true ||
                                                    errorMessage?.contains("PERMISSION_DENIED", ignoreCase = true) == true

                                            if (isPermissionError) {
                                                Log.w("HomeViewModel", "Permission error in DM loading - user likely logged out, clearing DMs")
                                                _uiState.update {
                                                    it.copy(
                                                        dms = emptyList(),
                                                        isLoading = false,
                                                        errorMessage = "default"
                                                    )
                                                }
                                            } else {
                                                Log.e("HomeViewModel", "Failed to load DMs", result.error)
                                                _uiState.update {
                                                    it.copy(
                                                        isLoading = false,
                                                        errorMessage = "DM을 불러올 수 없습니다."
                                                    )
                                                }
                                            }
                                        }

                                        is CustomResult.Initial -> {
                                            _uiState.update {
                                                it.copy(
                                                    isLoading = true,
                                                    errorMessage = "default"
                                                )
                                            }
                                            Log.d("HomeViewModel", "DM loading initial state.")
                                        }

                                        is CustomResult.Progress -> {
                                            val progressValue = result.progress
                                            Log.d(
                                                "HomeViewModel",
                                                "DM loading progress: $progressValue%"
                                            )
                                            _uiState.update { it.copy(isLoading = true) }
                                        }
                                    }
                                }
                        } else {
                            Log.w("HomeViewModel", "dmUseCases not initialized yet")
                            _uiState.update {
                                it.copy(
                                    dms = emptyList(),
                                    isLoading = false,
                                    errorMessage = "default"
                                )
                            }
                        }
                    }

                    else -> {
                        Log.w("HomeViewModel", "Unexpected result type in loadDms: $currentUserResult")
                        _uiState.update {
                            it.copy(
                                dms = emptyList(),
                                isLoading = false,
                                errorMessage = "default"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Unexpected error in loadDms", e)
                _uiState.update {
                    it.copy(
                        dms = emptyList(),
                        isLoading = false,
                        errorMessage = "default"
                    )
                }
            }
        }
    }

    // 프로젝트 아이템 클릭 시
    fun onProjectClick(projectId: DocumentId) {
        Log.d("HomeViewModel", "=== onProjectClick START === projectId=${projectId.value}")
        
        viewModelScope.launch {
            // 프로젝트 ID가 이미 선택되어 있으면 무시
            if (_uiState.value.selectedProjectId == projectId) {
                Log.d("HomeViewModel", "Project already selected, returning")
                return@launch
            }
            
            Log.d("HomeViewModel", "onProjectClick called for projectId: $projectId")
            
            // 먼저 프로젝트가 삭제되었는지 실시간으로 확인
            if (::coreProjectUseCases.isInitialized) {
                try {
                    // 스냅샷 리스너를 통해 실시간으로 프로젝트 상태 확인
                    coreProjectUseCases.getProjectDetailsStreamUseCase(projectId)
                        .filter { it !is CustomResult.Loading && it !is CustomResult.Initial }
                        .take(1) // Loading/Initial 제외하고 첫 번째 실제 값만 받음
                        .collect { result ->
                            when (result) {
                                is CustomResult.Success -> {
                                    val project = result.data
                                    Log.d("HomeViewModel", "Project found: ${project.name.value}, status: ${project.status}")
                                    
                                    // 프로젝트 상태 확인 - DELETED 상태인 경우 처리
                                    if (project.status == ProjectStatus.DELETED) {
                                        Log.w("HomeViewModel", "Project $projectId is marked as DELETED")
                                        removeDeletedProject(projectId, project.name.value)
                                        return@collect
                                    }
                                    
                                    // 정상 프로젝트인 경우 기존 로직 실행
                                    proceedWithProjectSelection(projectId)
                                }
                                is CustomResult.Failure -> {
                                    // 프로젝트 로딩 실패 - 삭제된 프로젝트일 가능성
                                    Log.w("HomeViewModel", "Failed to load project details for $projectId: ${result.error.message}")
                                    
                                    // 프로젝트 이름 찾기 (UI 상태에서)
                                    val projectName = _uiState.value.projects.find { it.id == projectId }?.name?.value ?: "알 수 없는 프로젝트"
                                    
                                    // 특정 에러 메시지들은 삭제된 프로젝트로 간주
                                    val errorMessage = result.error.message?.lowercase() ?: ""
                                    if (errorMessage.contains("not found") || 
                                        errorMessage.contains("permission") || 
                                        errorMessage.contains("failed to deserialize")) {
                                        Log.w("HomeViewModel", "Project appears to be deleted, removing wrapper")
                                        removeDeletedProject(projectId, projectName)
                                    } else {
                                        // 다른 에러의 경우 기본 동작 수행
                                        proceedWithProjectSelection(projectId)
                                    }
                                }
                                else -> {
                                    Log.d("HomeViewModel", "Unexpected result type: $result")
                                    // 예상치 못한 상태의 경우 기본 동작 수행
                                    proceedWithProjectSelection(projectId)
                                }
                            }
                        }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error checking project details for $projectId", e)
                    
                    // 예외 발생 시 기본 동작 수행
                    proceedWithProjectSelection(projectId)
                }
            } else {
                Log.w("HomeViewModel", "coreProjectUseCases not initialized yet")
                // UseCases가 초기화되지 않은 경우 기본 동작 수행
                proceedWithProjectSelection(projectId)
            }
        }
    }
    
    // 유효한 프로젝트 선택 시 기존 로직 실행
    private fun proceedWithProjectSelection(projectId: DocumentId) {
        Log.d("HomeViewModel", "Proceeding with project selection: ${projectId.value}")
        
        // 상태 업데이트
        _uiState.update { it.copy(
            selectedProjectId = projectId,
            selectedTopSection = TopSection.PROJECTS, // 프로젝트 탭으로 전환
            isLoading = true, // 로딩 상태로 설정
            projectStructure = ProjectStructureUiState(isLoading = true) // 프로젝트 구조 로딩 상태로 설정
        )}
        
        // 프로젝트 상세 정보 로드
        loadProjectDetails(projectId)
        
        // 프로젝트 구조 (카테고리 및 채널) 로드
        loadProjectStructure(projectId)
    }

    // 프로젝트 상세 정보 로드
    private fun loadProjectDetails(projectId: DocumentId) {
        viewModelScope.launch {
            Log.d("HomeViewModel", "loadProjectDetails called for projectId: $projectId")
            if (::coreProjectUseCases.isInitialized) {
                coreProjectUseCases.getProjectDetailsStreamUseCase(projectId)
                    .collectLatest { result: CustomResult<Project, Exception> ->
                    Log.d("HomeViewModel", "Received project details result: $result")
                    when (result) {
                        is CustomResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
                        }
                        is CustomResult.Success -> {
                            val project = result.data
                            _uiState.update { state ->
                                state.copy(
                                    projectName = project.name.value,
                                    projectDescription = "Project description placeholder for ${project.name.value}",
                                    isLoading = false,
                                    errorMessage = "default"
                                )
                            }
                            Log.d("HomeViewModel", "Project details loaded: ${project.name.value}")
                        }
                        is CustomResult.Failure -> {
                            Log.e("HomeViewModel", "Failed to load project details for $projectId", result.error)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = result.error.message ?: "프로젝트 상세 정보를 가져오지 못했습니다."
                                )
                            }
                        }
                        is CustomResult.Initial -> {
                            _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
                            Log.d("HomeViewModel", "Project details loading initial state.")
                        }
                        is CustomResult.Progress -> {
                            val progressValue = result.progress ?: 0f
                            Log.d("HomeViewModel", "Project details loading progress: $progressValue%")
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } else {
                Log.w("HomeViewModel", "coreProjectUseCases not initialized yet")
            }
        }
    }
    
    // 프로젝트 구조 (카테고리 및 채널) 로드
    private fun loadProjectStructure(projectId: DocumentId) {
        projectStructureUseCases = projectStructureUseCaseProvider.createForCurrentUser(projectId = projectId)
        viewModelScope.launch {
            Log.d("HomeViewModel", "loadProjectStructure called for projectId: $projectId")
            if (::projectStructureUseCases.isInitialized) {
                val projectStructureFlow =
                    projectStructureUseCases.getProjectAllCategoriesUseCase(projectId)

                projectStructureFlow.collectLatest { result: CustomResult<List<Category>, Exception> ->
                    Log.d("HomeViewModel", "Received project structure result: $result")
                    when (result) {
                        is CustomResult.Loading -> {
                            _uiState.update { state ->
                                state.copy(
                                    projectStructure = state.projectStructure.copy(
                                        isLoading = true,
                                        error = "default"
                                    )
                                )
                            }
                        }

                        is CustomResult.Success -> {
                            val categories = result.data
                            val categoriesMap =
                                categoryExpandedStates.getOrPut(projectId.value) { mutableMapOf() }

                            // Filter categories that are actual categories (not special "no category")
                            val categoryUiModels =
                                categories.filter { category -> category.isCategory.value }
                                    .map { categoryDomain ->
                                        val isExpanded =
                                            categoriesMap.getOrPut(categoryDomain.id.value) { true } // Default to expanded

                                        CategoryUiModel(
                                            id = categoryDomain.id,
                                            name = categoryDomain.name,
                                            channels = emptyList(), // TODO: Load channels for each category
                                            isExpanded = isExpanded
                                        )
                                    }

                            // TODO: Load direct channels (no category)
                            val directChannelUiModels = emptyList<ChannelUiModel>()

                            _uiState.update { state ->
                                state.copy(
                                    projectStructure = ProjectStructureUiState(
                                        categories = categoryUiModels,
                                        directChannel = directChannelUiModels,
                                        isLoading = false,
                                        error = "default"
                                    )
                                )
                            }
                            Log.d("HomeViewModel", "Project structure loaded for $projectId")
                        }

                        is CustomResult.Failure -> {
                            Log.e(
                                "HomeViewModel",
                                "Failed to load project structure for $projectId",
                                result.error
                            )
                            _uiState.update { state ->
                                state.copy(
                                    projectStructure = state.projectStructure.copy(
                                        isLoading = false,
                                        error = result.error.message ?: "프로젝트 구조를 가져오지 못했습니다."
                                    )
                                )
                            }
                        }

                        is CustomResult.Initial -> {
                            _uiState.update { state ->
                                state.copy(
                                    projectStructure = state.projectStructure.copy(
                                        isLoading = true,
                                        error = "default"
                                    )
                                )
                            }
                            Log.d("HomeViewModel", "Project structure loading initial state.")
                        }

                        is CustomResult.Progress -> {
                            val progressValue = result.progress
                            Log.d(
                                "HomeViewModel",
                                "Project structure loading progress: $progressValue%"
                            )
                            _uiState.update { state ->
                                state.copy(projectStructure = state.projectStructure.copy(isLoading = true))
                            }
                        }
                    }
                }
            } else {
                Log.w("HomeViewModel", "projectStructureUseCases not initialized yet")
            }
        }
    }

    // 카테고리 클릭 시 (접기/펼치기)
    fun onCategoryClick(category: CategoryUiModel) {
        val projectId = _uiState.value.selectedProjectId ?: return
        
        // 카테고리 확장 상태 토글
        categoryExpandedStates.getOrPut(projectId.value) { mutableMapOf() }[category.id.value] =
            !category.isExpanded
        
        // 프로젝트 구조 UI 상태 업데이트
        _uiState.update { state ->
            val updatedCategories = state.projectStructure.categories.map { cat ->
                if (cat.id == category.id) {
                    cat.copy(isExpanded = !cat.isExpanded)
                } else {
                    cat
                }
            }
            
            // 내부 categoryExpandedStates 캐시도 업데이트
            val categoryMap = categoryExpandedStates.getOrPut(projectId.value) { mutableMapOf() }
            updatedCategories.forEach { category ->
                categoryMap[category.id.value] = category.isExpanded
            }

            state.copy(
                projectStructure = state.projectStructure.copy(categories = updatedCategories)
            )
        }
    }
    
    // 채널 클릭 시
    fun onChannelClick(channel: ChannelUiModel) {
        val projectId = _uiState.value.selectedProjectId ?: return
        
        // 이미 선택된 채널이면 무시
        if (selectedChannelId == channel.id) return
        
        // 선택된 채널 ID 업데이트
        selectedChannelId = channel.id
        
        // 프로젝트 구조 UI 상태 업데이트 (선택된 채널 하이라이트)
        _uiState.update { state ->
            val updatedCategories = state.projectStructure.categories.map { category ->
                val updatedChannels = category.channels.map { ch ->
                    ch.copy(isSelected = ch.id == channel.id)
                }
                category.copy(channels = updatedChannels)
            }
            
            val updatedGeneralChannels = state.projectStructure.directChannel.map { ch ->
                ch.copy(isSelected = ch.id == channel.id)
            }
            
            state.copy(
                projectStructure = state.projectStructure.copy(
                    categories = updatedCategories,
                    directChannel = updatedGeneralChannels
                )
            )
        }
        
        // 채널 화면으로 이동 이벤트 발행
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.NavigateToChannel(projectId, channel.id.value))
        }
    }

    // 프로젝트 설정 아이콘 클릭 시 (새로 추가)
    fun onProjectSettingsClick(projectId: DocumentId) {
        viewModelScope.launch {
            // 설정은 별도 화면으로 네비게이션
            _eventFlow.emit(HomeEvent.NavigateToProjectSettings(projectId))
        }
    }

    // DM 아이템 클릭 시
    fun onDmClick(dmId: String) {
        viewModelScope.launch {
            when (_uiState.value.selectedTopSection) {
                TopSection.PROJECTS -> {
                    println("ViewModel: 프로젝트 추가 버튼 클릭 -> 화면 이동 요청")
                    _eventFlow.emit(HomeEvent.NavigateToAddProject) // 수정: 화면 이동 이벤트 발생
                }
                TopSection.DMS -> {
                    println("ViewModel: 친구 추가/DM 버튼 클릭")
                    _eventFlow.emit(HomeEvent.ShowAddFriendDialog)
                }
            }
        }
    }

    // FAB 클릭 시
    fun onAddButtonClick() {
        viewModelScope.launch {
            when (_uiState.value.selectedTopSection) {
                TopSection.PROJECTS -> {
                    println("ViewModel: 프로젝트 추가 버튼 클릭")
                    _eventFlow.emit(HomeEvent.ShowAddProjectDialog) // 또는 화면 이동 이벤트
                }
                TopSection.DMS -> {
                    println("ViewModel: 친구 추가/DM 버튼 클릭")
                    _eventFlow.emit(HomeEvent.ShowAddFriendDialog) // 또는 화면 이동 이벤트
                }
            }
        }
    }

    // 프로젝트 추가 버튼 클릭 시
    fun onProjectAddButtonClick() {
        viewModelScope.launch {
            println("ViewModel: 프로젝트 추가 버튼 클릭")
            _eventFlow.emit(HomeEvent.NavigateToAddProject) // 또는 화면 이동 이벤트
        }
    }
    
    fun onAddFriendClick() {
        viewModelScope.launch {
            println("ViewModel: 친구 추가 버튼 클릭")
            _eventFlow.emit(HomeEvent.ShowAddFriendDialog) // 또는 화면 이동 이벤트
        }
    }

    /**
     * 프로젝트 요소(카테고리/채널) 추가 버튼 클릭 시 호출됩니다.
     * 네비게이션 이벤트를 발생시켜 AddProjectElementDialog를 엽니다.
     *
     * @param projectId 현재 프로젝트의 ID.
     */
    fun onAddProjectElement(projectId: DocumentId) {
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.ShowAddProjectElementDialog(projectId))
        }
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = "default") }
    }
    
    // 전체 화면 표시 모드 토글
    fun toggleDetailDisplayMode() {
        _uiState.update { it.copy(isDetailFullScreen = !it.isDetailFullScreen) }
    }
    
    /**
     * 스낵바를 통해 메시지를 표시합니다.
     * @param message 표시할 메시지
     */
    fun showSnackbar(message: String) {
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.ShowSnackbar(message))
        }
    }
    
    /**
     * 프로젝트 구조를 새로고침합니다.
     * @param projectId 새로고침할 프로젝트 ID
     */
    fun refreshProjectStructure(projectId: DocumentId) {
        viewModelScope.launch {
            loadProjectStructure(projectId)
        }
    }
    
    // Domain 모델을 UI 모델로 변환하는 확장 함수
    private fun Project.toProjectUiModel(): ProjectUiModel {
        Log.d(
            "HomeViewModel",
            "Mapping Project domain to ProjectUiModel: id=${this.id.value}, name=${this.name.value}"
        )
        return ProjectUiModel(
            id = this.id,
            name = this.name,
            imageUrl = this.imageUrl,
        )
    }

    private fun formatTimestamp(timestamp: Instant): String {
        val localDateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        val now = LocalDateTime.now(ZoneId.systemDefault())
        return when {
            ChronoUnit.MINUTES.between(localDateTime, now) < 1 -> "방금 전"
            ChronoUnit.HOURS.between(localDateTime, now) < 1 -> "${ChronoUnit.MINUTES.between(localDateTime, now)}분 전"
            ChronoUnit.DAYS.between(localDateTime, now) < 1 -> localDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            ChronoUnit.DAYS.between(localDateTime, now) == 1L -> "어제"
            else -> localDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
        }
    }

    // DM 아이템 클릭 시 이벤트 발생
    fun onDmItemClick(dmUiModel: DmUiModel) {
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.NavigateToDmChat(dmUiModel.channelId))
        }
    }

    /**
     * 저장된 상태로부터 확장된 카테고리 목록을 복원합니다.
     * 화면 전환 시 이전 상태 복원에 사용됩니다.
     *
     * @param expandedCategoryIdsList 복원할 확장된 카테고리 ID 목록
     */
    fun restoreExpandedCategories(expandedCategoryIdsList: List<String>) {
        Log.d("HomeViewModel", "Restoring expanded categories: $expandedCategoryIdsList")
        val projectId = _uiState.value.selectedProjectId
        if (projectId != null) {
            _uiState.update { currentState ->
                val currentProjectStructure = currentState.projectStructure
                val updatedCategories = currentProjectStructure.categories.map { category ->
                    // category.id가 expandedCategoryIdsList에 포함되어 있으면 isExpanded를 true로 설정
                    category.copy(isExpanded = category.id.value in expandedCategoryIdsList)
                }
                
                // 내부 categoryExpandedStates 캐시도 업데이트
                val categoryMap =
                    categoryExpandedStates.getOrPut(projectId.value) { mutableMapOf() }
                updatedCategories.forEach { category ->
                    categoryMap[category.id.value] = category.isExpanded
                }

                currentState.copy(
                    projectStructure = currentProjectStructure.copy(categories = updatedCategories)
                )
            }
        }
    }
    
    /**
     * 에러 메시지를 UI에 표시합니다.
     *
     * @param message 표시할 에러 메시지
     */
    fun showErrorMessage(message: String) {
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.ShowSnackbar(message))
        }
    }

    fun onProjectSettingsClicked() {
        val currentProjectId = _uiState.value.selectedProjectId
        if (currentProjectId != null) {
            viewModelScope.launch {
                _eventFlow.emit(HomeEvent.NavigateToProjectSettings(currentProjectId))
            }
        } else {
            Log.w("HomeViewModel", "Project settings clicked but no project is selected.")
            // Optionally, show a snackbar message to the user
            // viewModelScope.launch { _eventFlow.emit(HomeEvent.ShowSnackbar("선택된 프로젝트가 없습니다.")) }
        }
        onProjectItemActionSheetDismiss() // Dismiss the bottom sheet
    }

    fun onClickTopSection(){
        Log.d("HomeViewModel", "onClickTopSection called")
        _uiState.update {
            if (uiState.value.selectedTopSection == TopSection.PROJECTS) {
                it.copy(
                    showBottomSheetItems = BottomSheetDialogBuilder()
                        .button(
                            label = "프로젝트 설정",
                            onClick = { onProjectSettingsClicked() },
                            icon = Icons.Filled.Settings
                        )
                        .build(),
                    showBottomSheet = true
                )
            } else {
                it.copy(showBottomSheet = false)
            }
        }
    }
    // ----------------------------
    // Long-press handlers & helpers
    // ----------------------------

    fun onCategoryLongPress(category: CategoryUiModel) {
        _uiState.update {
            it.copy(
                showBottomSheetItems = BottomSheetDialogBuilder()
                    .button(
                        label= "프로젝트 카테고리 편집",
                        icon = Icons.Default.Edit,
                        onClick = { onEditSelectedProjectCategory() }
                    ).build(),
                showBottomSheet = true,

                targetCategoryForSheet = category,
                targetChannelForSheet = null
            )
        }
    }

    fun onChannelLongPress(channel: ChannelUiModel) {
        _uiState.update {
            it.copy(
                showBottomSheetItems = BottomSheetDialogBuilder()
                    .button(
                        label = "프로젝트 체널 편집",
                        icon = Icons.Default.Edit,
                        onClick = { onEditSelectedProjectChannel() }
                    ).build(),
                showBottomSheet = true,

                targetCategoryForSheet = null,
                targetChannelForSheet = channel
            )
        }
    }

    fun onProjectItemActionSheetDismiss() {
        _uiState.update {
            it.copy(
                showBottomSheet = false,

                targetCategoryForSheet = null,
                targetChannelForSheet = null
            )
        }
    }

    fun onEditSelectedProjectChannel() {
        val projectId = _uiState.value.selectedProjectId
        val channel = _uiState.value.targetChannelForSheet

        if (projectId != null && channel != null) {
            val categoryId = getCategoryIdForChannel(channel.id.value)
            if (categoryId != null) {
                viewModelScope.launch {
                    _eventFlow.emit(
                        HomeEvent.NavigateToEditChannel(
                            projectId,
                            categoryId,
                            channel.id.value
                        )
                    )
                }
            } else {
                // Handle case where categoryId is not found for the channel (e.g., direct channel or error)
                Log.w("HomeViewModel", "Category ID not found for channel: ${channel.id.value}")
                // Optionally, show a snackbar message to the user
                // viewModelScope.launch { _eventFlow.emit(HomeEvent.ShowSnackbar("채널의 카테고리를 찾을 수 없습니다.")) }
            }
        }
        onProjectItemActionSheetDismiss()
    }

    fun onEditSelectedProjectCategory() {
        val projectId = _uiState.value.selectedProjectId
        val category = _uiState.value.targetCategoryForSheet

        if (projectId != null && category != null) {
            viewModelScope.launch {
                _eventFlow.emit(HomeEvent.NavigateToEditCategory(projectId, category.id))
            }
        }
        onProjectItemActionSheetDismiss()
    }


    private fun getCategoryIdForChannel(channelId: String): String? {
        val structure = _uiState.value.projectStructure
        structure.categories.forEach { cat ->
            if (cat.channels.any { it.id.value == channelId }) return cat.id.value
        }
        return null // Direct channel or not found
    }
    
    // === NavigationManager 통합 메서드들 ===
    
    fun navigateToProjectDetails(projectId: String) {
        navigationManger.navigateToProjectDetails(projectId)
    }
    
    fun navigateToChat(channelId: String) {
        navigationManger.navigateToChat(channelId)
    }
    
    fun navigateToDmChat(dmChannelId: String) {
        navigationManger.navigateToChat(dmChannelId)
    }
    
    fun navigateToAddProject() {
        navigationManger.navigateToAddProject()
    }
    
    fun navigateToProjectSettings(projectId: String) {
        navigationManger.navigateToProjectSettings(projectId)
    }
    
    fun navigateToProfile() {
        // TODO: User profile navigation 구현 시 추가
        // navigationManger.navigateToProfile()
    }
    
    fun navigateToSettings() {
        // TODO: Settings navigation 구현 시 추가
        // navigationManger.navigateToSettings()
    }
    
    fun navigateToFriends() {
        navigationManger.navigateToFriends()
    }
    
    fun navigateToCalendar(year: Int, month: Int, day: Int) {
        navigationManger.navigateToCalendar(year, month, day)
    }
    
    /**
     * 삭제된 프로젝트를 처리합니다.
     * ProjectWrapper를 제거하고 사용자에게 알림을 표시합니다.
     */
    private fun removeDeletedProject(projectId: DocumentId, projectName: String) {
        viewModelScope.launch {
            Log.d("HomeViewModel", "Removing deleted project: $projectId ($projectName)")
            
            if (::coreProjectUseCases.isInitialized) {
                try {
                    // ProjectWrapper 삭제
                    val deleteResult = coreProjectUseCases.deleteProjectsWrapperUseCase(projectId)
                    
                    when (deleteResult) {
                        is CustomResult.Success -> {
                            Log.d("HomeViewModel", "Successfully removed ProjectWrapper for $projectId")
                            
                            // 프로젝트 삭제 이벤트 발생
                            _eventFlow.emit(HomeEvent.ProjectDeleted(projectId, projectName))
                            
                            // 선택된 프로젝트가 삭제된 프로젝트인 경우 선택 해제
                            if (_uiState.value.selectedProjectId == projectId) {
                                _uiState.update { it.copy(
                                    selectedProjectId = null,
                                    selectedTopSection = TopSection.DMS,
                                    projectName = "",
                                    projectDescription = null,
                                    projectStructure = ProjectStructureUiState()
                                )}
                            }
                        }
                        is CustomResult.Failure -> {
                            Log.e("HomeViewModel", "Failed to remove ProjectWrapper for $projectId", deleteResult.error)
                            _eventFlow.emit(HomeEvent.ShowSnackbar("프로젝트 제거 중 오류가 발생했습니다."))
                        }
                        else -> {
                            Log.w("HomeViewModel", "Unexpected result from deleteProjectsWrapperUseCase: $deleteResult")
                            _eventFlow.emit(HomeEvent.ShowSnackbar("프로젝트 제거 중 문제가 발생했습니다."))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Exception while removing deleted project $projectId", e)
                    _eventFlow.emit(HomeEvent.ShowSnackbar("프로젝트 제거 중 오류가 발생했습니다."))
                }
            } else {
                Log.w("HomeViewModel", "coreProjectUseCases not initialized, cannot remove ProjectWrapper")
                _eventFlow.emit(HomeEvent.ShowSnackbar("잠시 후 다시 시도해주세요."))
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cancel all Flow collection jobs to prevent memory leaks
        userStreamJob?.cancel()
        projectsStreamJob?.cancel()
        dmsStreamJob?.cancel()
        projectDetailsJob?.cancel()
        projectStructureJob?.cancel()
        Log.d("HomeViewModel", "All Flow collection jobs cancelled")
    }
}