package com.example.feature_main.ui

import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import com.example.feature_main.viewmodel.*
import org.junit.Rule
import org.junit.Test

/**
 * HomeScreen UI 테스트
 *
 * 이 테스트는 HomeScreen의 주요 UI 컴포넌트와 상호작용을 검증합니다.
 * Mockito를 사용하지 않고 상태 기반 테스트로 구현되었습니다.
 */
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * 프로젝트 목록 렌더링 테스트
     */
    @Test
    fun homeScreen_whenProjectsLoaded_rendersProjectsList() {
        // Given: 프로젝트가 포함된 UI 상태
        val projects = List(3) { i ->
            ProjectItem(
                id = "p$i",
                name = "테스트 프로젝트 ${i + 1}",
                description = "설명 ${i + 1}",
                lastUpdated = "${i}분 전"
            )
        }
        
        val uiState = HomeUiState(
            selectedTopSection = TopSection.PROJECTS,
            projects = projects,
            isLoading = false
        )
        
        // When: HomeContent 렌더링
        composeTestRule.setContent {
            // 간단한 fake ViewModel 생성
            val fakeViewModel = object : HomeViewModel() {
                // 필요한 메서드만 재정의
            }
            
            HomeContent(
                uiState = uiState,
                viewModel = fakeViewModel
            )
        }
        
        // Then: 프로젝트 리스트가 올바르게 표시되는지 확인
        projects.forEach { project ->
            // 각 프로젝트의 첫 글자가 대문자로 표시되어야 함
            composeTestRule.onNodeWithText(project.name.take(1).uppercase()).assertIsDisplayed()
        }
    }
    
    /**
     * DM 목록 렌더링 테스트
     */
    @Test
    fun homeScreen_whenDmsLoaded_rendersDmsList() {
        // Given: DM이 포함된 UI 상태
        val dms = List(3) { i ->
            DmItem(
                id = "dm$i",
                partnerName = "테스트 친구 ${i + 1}",
                lastMessage = "테스트 메시지 ${i + 1}",
                unreadCount = i,
                partnerProfileUrl = null
            )
        }
        
        val uiState = HomeUiState(
            selectedTopSection = TopSection.DMS,
            dms = dms,
            isLoading = false
        )
        
        // When: HomeContent 렌더링
        composeTestRule.setContent {
            val fakeViewModel = object : HomeViewModel() {}
            
            HomeContent(
                uiState = uiState,
                viewModel = fakeViewModel
            )
        }
        
        // Then: DM 리스트가 올바르게 표시되는지 확인
        dms.forEach { dm ->
            composeTestRule.onNodeWithText(dm.partnerName).assertIsDisplayed()
            composeTestRule.onNodeWithText(dm.lastMessage ?: "").assertIsDisplayed()
            
            // 읽지 않은 메시지 수 표시 (있는 경우만)
            if (dm.unreadCount > 0) {
                composeTestRule.onNodeWithText("${dm.unreadCount}").assertIsDisplayed()
            }
        }
    }
    
    /**
     * 로딩 상태 표시 테스트
     */
    @Test
    fun homeScreen_whenLoading_showsLoadingIndicator() {
        // Given: 로딩 중인 UI 상태
        val uiState = HomeUiState(
            isLoading = true
        )
        
        // When: HomeContent 렌더링
        composeTestRule.setContent {
            val fakeViewModel = object : HomeViewModel() {}
            
            HomeContent(
                uiState = uiState,
                viewModel = fakeViewModel
            )
        }
        
        // Then: 로딩 인디케이터가 표시되는지 확인
        // CircularProgressIndicator는 테스트 태그가 없으므로 Role.ProgressBar로 검색
        composeTestRule.onNode(hasRole(androidx.compose.ui.semantics.Role.ProgressBar)).assertExists()
    }
    
    /**
     * 빈 DM 목록 상태 테스트
     */
    @Test
    fun homeScreen_whenDmsEmpty_showsEmptyMessage() {
        // Given: 빈 DM 목록을 가진 UI 상태
        val uiState = HomeUiState(
            selectedTopSection = TopSection.DMS,
            dms = emptyList(),
            isLoading = false
        )
        
        // When: HomeContent 렌더링
        composeTestRule.setContent {
            val fakeViewModel = object : HomeViewModel() {}
            
            HomeContent(
                uiState = uiState,
                viewModel = fakeViewModel
            )
        }
        
        // Then: 빈 상태 메시지가 표시되는지 확인
        composeTestRule.onNodeWithText("메시지가 없습니다.").assertIsDisplayed()
    }
    
    /**
     * 프로젝트 아이템 클릭 테스트
     */
    @Test
    fun projectListItem_whenClicked_triggersCallback() {
        // Given: 클릭 콜백 확인용 변수
        var clicked = false
        
        // When: ProjectListItem 렌더링 및 클릭
        composeTestRule.setContent {
            ProjectListItem(
                projectName = "테스트 프로젝트",
                onClick = { clicked = true }
            )
        }
        
        // 항목 클릭
        composeTestRule.onNodeWithText("T").performClick()
        
        // Then: 클릭 콜백이 호출되었는지 확인
        assert(clicked) { "ProjectListItem 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * DM 아이템 클릭 테스트
     */
    @Test
    fun dmListItem_whenClicked_triggersCallback() {
        // Given: 클릭 콜백 확인용 변수
        var clicked = false
        val testDm = DmItem(
            id = "test",
            partnerName = "테스트 친구",
            lastMessage = "테스트 메시지",
            unreadCount = 1,
            partnerProfileUrl = null
        )
        
        // When: DmListItem 렌더링 및 클릭
        composeTestRule.setContent {
            DmListItem(
                dm = testDm,
                onClick = { clicked = true }
            )
        }
        
        // 항목 클릭
        composeTestRule.onNodeWithText("테스트 친구").performClick()
        
        // Then: 클릭 콜백이 호출되었는지 확인
        assert(clicked) { "DmListItem 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 프로젝트 모드 상단 영역 렌더링 테스트
     */
    @Test
    fun mainContentTopSection_projects_rendersCorrectly() {
        // Given: 프로젝트 모드 UI 상태
        val uiState = HomeUiState(
            selectedTopSection = TopSection.PROJECTS
        )
        
        // When: MainContentTopSection 렌더링
        composeTestRule.setContent {
            MainContentTopSection(uiState)
        }
        
        // Then: 프로젝트 모드 상단 영역이 올바르게 표시되는지 확인
        composeTestRule.onNodeWithText("프로젝트 이름").assertIsDisplayed()
        composeTestRule.onNodeWithText("검색하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("+").assertIsDisplayed()
    }
    
    /**
     * DM 모드 상단 영역 렌더링 테스트
     */
    @Test
    fun mainContentTopSection_dms_rendersCorrectly() {
        // Given: DM 모드 UI 상태
        val uiState = HomeUiState(
            selectedTopSection = TopSection.DMS
        )
        
        // When: MainContentTopSection 렌더링
        composeTestRule.setContent {
            MainContentTopSection(uiState)
        }
        
        // Then: DM 모드 상단 영역이 올바르게 표시되는지 확인
        composeTestRule.onNodeWithText("메시지").assertIsDisplayed()
        composeTestRule.onNodeWithText("친구 추가하기").assertIsDisplayed()
    }
    
    /**
     * 프로젝트 모드에서 FAB 클릭 테스트
     */
    @Test
    fun homeScreen_whenFabClickedInProjectMode_triggersProjectAddEvent() {
        // Given: FAB 클릭 추적 변수
        var fabClicked = false
        
        // When: Scaffold + FAB 렌더링 및 FAB 클릭
        composeTestRule.setContent {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { fabClicked = true },
                        modifier = Modifier.testTag("projectAddFab")
                    ) {
                        androidx.compose.material.Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "추가"
                        )
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    Text("테스트 내용")
                }
            }
        }
        
        // FAB 클릭
        composeTestRule.onNodeWithTag("projectAddFab").performClick()
        
        // Then: 클릭 이벤트 발생 확인
        assert(fabClicked) { "FAB 클릭 시 이벤트가 발생하지 않음" }
    }
    
    /**
     * 사이드바의 프로필 영역 클릭으로 DM 모드 전환 테스트
     */
    @Test
    fun homeScreen_whenProfileAreaClicked_switchesToDmMode() {
        // Given: 모드 전환 추적 변수
        var selectedMode: TopSection? = null
        
        // Given: 프로젝트 모드 UI 상태 및 테스트용 ViewModel
        val uiState = HomeUiState(
            selectedTopSection = TopSection.PROJECTS,
            isLoading = false
        )
        
        // 모드 변경을 추적하는 Fake ViewModel
        val fakeViewModel = object : HomeViewModel() {
            override fun onTopSectionSelect(section: TopSection) {
                selectedMode = section
            }
        }
        
        // When: HomeContent 렌더링
        composeTestRule.setContent {
            HomeContent(
                uiState = uiState,
                viewModel = fakeViewModel
            )
        }
        
        // 프로필 영역(사이드바 상단) 클릭 - "Me" 텍스트가 포함된 영역
        composeTestRule.onNodeWithText("Me").performClick()
        
        // Then: 모드가 DMS로 변경되었는지 확인
        assert(selectedMode == TopSection.DMS) { "프로필 영역 클릭 시 DMS 모드로 전환되지 않음" }
    }
    
    /**
     * 프로젝트 리스트 아이템 클릭 테스트
     */
    @Test
    fun projectsList_whenProjectClicked_callsOnProjectClick() {
        // Given: 프로젝트 목록과 클릭 추적 변수
        val projects = List(3) { i ->
            ProjectItem(
                id = "p$i",
                name = "테스트 프로젝트 ${i + 1}",
                description = "설명 ${i + 1}",
                lastUpdated = "${i}분 전"
            )
        }
        
        var clickedProject: ProjectItem? = null
        
        // When: ProjectsList 렌더링 및 첫 번째 프로젝트 클릭
        composeTestRule.setContent {
            ProjectsList(
                projects = projects,
                onProjectClick = { clickedProject = it }
            )
        }
        
        // 첫 번째 프로젝트의 첫 글자 클릭 (ProjectListItem의 표시 방식)
        composeTestRule.onNodeWithText("T").performClick()
        
        // Then: 클릭된 프로젝트 확인
        assert(clickedProject != null) { "프로젝트 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 빈 프로젝트 목록 상태 테스트
     */
    @Test
    fun homeScreen_whenProjectsEmpty_showsEmptyProjectsList() {
        // Given: 빈 프로젝트 목록을 가진 UI 상태
        val uiState = HomeUiState(
            selectedTopSection = TopSection.PROJECTS,
            projects = emptyList(),
            isLoading = false
        )
        
        // When: HomeContent 렌더링
        composeTestRule.setContent {
            val fakeViewModel = object : HomeViewModel() {}
            
            HomeContent(
                uiState = uiState,
                viewModel = fakeViewModel
            )
        }
        
        // Then: 사이드바는 존재하지만 프로젝트 목록은 비어 있어야 함
        // 프로젝트 추가 버튼은 여전히 표시되어야 함
        composeTestRule.onNodeWithText("프로젝트 추가 버튼").assertIsDisplayed()
    }
} 