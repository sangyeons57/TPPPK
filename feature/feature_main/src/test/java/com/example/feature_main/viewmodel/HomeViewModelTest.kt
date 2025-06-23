package com.example.feature_main.viewmodel

import com.example.data.util.CoroutinesTestRule
import com.example.data.util.FlowTestExtensions.EventCollector
import com.example.data.util.FlowTestExtensions.getValue
import com.example.domain.model.DmConversation
import com.example.domain.model.Project
import com.example.domain.repository.base.ProjectRepository
import com.example.domain.repository.ChannelRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import kotlinx.coroutines.flow.flowOf

/**
 * HomeViewModel 테스트
 *
 * 이 테스트는 순수 JUnit 환경에서 HomeViewModel의 기능을 검증합니다.
 * Fake 구현체를 통해 외부 의존성 없이 테스트합니다.
 */
@ExperimentalCoroutinesApi
class HomeViewModelTest {

    // Coroutines 테스트 환경 설정
    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    // 테스트 대상 (System Under Test)
    private lateinit var viewModel: com.example.feature_home.HomeViewModel

    // Fake Repositories
    private lateinit var fakeProjectRepository: FakeProjectRepository
    private lateinit var fakeChannelRepository: FakeChannelRepository

    // 테스트 데이터
    private val testProject1 = Project(
        id = "project_1",
        name = "테스트 프로젝트 1",
        description = "테스트 프로젝트 설명 1",
        createdAt = LocalDateTime.now(),
        createdBy = "test_user_1",
        isPublic = true
    )
    
    private val testProject2 = Project(
        id = "project_2",
        name = "테스트 프로젝트 2",
        description = "테스트 프로젝트 설명 2",
        createdAt = LocalDateTime.now(),
        createdBy = "test_user_1",
        isPublic = false
    )
    
    private val testDm1 = DmConversation(
        id = "dm_1",
        partnerId = "partner_1",
        partnerName = "테스트 친구 1",
        lastMessage = "안녕하세요?",
        lastMessageTime = LocalDateTime.now().minusHours(1),
        unreadCount = 2
    )
    
    private val testDm2 = DmConversation(
        id = "dm_2",
        partnerId = "partner_2",
        partnerName = "테스트 친구 2",
        lastMessage = "프로젝트 확인해주세요",
        lastMessageTime = LocalDateTime.now().minusMinutes(30),
        unreadCount = 0
    )

    /**
     * 테스트 초기화
     */
    @Before
    fun setup() {
        // Fake Repository 초기화
        fakeProjectRepository = FakeProjectRepository()
        fakeChannelRepository = FakeChannelRepository()

        // 테스트 데이터 설정
        fakeProjectRepository.addProject(testProject1)
        fakeProjectRepository.addProject(testProject2)
        fakeChannelRepository.addDmChannel(testDm1)
        fakeChannelRepository.addDmChannel(testDm2)

        // ViewModel 초기화 (의존성 주입)
        viewModel =
            com.example.feature_home.HomeViewModel(fakeProjectRepository, fakeChannelRepository)
    }

    /**
     * 초기 상태 테스트
     */
    @Test
    fun `초기 상태는 프로젝트 탭을 선택하고 프로젝트 목록을 로드해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기화된 ViewModel (setup에서 이미 초기화됨)

        // When: UI 상태 가져오기
        val initialState = viewModel.uiState.getValue()

        // Then: 초기 상태 확인
        assertEquals(com.example.feature_home.TopSection.PROJECTS, initialState.selectedTopSection)
        assertFalse(initialState.isLoading)
        assertTrue(initialState.projects.isNotEmpty())
        assertEquals("default", initialState.errorMessage)
    }

    /**
     * 탭 전환 테스트
     */
    @Test
    fun `DM 탭으로 전환 시 DM 목록을 로드해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 초기 상태의 ViewModel

        // When: DM 탭으로 전환
        viewModel.onTopSectionSelect(com.example.feature_home.TopSection.DMS)

        // Then: 상태 업데이트 확인
        val state = viewModel.uiState.getValue()
        assertEquals(com.example.feature_home.TopSection.DMS, state.selectedTopSection)
        assertTrue(state.dms.isNotEmpty())
        assertEquals(2, state.dms.size)
    }

    /**
     * 프로젝트 클릭 테스트
     */
    @Test
    fun `프로젝트 클릭 시 NavigateToProjectDetails 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정
        val eventCollector = EventCollector<com.example.feature_home.HomeEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)

        // When: 프로젝트 클릭
        val projectId = "test_project_id"
        viewModel.onProjectClick(projectId)

        // Then: 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is com.example.feature_home.HomeEvent.NavigateToProjectDetails)
        assertEquals(projectId, (event as com.example.feature_home.HomeEvent.NavigateToProjectDetails).projectId)
    }

    /**
     * 추가 버튼 클릭 테스트 (프로젝트 탭)
     */
    @Test
    fun `프로젝트 탭에서 추가 버튼 클릭 시 ShowAddProjectDialog 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 프로젝트 탭 선택
        val eventCollector = EventCollector<com.example.feature_home.HomeEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        viewModel.onTopSectionSelect(com.example.feature_home.TopSection.PROJECTS)

        // When: 추가 버튼 클릭
        viewModel.onAddButtonClick()

        // Then: 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is com.example.feature_home.HomeEvent.ShowAddProjectDialog)
    }

    /**
     * 추가 버튼 클릭 테스트 (DM 탭)
     */
    @Test
    fun `DM 탭에서 추가 버튼 클릭 시 ShowAddFriendDialog 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정 및 DM 탭 선택
        val eventCollector = EventCollector<com.example.feature_home.HomeEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)
        viewModel.onTopSectionSelect(com.example.feature_home.TopSection.DMS)

        // When: 추가 버튼 클릭
        viewModel.onAddButtonClick()

        // Then: 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is com.example.feature_home.HomeEvent.ShowAddFriendDialog)
    }

    /**
     * 프로젝트 추가 버튼 클릭 테스트
     */
    @Test
    fun `프로젝트 추가 버튼 클릭 시 NavigateToAddProject 이벤트가 발생해야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: 이벤트 수집기 설정
        val eventCollector = EventCollector<com.example.feature_home.HomeEvent>()
        eventCollector.collectFrom(coroutinesTestRule.testCoroutineScope, viewModel.eventFlow)

        // When: 프로젝트 추가 버튼 클릭
        viewModel.onProjectAddButtonClick()

        // Then: 이벤트 발생 확인
        assertTrue(eventCollector.events.isNotEmpty())
        val event = eventCollector.events.first()
        assertTrue(event is com.example.feature_home.HomeEvent.NavigateToAddProject)
    }

    /**
     * Repository 오류 테스트 (프로젝트 로딩)
     */
    @Test
    fun `프로젝트 Repository 오류 발생 시 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: Repository에서 오류가 발생하도록 설정
        fakeProjectRepository.setShouldSimulateError(true)

        // When: ViewModel 초기화 (자동으로 프로젝트 로딩)
        viewModel =
            com.example.feature_home.HomeViewModel(fakeProjectRepository, fakeChannelRepository)

        // Then: 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertEquals("프로젝트 로드 실패", state.errorMessage)
    }

    /**
     * Repository 오류 테스트 (DM 로딩)
     */
    @Test
    fun `DM Repository 오류 발생 시 에러 메시지가 표시되어야 함`() = coroutinesTestRule.runBlockingTest {
        // Given: Repository에서 오류가 발생하도록 설정
        fakeChannelRepository.setShouldSimulateError(true)

        // When: DM 탭으로 전환
        viewModel.onTopSectionSelect(com.example.feature_home.TopSection.DMS)

        // Then: 에러 메시지 확인
        val state = viewModel.uiState.getValue()
        assertEquals("DM 로드 실패", state.errorMessage)
    }
}

/**
 * HomeViewModel 테스트용 FakeProjectRepository
 */
class FakeProjectRepository : ProjectRepository {
    private val projects = mutableListOf<Project>()
    private val projectsFlow = MutableStateFlow<List<Project>>(emptyList())
    private var shouldSimulateError = false

    fun addProject(project: Project) {
        projects.add(project)
        projectsFlow.value = projects.toList()
    }

    fun clearProjects() {
        projects.clear()
        projectsFlow.value = emptyList()
    }

    fun setShouldSimulateError(shouldError: Boolean) {
        shouldSimulateError = shouldError
    }

    override fun getProjectListStream(): Flow<List<Project>> = projectsFlow

    override suspend fun fetchProjectList(): Result<Unit> {
        return if (shouldSimulateError) {
            Result.failure(Exception("Simulated project fetch error"))
        } else {
            projectsFlow.value = projects
            Result.success(Unit)
        }
    }

    override suspend fun isProjectNameAvailable(name: String): Result<Boolean> {
        return if (shouldSimulateError) {
            Result.failure(Exception("Simulated error"))
        } else {
            Result.success(!projects.any { it.name == name })
        }
    }

    override suspend fun joinProjectWithCode(codeOrLink: String): Result<String> {
        return if (shouldSimulateError) {
            Result.failure(Exception("Simulated error"))
        } else {
            Result.success("project_id")
        }
    }
    
    override suspend fun getProjectInfoFromToken(token: String): Result<ProjectInfo> {
        return if (shouldSimulateError) {
            Result.failure(Exception("Simulated error"))
        } else {
            Result.success(ProjectInfo("project_id", "Test Project", "Test Description", true))
        }
    }
    
    override suspend fun joinProjectWithToken(token: String): Result<String> {
        return if (shouldSimulateError) {
            Result.failure(Exception("Simulated error"))
        } else {
            Result.success("project_id")
        }
    }
    
    override suspend fun createProject(name: String, description: String, isPublic: Boolean): Result<Project> {
        return if (shouldSimulateError) {
            Result.failure(Exception("Simulated error"))
        } else {
            val project = Project(
                id = "new_project_id",
                name = name,
                description = description,
                createdAt = LocalDateTime.now(),
                createdBy = "current_user_id",
                isPublic = isPublic
            )
            addProject(project)
            Result.success(project)
        }
    }
    
    override suspend fun getAvailableProjectsForScheduling(): Result<List<Project>> {
        return if (shouldSimulateError) {
            Result.failure(Exception("Simulated error"))
        } else {
            Result.success(projects)
        }
    }
}

/**
 * HomeViewModel 테스트용 FakeChannelRepository
 */
class FakeChannelRepository : ChannelRepository {
    private val dmChannels = mutableListOf<Channel>()
    private val dmChannelsFlow = MutableStateFlow<List<Channel>>(emptyList())
    private var shouldSimulateError = false

    fun addDmChannel(channel: Channel) {
        dmChannels.add(channel)
        dmChannelsFlow.value = dmChannels.toList()
    }

    fun clearDmChannels() {
        dmChannels.clear()
        dmChannelsFlow.value = emptyList()
    }

    fun setShouldSimulateError(shouldError: Boolean) {
        shouldSimulateError = shouldError
    }

    override fun getUserDmChannelsStream(userId: String): Flow<List<Channel>> = dmChannelsFlow

    override suspend fun getUserDmChannels(userId: String): Result<List<Channel>> {
        return if (shouldSimulateError) {
            Result.failure(Exception("Simulated DM fetch error"))
        } else {
            Result.success(dmChannels)
        }
    }

    // 나머지 필요한 메서드들은 기본 구현만 제공 (테스트에 사용되지 않음)
    override suspend fun createChannel(name: String, description: String?, type: String, isPrivate: Boolean, projectId: String?, categoryId: String?): Result<Channel> = Result.success(Channel("channel1", "Channel", null, "TEXT", false, null, null, emptyList(), emptyMap(), null, null))
    override suspend fun createDmChannel(otherUserId: String, isActive: Boolean): Result<Channel> = Result.success(Channel("dm1", "DM", null, "DM", false, null, null, listOf("user1", otherUserId), emptyMap(), null, null))
    override suspend fun getChannelById(channelId: String): Result<Channel?> = Result.success(null)
    override suspend fun updateChannel(channelId: String, updates: Map<String, Any>): Result<Unit> = Result.success(Unit)
    override suspend fun deleteChannel(channelId: String): Result<Unit> = Result.success(Unit)
    override suspend fun addDmParticipant(channelId: String, userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun removeDmParticipant(channelId: String, userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getDmParticipants(channelId: String): Result<List<String>> = Result.success(emptyList())
    override fun getDmParticipantsStream(channelId: String): Flow<List<String>> = flowOf(emptyList())
    override suspend fun createOrGetDmChannel(userId1: String, userId2: String): Result<Channel> = Result.success(Channel("dm1", "DM", null, "DM", false, null, null, listOf(userId1, userId2), emptyMap(), null, null))
    override suspend fun getDmChannelByUsers(userId1: String, userId2: String): Result<Channel?> = Result.success(null)
    override suspend fun getChannelMessages(channelId: String, limit: Int, beforeMessageId: String?): Result<List<Message>> = Result.success(emptyList())
    override fun getChannelMessagesStream(channelId: String, limit: Int): Flow<List<Message>> = flowOf(emptyList())
    override suspend fun sendMessage(channelId: String, message: String, attachments: List<String>?): Result<Message> = Result.success(Message("msg1", "channel1", "user1", "username", null, "test", System.currentTimeMillis(), false, emptyList()))
    override suspend fun editMessage(messageId: String, channelId: String, newContent: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteMessage(messageId: String, channelId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getChannelMembers(channelId: String): Result<List<User>> = Result.success(emptyList())
    override fun getChannelMembersStream(channelId: String): Flow<List<User>> = flowOf(emptyList())
    override suspend fun addChannelMember(channelId: String, userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun removeChannelMember(channelId: String, userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getChannelPermissions(channelId: String): Result<List<ChannelPermission>> = Result.success(emptyList())
} 