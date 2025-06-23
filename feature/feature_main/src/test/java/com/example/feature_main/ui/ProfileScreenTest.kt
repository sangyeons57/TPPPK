package com.example.feature_main.ui

import androidx.compose.material.icons.Icons
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/**
 * ProfileScreen UI 테스트
 *
 * 이 테스트는 ProfileScreen의 주요 UI 컴포넌트와 상호작용을 검증합니다.
 * Mockito를 사용하지 않고 상태 기반 테스트로 구현되었습니다.
 */
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * 프로필 정보 표시 테스트
     */
    @Test
    fun profileContent_whenProfileLoaded_displaysUserInfo() {
        // Given: 프로필 정보를 가진 UI 상태
        val profile = UserProfileData(
            userId = "test123",
            name = "테스트 사용자",
            email = "test@example.com",
            memo = "테스트 중입니다",
            profileImageUrl = null
        )
        
        // When: ProfileContent 렌더링
        composeTestRule.setContent {
            com.example.feature_profile.ui.ProfileContent(
                isLoading = false,
                profile = profile,
                onEditProfileImageClick = {},
                onEditStatusClick = {},
                onSettingsClick = {},
                onLogoutClick = {},
                onFriendsClick = {},
                onStatusClick = {}
            )
        }
        
        // Then: 프로필 정보가 올바르게, 표시되는지 확인
        composeTestRule.onNodeWithText("테스트 사용자").assertIsDisplayed()
        composeTestRule.onNodeWithText("테스트 중입니다").assertIsDisplayed()
    }
    
    /**
     * 프로필 로딩 상태 테스트
     */
    @Test
    fun profileContent_whenLoading_disablesInteractions() {
        // Given: 로딩 중인 UI 상태와 빈 프로필
        val profile = UserProfileData(
            userId = "test123",
            name = "테스트 사용자",
            email = "test@example.com",
            memo = "테스트 중입니다",
            profileImageUrl = null
        )
        
        var settingsClicked = false
        
        // When: ProfileContent 렌더링 (로딩 중 상태)
        composeTestRule.setContent {
            com.example.feature_profile.ui.ProfileContent(
                isLoading = true,
                profile = profile,
                onEditProfileImageClick = {},
                onEditStatusClick = {},
                onSettingsClick = { settingsClicked = true },
                onLogoutClick = {},
                onFriendsClick = {},
                onStatusClick = {}
            )
        }
        
        // 설정 메뉴 항목 클릭 시도
        composeTestRule.onNodeWithText("설정").performClick()
        
        // Then: 콜백이 호출되지 않아야 함 (로딩 중에는 비활성화)
        assert(!settingsClicked) { "로딩 중에는 메뉴 항목이 비활성화되어야 함" }
    }
    
    /**
     * 프로필 메뉴 아이템 테스트
     */
    @Test
    fun profileMenuItem_whenClicked_triggersCallback() {
        // Given: 클릭 콜백 확인용 변수
        var clicked = false
        
        // When: ProfileMenuItem 렌더링 및 클릭
        composeTestRule.setContent {
            com.example.feature_profile.ui.ProfileMenuItem(
                text = "테스트 메뉴",
                icon = Icons.Default.Settings,
                onClick = { clicked = true }
            )
        }
        
        // 메뉴 클릭
        composeTestRule.onNodeWithText("테스트 메뉴").performClick()
        
        // Then: 클릭 콜백이 호출되었는지 확인
        assert(clicked) { "ProfileMenuItem 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 로그아웃 버튼 테스트
     */
    @Test
    fun profileContent_whenLogoutClicked_triggersCallback() {
        // Given: 클릭 콜백 확인용 변수
        var logoutClicked = false
        
        // When: ProfileContent 렌더링 및 로그아웃 버튼 클릭
        composeTestRule.setContent {
            com.example.feature_profile.ui.ProfileContent(
                isLoading = false,
                profile = UserProfileData(
                    userId = "test123",
                    name = "테스트 사용자",
                    email = "test@example.com",
                    memo = "테스트 중입니다",
                    profileImageUrl = null
                ),
                onEditProfileImageClick = {},
                onEditStatusClick = {},
                onSettingsClick = {},
                onLogoutClick = { logoutClicked = true },
                onFriendsClick = {},
                onStatusClick = {}
            )
        }
        
        // 로그아웃 버튼 클릭
        composeTestRule.onNodeWithText("로그아웃").performClick()
        
        // Then: 로그아웃 콜백이 호출되었는지 확인
        assert(logoutClicked) { "로그아웃 버튼 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 상태 메시지 편집 버튼 테스트
     */
    @Test
    fun profileContent_whenEditStatusClicked_triggersCallback() {
        // Given: 클릭 콜백 확인용 변수
        var editStatusClicked = false
        
        // When: ProfileContent 렌더링 및 상태 메시지 편집 버튼 클릭
        composeTestRule.setContent {
            com.example.feature_profile.ui.ProfileContent(
                isLoading = false,
                profile = UserProfileData(
                    userId = "test123",
                    name = "테스트 사용자",
                    email = "test@example.com",
                    memo = "테스트 중입니다",
                    profileImageUrl = null
                ),
                onEditProfileImageClick = {},
                onEditStatusClick = { editStatusClicked = true },
                onSettingsClick = {},
                onLogoutClick = {},
                onFriendsClick = {},
                onStatusClick = {}
            )
        }
        
        // 상태 메시지 옆의 편집 버튼 클릭
        composeTestRule.onNodeWithContentDescription("상태 메시지 변경").performClick()
        
        // Then: 상태 메시지 편집 콜백이 호출되었는지 확인
        assert(editStatusClicked) { "상태 메시지 편집 버튼 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 프로필 이미지 편집 버튼 테스트
     */
    @Test
    fun profileContent_whenEditProfileImageClicked_triggersCallback() {
        // Given: 클릭 콜백 확인용 변수
        var editProfileImageClicked = false
        
        // When: ProfileContent 렌더링 및 프로필 이미지 편집 버튼 클릭
        composeTestRule.setContent {
            com.example.feature_profile.ui.ProfileContent(
                isLoading = false,
                profile = UserProfileData(
                    userId = "test123",
                    name = "테스트 사용자",
                    email = "test@example.com",
                    memo = "테스트 중입니다",
                    profileImageUrl = null
                ),
                onEditProfileImageClick = { editProfileImageClicked = true },
                onEditStatusClick = {},
                onSettingsClick = {},
                onLogoutClick = {},
                onFriendsClick = {},
                onStatusClick = {}
            )
        }
        
        // 프로필 이미지 편집 버튼 클릭
        composeTestRule.onNodeWithContentDescription("프로필 이미지 변경").performClick()
        
        // Then: 프로필 이미지 편집 콜백이 호출되었는지 확인
        assert(editProfileImageClicked) { "프로필 이미지 편집 버튼 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 설정 메뉴 테스트
     */
    @Test
    fun profileContent_whenSettingsClicked_triggersCallback() {
        // Given: 클릭 콜백 확인용 변수
        var settingsClicked = false
        
        // When: ProfileContent 렌더링 및 설정 메뉴 클릭
        composeTestRule.setContent {
            com.example.feature_profile.ui.ProfileContent(
                isLoading = false,
                profile = UserProfileData(
                    userId = "test123",
                    name = "테스트 사용자",
                    email = "test@example.com",
                    memo = "테스트 중입니다",
                    profileImageUrl = null
                ),
                onEditProfileImageClick = {},
                onEditStatusClick = {},
                onSettingsClick = { settingsClicked = true },
                onLogoutClick = {},
                onFriendsClick = {},
                onStatusClick = {}
            )
        }
        
        // 설정 메뉴 클릭
        composeTestRule.onNodeWithText("설정").performClick()
        
        // Then: 설정 콜백이 호출되었는지 확인
        assert(settingsClicked) { "설정 메뉴 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 친구 메뉴 테스트
     */
    @Test
    fun profileContent_whenFriendsClicked_triggersCallback() {
        // Given: 클릭 콜백 확인용 변수
        var friendsClicked = false
        
        // When: ProfileContent 렌더링 및 친구 메뉴 클릭
        composeTestRule.setContent {
            com.example.feature_profile.ui.ProfileContent(
                isLoading = false,
                profile = UserProfileData(
                    userId = "test123",
                    name = "테스트 사용자",
                    email = "test@example.com",
                    memo = "테스트 중입니다",
                    profileImageUrl = null
                ),
                onEditProfileImageClick = {},
                onEditStatusClick = {},
                onSettingsClick = {},
                onLogoutClick = {},
                onFriendsClick = { friendsClicked = true },
                onStatusClick = {}
            )
        }
        
        // 친구 메뉴 클릭
        composeTestRule.onNodeWithText("친구").performClick()
        
        // Then: 친구 콜백이 호출되었는지 확인
        assert(friendsClicked) { "친구 메뉴 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 상태 표시 메뉴 테스트
     */
    @Test
    fun profileContent_whenStatusClicked_triggersCallback() {
        // Given: 클릭 콜백 확인용 변수
        var statusClicked = false
        
        // When: ProfileContent 렌더링 및 상태 표시 메뉴 클릭
        composeTestRule.setContent {
            com.example.feature_profile.ui.ProfileContent(
                isLoading = false,
                profile = UserProfileData(
                    userId = "test123",
                    name = "테스트 사용자",
                    email = "test@example.com",
                    memo = "테스트 중입니다",
                    profileImageUrl = null
                ),
                onEditProfileImageClick = {},
                onEditStatusClick = {},
                onSettingsClick = {},
                onLogoutClick = {},
                onFriendsClick = {},
                onStatusClick = { statusClicked = true }
            )
        }
        
        // 상태 표시 메뉴 클릭
        composeTestRule.onNodeWithText("상태 표시").performClick()
        
        // Then: 상태 표시 콜백이 호출되었는지 확인
        assert(statusClicked) { "상태 표시 메뉴 클릭 시 콜백이 호출되지 않음" }
    }
    
    /**
     * 널 프로필 표시 테스트
     */
    @Test
    fun profileContent_whenProfileIsNull_displaysDefaultValues() {
        // When: ProfileContent를 null 프로필로 렌더링
        composeTestRule.setContent {
            com.example.feature_profile.ui.ProfileContent(
                isLoading = false,
                profile = null, // null 프로필
                onEditProfileImageClick = {},
                onEditStatusClick = {},
                onSettingsClick = {},
                onLogoutClick = {},
                onFriendsClick = {},
                onStatusClick = {}
            )
        }
        
        // Then: 기본값이 올바르게 표시되는지 확인
        composeTestRule.onNodeWithText("사용자 이름").assertIsDisplayed()
        composeTestRule.onNodeWithText("상태 메시지 없음").assertIsDisplayed()
    }
    
    /**
     * 컴포넌트 비활성화 테스트
     */
    @Test
    fun profileMenuItem_whenDisabled_isNotClickable() {
        // Given: 클릭 콜백 확인용 변수
        var clicked = false
        
        // When: 비활성화된 ProfileMenuItem 렌더링 및 클릭 시도
        composeTestRule.setContent {
            com.example.feature_profile.ui.ProfileMenuItem(
                text = "비활성화 메뉴",
                icon = Icons.Default.Settings,
                onClick = { clicked = true },
                enabled = false // 비활성화 상태
            )
        }
        
        // 메뉴 클릭 시도
        composeTestRule.onNodeWithText("비활성화 메뉴").performClick()
        
        // Then: 클릭 콜백이 호출되지 않아야 함
        assert(!clicked) { "비활성화된 ProfileMenuItem 클릭 시 콜백이 호출되지 않아야 함" }
    }
    
    /**
     * 모든 메뉴 항목 표시 테스트
     */
    @Test
    fun profileContent_displaysAllMenuItems() {
        // When: ProfileContent 렌더링
        composeTestRule.setContent {
            com.example.feature_profile.ui.ProfileContent(
                isLoading = false,
                profile = UserProfileData(
                    userId = "test123",
                    name = "테스트 사용자",
                    email = "test@example.com",
                    memo = "테스트 중입니다",
                    profileImageUrl = null
                ),
                onEditProfileImageClick = {},
                onEditStatusClick = {},
                onSettingsClick = {},
                onLogoutClick = {},
                onFriendsClick = {},
                onStatusClick = {}
            )
        }
        
        // Then: 모든 메뉴 항목이 표시되는지 확인
        listOf("상태 표시", "친구", "설정", "로그아웃").forEach { menuText ->
            composeTestRule.onNodeWithText(menuText).assertIsDisplayed()
        }
    }
} 