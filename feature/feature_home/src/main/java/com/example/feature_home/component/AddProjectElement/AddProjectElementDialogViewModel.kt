package com.example.feature_home.component.AddProjectElement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.vo.DocumentId
import com.example.domain.vo.Name
import com.example.domain.vo.OwnerId
import com.example.domain.vo.category.CategoryName
import com.example.domain.vo.category.CategoryOrder
import com.example.domain.vo.category.IsCategoryFlag
import com.example.domain_usecase.provider.project.ProjectChannelUseCaseProvider
import com.example.domain_usecase.provider.project.ProjectStructureUseCaseProvider
import com.example.domain_usecase.provider.project.ProjectMemberUseCaseProvider
import com.example.domain.vo.user.UserName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.core_ui.util.withLoading
import com.example.feature_home.dialog.viewmodel.AddProjectElementDialogEvent
import com.example.feature_home.dialog.viewmodel.AddProjectElementDialogUiState
import com.example.feature_home.dialog.viewmodel.CreateElementType
import java.time.Instant

/**
 * AddProjectElementDialogViewModel: 프로젝트 요소(카테고리/채널) 생성 다이얼로그의 ViewModel
 */
@HiltViewModel
class AddProjectElementDialogViewModel @Inject constructor(
    private val projectStructureUseCaseProvider: ProjectStructureUseCaseProvider,
    private val projectChannelUseCaseProvider: ProjectChannelUseCaseProvider,
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProjectElementDialogUiState())
    val uiState: StateFlow<AddProjectElementDialogUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddProjectElementDialogEvent>()
    val eventFlow: SharedFlow<AddProjectElementDialogEvent> = _eventFlow.asSharedFlow()

    private var projectId: DocumentId? = null

    /**
     * 다이얼로그 내 입력 상태/오류/선택값을 모두 초기화합니다.
     * - 다음 열림 시 깨끗한 상태로 시작하도록 보장
     */
    fun resetFormState() {
        _uiState.value = AddProjectElementDialogUiState()
    }

    /**
     * 다이얼로그 초기화
     * 
     * @param projectId 프로젝트 ID
     */
    fun initialize(projectId: String) {
        if (projectId.isBlank()) {
            return
        }
        
        this.projectId = DocumentId(projectId)
        
        // 프로젝트 구조 로드하여 카테고리 목록 가져오기
        loadProjectCategories()
    }

    /**
     * 탭 변경 처리
     *
     * @param selectedTab 선택된 탭 (0: 카테고리, 1: 채널, 2: 멤버 초대)
     */
    fun onTabChanged(selectedTab: Int) {
        val tabType = when (selectedTab) {
            0 -> CreateElementType.CATEGORY
            1 -> CreateElementType.CHANNEL
            2 -> CreateElementType.MEMBER_INVITE
            else -> CreateElementType.CATEGORY
        }
        
        _uiState.value = _uiState.value.copy(
            selectedTab = tabType,
            // 탭 변경 시 입력값 초기화
            categoryName = "",
            channelName = "",
            selectedCategoryId = null,
            selectedChannelType = ProjectChannelType.MESSAGES,
            categoryNameError = null,
            channelNameError = null,
            memberInviteUserName = "",
            memberInviteUserNameError = null,
            isSendingInvite = false
        )
    }

    /**
     * 카테고리 이름 변경 처리
     */
    fun onCategoryNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(
            categoryName = name,
            categoryNameError = null
        )
    }

    /**
     * 채널 이름 변경 처리
     */
    fun onChannelNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(
            channelName = name,
            channelNameError = null
        )
    }

    /**
     * 채널 카테고리 변경 처리
     */
    fun onChannelCategoryChanged(categoryId: String?) {
        _uiState.value = _uiState.value.copy(
            selectedCategoryId = categoryId
        )
    }

    /**
     * 채널 타입 변경 처리
     */
    fun onChannelTypeChanged(channelType: ProjectChannelType) {
        _uiState.value = _uiState.value.copy(
            selectedChannelType = channelType
        )
    }

    /**
     * 카테고리 생성 처리
     */
    fun onCreateCategory() {
        val projectId = this.projectId ?: return

        viewModelScope.launch {
            try {
                // 이전 오류 상태 초기화
                _uiState.value = _uiState.value.copy(categoryNameError = null)
                val structureUseCases = projectStructureUseCaseProvider.createForProject(projectId)
                // Let domain validation handle the validation - trim first to ensure clean input
                val categoryName = CategoryName(_uiState.value.categoryName.trim())

                _uiState.withLoading(setLoading = { s, l -> s.copy(isLoading = l) }) {
                    when (val result =
                        structureUseCases.addCategoryUseCase(projectId, categoryName)) {
                        is CustomResult.Success -> {
                            _eventFlow.emit(AddProjectElementDialogEvent.CategoryCreated(result.data))
                            _eventFlow.emit(AddProjectElementDialogEvent.DismissDialog)
                        }

                        is CustomResult.Failure -> {
                            _uiState.value = _uiState.value.copy(
                                categoryNameError = result.error.message ?: "카테고리 생성에 실패했습니다."
                            )
                        }

                        else -> {
                            _uiState.value = _uiState.value.copy(
                                categoryNameError = "카테고리 생성에 실패했습니다."
                            )
                        }
                    }
                }
            } catch (e: IllegalArgumentException) {
                // Domain validation errors (from CategoryName validation)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    categoryNameError = e.message ?: "카테고리 이름이 올바르지 않습니다."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    categoryNameError = "카테고리 생성 중 오류가 발생했습니다."
                )
            }
        }
    }

    /**
     * 채널 생성 처리
     */
    fun onCreateChannel() {
        val projectId = this.projectId ?: return

        // 채널 이름 검증
        if (_uiState.value.channelName.isBlank()) {
            _uiState.value = _uiState.value.copy(
                channelNameError = "채널 이름을 입력해주세요."
            )
            return
        }

        if (_uiState.value.channelName.length > 50) {
            _uiState.value = _uiState.value.copy(
                channelNameError = "채널 이름은 50자 이하로 입력해주세요."
            )
            return
        }

        viewModelScope.launch {
            try {
                // 이전 오류 상태 초기화
                _uiState.value = _uiState.value.copy(channelNameError = null)
                val channelUseCases = projectChannelUseCaseProvider.createForProject(projectId)
                val channelName = Name(_uiState.value.channelName.trim())
                
                // 카테고리 ID 처리 (null이면 NO_CATEGORY_ID 사용)
                val categoryId = _uiState.value.selectedCategoryId 
                    ?: Category.NO_CATEGORY_ID

                _uiState.withLoading(setLoading = { s, l -> s.copy(isLoading = l) }) {
                    when (val result = channelUseCases.addProjectChannelUseCase(
                        projectId = projectId,
                        channelName = channelName,
                        categoryId = DocumentId(categoryId),
                        channelType = _uiState.value.selectedChannelType
                    )) {
                        is CustomResult.Success -> {
                            _eventFlow.emit(AddProjectElementDialogEvent.ChannelCreated(result.data))
                            _eventFlow.emit(AddProjectElementDialogEvent.DismissDialog)
                        }

                        is CustomResult.Failure -> {
                            _uiState.value = _uiState.value.copy(
                                channelNameError = result.error.message ?: "채널 생성에 실패했습니다."
                            )
                        }

                        else -> {
                            _uiState.value = _uiState.value.copy(
                                channelNameError = "채널 생성에 실패했습니다."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    channelNameError = "채널 생성 중 오류가 발생했습니다."
                )
            }
        }
    }

    /**
     * 다이얼로그 닫기 처리
     */
    fun onDismiss() {
        viewModelScope.launch {
            _eventFlow.emit(AddProjectElementDialogEvent.DismissDialog)
            // 닫힐 때 폼 상태 정리
            resetFormState()
        }
    }

    /**
     * 프로젝트 카테고리 목록 로드
     */
    private fun loadProjectCategories() {
        val projectId = this.projectId ?: return
        
        viewModelScope.launch {
            _uiState.withLoading(setLoading = { s, l -> s.copy(isLoading = l) }) {
                try {
                    val structureUseCases =
                        projectStructureUseCaseProvider.createForProject(projectId)

                    // Flow가 Success/Failure를 방출할 때까지 로딩 상태로 대기
                    val terminalResult = structureUseCases
                        .getProjectAllCategoriesUseCase()
                        .first { it is CustomResult.Success || it is CustomResult.Failure }

                    when (terminalResult) {
                        is CustomResult.Success -> {
                            val categories = terminalResult.data.toMutableList()

                            // NoCategory가 없으면 UI 표시용으로 추가
                            val hasNoCategory =
                                categories.any { it.id.value == Category.NO_CATEGORY_ID }
                            if (!hasNoCategory) {
                                val noCategory = Category.fromDataSource(
                                    id = DocumentId(Category.NO_CATEGORY_ID),
                                    name = CategoryName.NO_CATEGORY_NAME,
                                    order = CategoryOrder(Category.NO_CATEGORY_ORDER),
                                    createdBy = OwnerId("system"),
                                    createdAt = Instant.now(),
                                    updatedAt = Instant.now(),
                                    isCategory = IsCategoryFlag.FALSE
                                )
                                categories.add(0, noCategory)
                            }

                            _uiState.value = _uiState.value.copy(
                                availableCategories = categories.sortedBy { it.order.value }
                            )
                        }

                        is CustomResult.Failure -> {
                            // 실패 시 NoCategory만 표시
                            val noCategory = Category.fromDataSource(
                                id = DocumentId(Category.NO_CATEGORY_ID),
                                name = CategoryName.NO_CATEGORY_NAME,
                                order = CategoryOrder(Category.NO_CATEGORY_ORDER),
                                createdBy = OwnerId("system"),
                                createdAt = Instant.now(),
                                updatedAt = Instant.now(),
                                isCategory = IsCategoryFlag.FALSE
                            )
                            _uiState.value = _uiState.value.copy(
                                availableCategories = listOf(noCategory)
                            )
                        }

                        else -> {
                            // 이 분기는 first { Success || Failure } 조건상 거의 도달하지 않지만,
                            // 방어적으로 기본 NoCategory만 표시
                            val noCategory = Category.fromDataSource(
                                id = DocumentId(Category.NO_CATEGORY_ID),
                                name = CategoryName.NO_CATEGORY_NAME,
                                order = CategoryOrder(Category.NO_CATEGORY_ORDER),
                                createdBy = OwnerId("system"),
                                createdAt = Instant.now(),
                                updatedAt = Instant.now(),
                                isCategory = IsCategoryFlag.FALSE
                            )
                            _uiState.value = _uiState.value.copy(
                                availableCategories = listOf(noCategory)
                            )
                        }
                    }
                } catch (e: Exception) {
                    // 예외 발생 시 NoCategory만 표시
                    val noCategory = Category.fromDataSource(
                        id = DocumentId(Category.NO_CATEGORY_ID),
                        name = CategoryName.NO_CATEGORY_NAME,
                        order = CategoryOrder(Category.NO_CATEGORY_ORDER),
                        createdBy = OwnerId("system"),
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                        isCategory = IsCategoryFlag.FALSE
                    )
                    _uiState.value = _uiState.value.copy(
                        availableCategories = listOf(noCategory)
                    )
                }
            }
        }
    }

    /**
     * 멤버 초대 사용자 이름 변경 처리
     */
    fun onMemberInviteUserNameChanged(userName: String) {
        _uiState.value = _uiState.value.copy(
            memberInviteUserName = userName,
            memberInviteUserNameError = if (userName.isNotBlank()) null else _uiState.value.memberInviteUserNameError
        )
    }

    /**
     * 멤버 초대 전송
     */
    fun onSendMemberInvite() {
        val projectId = this.projectId ?: return
        val userName = _uiState.value.memberInviteUserName.trim()

        if (userName.isBlank()) {
            _uiState.value = _uiState.value.copy(
                memberInviteUserNameError = "사용자 이름을 입력해주세요."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSendingInvite = true,
                memberInviteUserNameError = null
            )

            try {
                val memberUseCases = projectMemberUseCaseProvider.createForProject(projectId)
                val userNameVO = UserName(userName)

                memberUseCases.sendProjectInviteMessageUseCase(userNameVO, projectId.value)
                    .collect { result ->
                        when (result) {
                            is CustomResult.Loading -> {
                                // 이미 로딩 상태 설정됨
                            }

                            is CustomResult.Success -> {
                                _uiState.value = _uiState.value.copy(
                                    isSendingInvite = false,
                                    memberInviteUserName = "",
                                    memberInviteUserNameError = null
                                )
                                _eventFlow.emit(AddProjectElementDialogEvent.MemberInvited(userName))
                            }

                            is CustomResult.Failure -> {
                                _uiState.value = _uiState.value.copy(
                                    isSendingInvite = false,
                                    memberInviteUserNameError = result.error.message
                                        ?: "초대 전송 중 오류가 발생했습니다."
                                )
                            }

                            else -> {
                                // Other states - continue loading
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSendingInvite = false,
                    memberInviteUserNameError = e.message ?: "초대 전송 중 오류가 발생했습니다."
                )
            }
        }
    }
}
