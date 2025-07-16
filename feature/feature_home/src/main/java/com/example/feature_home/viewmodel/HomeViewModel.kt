package com.example.feature_home.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.core_navigation.core.NavigationManger
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.feature_home.model.CategoryUiModel
import com.example.feature_home.model.ChannelUiModel
import com.example.feature_home.model.DmUiModel
import com.example.feature_home.model.ProjectStructureItem
import com.example.feature_home.model.ProjectStructureUiState
import com.example.feature_home.model.ProjectUiModel
import com.example.feature_home.viewmodel.service.HomeServiceProvider
import com.example.feature_home.viewmodel.service.HomeServices
import com.example.feature_home.viewmodel.service.DialogManagementService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

/**
 * 리팩토링된 HomeViewModel - Service Provider 패턴 적용
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeServiceProvider: HomeServiceProvider,
    private val navigationManger: NavigationManger
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<HomeEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // Service 그룹들
    private lateinit var services: HomeServices
    
    // 선택된 채널 ID
    private var selectedChannelId: DocumentId? = null
    
    // Job management for Flow collectors to prevent memory leaks
    private var userStreamJob: Job? = null
    private var projectsStreamJob: Job? = null
    private var dmsStreamJob: Job? = null
    private var projectDetailsJob: Job? = null
    private var projectStructureJob: Job? = null
    
    // 다이얼로그 상태
    private lateinit var dialogState: com.example.feature_home.viewmodel.service.DialogManagementService.DialogState
    
    // 현재 사용자 ID
    private var currentUserId: UserId = UserId.EMPTY
        set(value) {
            Log.d("HomeViewModel", "Setting currentUserId: $value")
            field = value
            if (value.isNotBlank()) {
                loadDataForUser()
            }
        }

    init {
        Log.d("HomeViewModel", "HomeViewModel initialized")
        // Initialize services for current user without project context
        services = homeServiceProvider.createForCurrentUser()
        dialogState = services.dialogManagementService.getInitialDialogState()
        // Initialize dialog state after services are created
        dialogState = services.dialogManagementService.getInitialDialogState()
        startUserStream()
    }


    /**
     * 선택 상태를 초기화합니다.
     */
    fun resetSelectionState() {
        Log.d("HomeViewModel", "Resetting selection state")
        _uiState.update { it.copy(
            selectedProjectId = null,
            selectedDmId = null,
            selectedTopSection = TopSection.DMS,
            projectName = "",
            projectDescription = null,
            projectStructure = ProjectStructureUiState()
        )}
    }

    /**
     * 사용자 스트림 시작
     */
    private fun startUserStream() {
        userStreamJob?.cancel()
        userStreamJob = viewModelScope.launch {
            Log.d("HomeViewModel", "Starting user stream")
            services.loadUserDataService.getCurrentUserStream().collectLatest { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val userData = result.data
                        Log.d("HomeViewModel", "User data received: ${userData.userId}")
                        currentUserId = userData.userId
                        
                        _uiState.update { state ->
                            state.copy(
                                userInitial = userData.userInitial,
                                userProfileImageUrl = userData.userProfileImageUrl
                            )
                        }
                    }
                    
                    is CustomResult.Failure -> {
                        Log.e("HomeViewModel", "Failed to get user data", result.error)
                        _uiState.update { it.copy(errorMessage = "사용자 정보 로드 실패") }
                    }
                    
                    else -> {
                        Log.d("HomeViewModel", "User stream loading...")
                    }
                }
            }
        }
    }

    /**
     * 현재 사용자에 대한 모든 데이터를 로드합니다.
     */
    private fun loadDataForUser() {
        Log.d("HomeViewModel", "Loading data for user: $currentUserId")
        loadProjects()
        loadDms()
    }

    /**
     * 상단 탭 선택 시 호출
     */
    fun onTopSectionSelect(section: TopSection) {
        if (_uiState.value.selectedTopSection == section && !_uiState.value.isLoading) return
        
        _uiState.update { it.copy(selectedTopSection = section, isLoading = true, errorMessage = "default") }
        loadDataForSelectedSection()
    }

    /**
     * 현재 선택된 탭에 맞는 데이터 로드
     */
    private fun loadDataForSelectedSection() {
        when (_uiState.value.selectedTopSection) {
            TopSection.PROJECTS -> loadProjects()
            TopSection.DMS -> loadDms()
        }
    }

    /**
     * 프로젝트 데이터 로드
     */
    private fun loadProjects() {
        projectsStreamJob?.cancel()
        projectsStreamJob = viewModelScope.launch {
            Log.d("HomeViewModel", "Loading projects")
            services.loadProjectsService.getUserParticipatingProjectsStream().collectLatest { result ->
                when (result) {
                    is CustomResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
                    }
                    
                    is CustomResult.Success -> {
                        val projects = result.data
                        _uiState.update { state ->
                            state.copy(
                                projects = projects,
                                isLoading = false,
                                errorMessage = if (projects.isEmpty()) "프로젝트가 없습니다." else "default"
                            )
                        }
                        Log.d("HomeViewModel", "Projects loaded: ${projects.size}")
                    }
                    
                    is CustomResult.Failure -> {
                        Log.e("HomeViewModel", "Failed to load projects", result.error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "프로젝트를 불러올 수 없습니다."
                            )
                        }
                    }
                    
                    is CustomResult.Initial -> {
                        Log.d("HomeViewModel", "Initial state")
                    }
                    
                    is CustomResult.Progress -> {
                        Log.d("HomeViewModel", "Progress: ${result.progress}%")
                    }
                }
            }
        }
    }

    /**
     * DM 데이터 로드
     */
    private fun loadDms() {
        dmsStreamJob?.cancel()
        dmsStreamJob = viewModelScope.launch {
            Log.d("HomeViewModel", "Loading DMs")
            services.loadDmsService.getUserDmsStream().collectLatest { result ->
                when (result) {
                    is CustomResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = "default") }
                    }
                    
                    is CustomResult.Success -> {
                        val dms = result.data
                        _uiState.update { state ->
                            state.copy(
                                dms = dms,
                                isLoading = false,
                                errorMessage = if (dms.isEmpty()) "DM이 없습니다." else "default"
                            )
                        }
                        Log.d("HomeViewModel", "DMs loaded: ${dms.size}")
                    }
                    
                    is CustomResult.Failure -> {
                        Log.e("HomeViewModel", "Failed to load DMs", result.error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "DM을 불러올 수 없습니다."
                            )
                        }
                    }
                    
                    is CustomResult.Initial -> {
                        Log.d("HomeViewModel", "Initial state")
                    }
                    
                    is CustomResult.Progress -> {
                        Log.d("HomeViewModel", "Progress: ${result.progress}%")
                    }
                }
            }
        }
    }

    /**
     * 프로젝트 클릭 처리
     */
    fun onProjectClick(projectId: DocumentId) {
        Log.d("HomeViewModel", "Project clicked: $projectId")
        
        if (_uiState.value.selectedProjectId == projectId) return
        
        _uiState.update { it.copy(selectedProjectId = projectId) }
        
        // 프로젝트 선택 시 해당 프로젝트 컨텍스트로 services 재생성
        services = homeServiceProvider.create(projectId, currentUserId)
        
        loadProjectDetails(projectId)
        loadProjectStructure(projectId)
    }

    /**
     * 프로젝트 상세정보 로드
     */
    private fun loadProjectDetails(projectId: DocumentId) {
        projectDetailsJob?.cancel()
        projectDetailsJob = viewModelScope.launch {
            services.projectSelectionService.getProjectDetailsStream(projectId).collectLatest { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val project = result.data
                        _uiState.update { state ->
                            state.copy(
                                projectName = project.name.value,
                                projectDescription = null  // Project model doesn't have description field
                            )
                        }
                    }
                    
                    is CustomResult.Failure -> {
                        Log.e("HomeViewModel", "Failed to load project details", result.error)
                    }
                    
                    else -> {
                        Log.d("HomeViewModel", "Loading project details...")
                    }
                }
            }
        }
    }

    /**
     * 프로젝트 구조 로드
     */
    private fun loadProjectStructure(projectId: DocumentId) {
        projectStructureJob?.cancel()
        projectStructureJob = viewModelScope.launch {
            services.projectSelectionService.getProjectStructureStream(projectId).collectLatest { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val structure = result.data
                        
                        // Apply current expansion states from CategoryManagementService
                        val updatedCategories = structure.categories.map { category ->
                            val isExpanded = services.categoryManagementService.getCategoryExpansionState(projectId, category.id)
                            category.copy(isExpanded = isExpanded)
                        }
                        
                        // Rebuild unified structure items with updated expansion states
                        val updatedUnifiedItems = mutableListOf<com.example.feature_home.model.ProjectStructureItem>()
                        
                        // Add updated categories
                        updatedCategories.forEach { category ->
                            updatedUnifiedItems.add(
                                com.example.feature_home.model.ProjectStructureItem.CategoryItem(
                                    category = category,
                                    globalOrder = category.order
                                )
                            )
                        }
                        
                        // Add direct channels
                        structure.directChannel.forEach { channel ->
                            val globalOrder = channel.order.value
                            updatedUnifiedItems.add(
                                ProjectStructureItem.DirectChannelItem(
                                    channel = channel,
                                    globalOrder = globalOrder
                                )
                            )
                        }
                        
                        val updatedStructure = structure.copy(
                            categories = updatedCategories,
                            unifiedStructureItems = updatedUnifiedItems.sortedBy { it.globalOrder }
                        )
                        
                        _uiState.update { state ->
                            state.copy(projectStructure = updatedStructure)
                        }
                        
                        Log.d("HomeViewModel", "Project structure loaded with ${updatedCategories.size} categories, expansion states preserved")
                    }
                    
                    is CustomResult.Failure -> {
                        Log.e("HomeViewModel", "Failed to load project structure", result.error)
                    }
                    
                    else -> {
                        Log.d("HomeViewModel", "Loading project structure...")
                    }
                }
            }
        }
    }

    /**
     * 카테고리 클릭 처리
     */
    fun onCategoryClick(category: CategoryUiModel) {
        val projectId = _uiState.value.selectedProjectId ?: return
        
        // CategoryManagementService에서 현재 실제 상태를 먼저 확인
        val currentStateInService = services.categoryManagementService.getCategoryExpansionState(projectId, category.id)
        Log.d("HomeViewModel", "Category clicked: ${category.name}, UI shows: ${category.isExpanded}, Service has: $currentStateInService")
        
        val newExpanded = services.categoryManagementService.toggleCategoryExpansion(projectId, category.id)
        
        Log.d("HomeViewModel", "CategoryManagementService toggled: $currentStateInService -> $newExpanded")
        
        // UI 상태 업데이트 - unified structure도 함께 업데이트
        val updatedCategories = _uiState.value.projectStructure.categories.map { cat ->
            if (cat.id == category.id) {
                Log.d("HomeViewModel", "Updating category ${cat.name.value}: ${cat.isExpanded} -> $newExpanded")
                cat.copy(isExpanded = newExpanded)
            } else {
                cat
            }
        }
        
        // Rebuild unified structure items with updated expansion states
        val updatedUnifiedItems = mutableListOf<com.example.feature_home.model.ProjectStructureItem>()
        
        // Add updated categories
        updatedCategories.forEach { cat ->
            updatedUnifiedItems.add(
                com.example.feature_home.model.ProjectStructureItem.CategoryItem(
                    category = cat,
                    globalOrder = cat.order
                )
            )
        }
        
        // Add direct channels
        _uiState.value.projectStructure.directChannel.forEachIndexed { index, channel ->
            val globalOrder = 1 + index
            updatedUnifiedItems.add(
                com.example.feature_home.model.ProjectStructureItem.DirectChannelItem(
                    channel = channel,
                    globalOrder = globalOrder
                )
            )
        }
        
        _uiState.update { state ->
            state.copy(
                projectStructure = state.projectStructure.copy(
                    categories = updatedCategories,
                    unifiedStructureItems = updatedUnifiedItems.sortedBy { it.globalOrder }
                )
            )
        }
        
        Log.d("HomeViewModel", "Updated categories count: ${updatedCategories.size}, unified items: ${updatedUnifiedItems.size}")
    }

    /**
     * 채널 클릭 처리
     */
    fun onChannelClick(channel: ChannelUiModel) {
        Log.d("HomeViewModel", "Channel clicked: ${channel.name}")
        
        val projectId = _uiState.value.selectedProjectId ?: return
        services.navigationService.handleChannelClick(projectId, channel)
    }

    /**
     * DM 아이템 클릭 처리
     */
    fun onDmItemClick(dm: DmUiModel) {
        Log.d("HomeViewModel", "DM item clicked: ${dm.partnerName}")
        services.navigationService.handleDmItemClick(dm)
    }

    /**
     * 카테고리 롱프레스 처리
     */
    fun onCategoryLongPress(category: CategoryUiModel) {
        Log.d("HomeViewModel", "Category long pressed: ${category.name}")
        
        val items = services.dialogManagementService.createCategoryLongPressActionSheet(
            category = category,
            onEditClick = { cat -> onCategoryEditClick(cat) },
            onReorderClick = { onReorderClick() }
        )
        dialogState = services.dialogManagementService.showBottomSheet(dialogState, items)
        
        _uiState.update { it.copy(
            showBottomSheet = true,
            showBottomSheetItems = items
        )}
    }

    /**
     * 채널 롱프레스 처리
     */
    fun onChannelLongPress(channel: ChannelUiModel, categoryId: String? = null) {
        Log.d("HomeViewModel", "Channel long pressed: ${channel.name}, categoryId: $categoryId")
        
        val items = services.dialogManagementService.createChannelLongPressActionSheet(
            channel = channel,
            categoryId = categoryId,
            onEditClick = { ch, catId -> onChannelEditClick(ch, catId) },
            onReorderClick = { 
                if (categoryId != null && categoryId != Category.NO_CATEGORY_ID) {
                    onChannelReorderClick(DocumentId(categoryId))
                } else {
                    onReorderClick() // 직속 채널의 경우 전체 구조 순서 변경
                }
            }
        )
        dialogState = services.dialogManagementService.showBottomSheet(dialogState, items)
        
        _uiState.update { it.copy(
            showBottomSheet = true,
            showBottomSheetItems = items
        )}
    }

    /**
     * DM 롱프레스 처리
     */
    fun onDmLongPress(dm: DmUiModel) {
        Log.d("HomeViewModel", "DM long pressed: ${dm.partnerName}")
        
        val items = services.dialogManagementService.createDmLongPressActionSheet(dm)
        dialogState = services.dialogManagementService.showBottomSheet(dialogState, items)
        
        _uiState.update { it.copy(
            showBottomSheet = true,
            showBottomSheetItems = items
        )}
    }

    /**
     * 프로젝트 설정 클릭 처리
     */
    fun onProjectSettingsClicked(projectId: DocumentId) {
        Log.d("HomeViewModel", "Project settings clicked: $projectId")
        services.navigationService.navigateToProjectSettings(projectId)
    }

    /**
     * 프로젝트 추가 버튼 클릭 처리
     */
    fun onProjectAddButtonClick() {
        Log.d("HomeViewModel", "Project add button clicked")
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.ShowAddProjectDialog)
        }
    }

    /**
     * 친구 추가 버튼 클릭 처리
     */
    fun onAddFriendClick() {
        Log.d("HomeViewModel", "Add friend button clicked")
    }

    /**
     * 프로젝트 요소 추가 처리
     */
    fun onAddProjectElement(projectId: DocumentId) {
        Log.d("HomeViewModel", "Add project element: $projectId")
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.ShowAddProjectElementDialog(projectId))
        }
    }



    /**
     * 통합된 프로젝트 구조 순서 변경 처리 (카테고리와 직속 채널 통합)
     */
    fun onReorderUnifiedProjectStructure(
        projectId: DocumentId,
        reorderedStructure: List<ProjectStructureItem>
    ) {
        Log.d("HomeViewModel", "Reordering unified project structure for project: $projectId")
        Log.d("HomeViewModel", "Reordered structure: $reorderedStructure")
        
        viewModelScope.launch {
            val result = services.categoryManagementService.reorderUnifiedProjectStructure(
                projectId = projectId,
                reorderedStructure = reorderedStructure
            )
            when (result) {
                is CustomResult.Success -> {
                    Log.d("HomeViewModel", "Successfully reordered unified project structure")
                    refreshProjectStructure(projectId)
                }
                is CustomResult.Failure -> {
                    Log.e("HomeViewModel", "Failed to reorder unified project structure", result.error)
                    _eventFlow.emit(HomeEvent.ShowSnackbar("프로젝트 구조 순서 변경에 실패했습니다."))
                }
                else -> {}
            }
        }
    }

    /**
     * 프로젝트 구조 새로고침
     */
    fun refreshProjectStructure(projectId: DocumentId) {
        Log.d("HomeViewModel", "Refreshing project structure: $projectId")
        viewModelScope.launch {
            services.projectSelectionService.refreshProjectStructure(projectId)
            loadProjectStructure(projectId)
        }
    }

    /**
     * 카테고리 확장 상태 복원
     */
    fun restoreExpandedCategories(expandedCategoryIds: List<String>) {
        Log.d("HomeViewModel", "Restoring expanded categories: $expandedCategoryIds")
        val projectId = _uiState.value.selectedProjectId ?: return
        services.categoryManagementService.restoreExpandedCategories(projectId, expandedCategoryIds)
    }

    /**
     * 액션 시트 닫기
     */
    fun onProjectItemActionSheetDismiss() {
        Log.d("HomeViewModel", "Action sheet dismissed")
        dialogState = services.dialogManagementService.dismissBottomSheet(dialogState)
        _uiState.update { it.copy(showBottomSheet = false, showBottomSheetItems = emptyList()) }
    }

    /**
     * 에러 메시지 표시됨 처리
     */
    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = "default") }
    }

    /**
     * 상단 섹션 클릭 처리
     */
    fun onClickTopSection() {
        Log.d("HomeViewModel", "Top section clicked")
        // 추가 처리 필요시 구현
    }


    /**
     * 정리 작업
     */
    override fun onCleared() {
        super.onCleared()
        userStreamJob?.cancel()
        projectsStreamJob?.cancel()
        dmsStreamJob?.cancel()
        projectDetailsJob?.cancel()
        projectStructureJob?.cancel()
        
        // 카테고리 상태 정리
        _uiState.value.selectedProjectId?.let { projectId ->
            if (::services.isInitialized) {
                services.categoryManagementService.clearCategoryStates(projectId)
            }
        }
        
        Log.d("HomeViewModel", "HomeViewModel cleared")
    }

    /**
     * 카테고리 편집 버튼 클릭 처리 (직접 편집 화면으로 네비게이션)
     */
    private fun onCategoryEditClick(category: CategoryUiModel) {
        Log.d("HomeViewModel", "Category edit clicked: ${category.name} - navigating directly to edit screen")
        
        // 바텀시트 닫기
        onProjectItemActionSheetDismiss()
        
        // 현재 프로젝트 ID 가져오기
        val projectId = _uiState.value.selectedProjectId
        if (projectId != null) {
            // 직접 편집 화면으로 네비게이션
            Log.d("HomeViewModel", "Navigating to edit category: projectId=${projectId.value}, categoryId=${category.id.value}")
            navigationManger.navigateToEditCategory(projectId.value, category.id.value)
        }
    }


    /**
     * 채널 편집 버튼 클릭 처리 (직접 편집 화면으로 네비게이션)
     */
    private fun onChannelEditClick(channel: ChannelUiModel, categoryId: String?) {
        Log.d("HomeViewModel", "Channel edit clicked: ${channel.name} - navigating directly to edit screen")
        
        // 바텀시트 닫기
        onProjectItemActionSheetDismiss()
        
        // 현재 프로젝트 ID 가져오기
        val projectId = _uiState.value.selectedProjectId
        if (projectId != null) {
            // 직접 편집 화면으로 네비게이션
            Log.d("HomeViewModel", "Navigating to edit channel: projectId=${projectId.value}, channelId=${channel.id.value}")
            navigationManger.navigateToEditChannel(projectId.value, channel.id.value)
        }
    }


    /**
     * 순서 변경 버튼 클릭 처리 (통합 - 카테고리/채널 모두 동일)
     */
    private fun onReorderClick() {
        Log.d("HomeViewModel", "Reorder clicked - showing unified project structure dialog")
        
        // 바텀시트 닫기
        onProjectItemActionSheetDismiss()
        
        // 통합 프로젝트 구조 순서 변경 다이얼로그 표시
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.ShowReorderProjectStructureDialog)
        }
    }

    /**
     * 카테고리 내 채널 순서 변경 버튼 클릭 처리
     */
    private fun onChannelReorderClick(categoryId: DocumentId) {
        Log.d("HomeViewModel", "Channel reorder clicked - showing category channels dialog for categoryId: $categoryId")
        
        // 바텀시트 닫기
        onProjectItemActionSheetDismiss()
        
        // 카테고리별 채널 순서 변경 다이얼로그 표시
        viewModelScope.launch {
            _eventFlow.emit(HomeEvent.ShowReorderCategoryChannelsDialog(categoryId))
        }
    }
    
    /**
     * 카테고리 내 채널 순서 변경 처리
     */
    fun onReorderCategoryChannels(
        projectId: DocumentId,
        categoryId: DocumentId,
        reorderedChannels: List<ChannelUiModel>
    ) {
        Log.d("HomeViewModel", "Reordering category channels - projectId: $projectId, categoryId: $categoryId")
        Log.d("HomeViewModel", "Reordered channels: ${reorderedChannels.map { it.name.value }}")
        
        viewModelScope.launch {
            val result = services.categoryManagementService.reorderCategoryChannels(
                projectId = projectId,
                categoryId = categoryId,
                reorderedChannels = reorderedChannels
            )
            when (result) {
                is CustomResult.Success -> {
                    Log.d("HomeViewModel", "Successfully reordered category channels")
                    refreshProjectStructure(projectId)
                }
                is CustomResult.Failure -> {
                    Log.e("HomeViewModel", "Failed to reorder category channels", result.error)
                    _eventFlow.emit(HomeEvent.ShowSnackbar("채널 순서 변경에 실패했습니다."))
                }
                else -> {}
            }
        }
    }
}