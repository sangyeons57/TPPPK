package com.example.feature_settings.ui

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.domain.model.User
import com.example.feature_settings.viewmodel.EditProfileUiState
import com.example.feature_settings.viewmodel.EditProfileViewModel
import org.junit.Rule
import org.junit.Test

/**
 * EditProfileScreen UI 테스트
 *
 * 이 테스트는 EditProfileScreen의 주요 UI 컴포넌트와 상호작용을 검증합니다.
 * Mockito를 사용하지 않고 상태 기반 테스트로 구현되었습니다.
 */
class EditProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * 프로필 정보 표시 테스트
     */
    @Test
    fun editProfileContent_whenProfileLoaded_displaysUserInfo() {
        // Given: 사용자 프로필 정보를 가진 UI 상태
        val user = User(
            userId = "test123",
            email = "test@example.com",
            name = "테스트 사용자",
            profileImageUrl = null,
            statusMessage = "테스트 중입니다"
        )
        
        val uiState = EditProfileUiState(
            user = user,
            isLoading = false,
            isUploading = false
        )
        
        // When: EditProfileContent 렌더링
        composeTestRule.setContent {
            EditProfileContent(
                uiState = uiState,
                onSelectImageClick = {},
                onRemoveImageClick = {},
                onChangeNameClick = {},
                onChangeStatusClick = {}
            )
        }
        
        // Then: 사용자 정보가 올바르게 표시되는지 확인
        composeTestRule.onNodeWithText("테스트 사용자").assertIsDisplayed()
        composeTestRule.onNodeWithText("test@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("테스트 중입니다").assertIsDisplayed()
    }
    
    /**
     * 이메일 정보가 수정 불가능한지 테스트
     */
    @Test
    fun profileInfoRow_emailIsNotEditable() {
        // Given: 사용자 프로필 정보를 가진 UI 상태
        val user = User(
            userId = "test123",
            email = "test@example.com",
            name = "테스트 사용자",
            profileImageUrl = null,
            statusMessage = "테스트 중입니다"
        )
        
        val uiState = EditProfileUiState(
            user = user,
            isLoading = false,
            isUploading = false
        )
        
        // When: EditProfileContent 렌더링
        composeTestRule.setContent {
            EditProfileContent(
                uiState = uiState,
                onSelectImageClick = {},
                onRemoveImageClick = {},
                onChangeNameClick = {},
                onChangeStatusClick = {}
            )
        }
        
        // Then: 이메일 행에는 수정 아이콘이 표시되지 않아야 함
        composeTestRule.onNodeWithText("이메일")
            .onParent()
            .onChildren()
            .filterToOne(hasText("test@example.com"))
            .onParent()
            .onChildren()
            .filter(hasContentDescription("이메일 변경"))
            .assertDoesNotExist()
    }
    
    /**
     * 이름 정보가 수정 가능한지 테스트
     */
    @Test
    fun profileInfoRow_nameIsEditable() {
        // Given: 사용자 프로필 정보를 가진 UI 상태와 클릭 콜백 모니터링 변수
        val user = User(
            userId = "test123",
            email = "test@example.com",
            name = "테스트 사용자",
            profileImageUrl = null,
            statusMessage = "테스트 중입니다"
        )
        
        var changeNameClicked = false
        
        val uiState = EditProfileUiState(
            user = user,
            isLoading = false,
            isUploading = false
        )
        
        // When: EditProfileContent 렌더링
        composeTestRule.setContent {
            EditProfileContent(
                uiState = uiState,
                onSelectImageClick = {},
                onRemoveImageClick = {},
                onChangeNameClick = { changeNameClicked = true },
                onChangeStatusClick = {}
            )
        }
        
        // 이름 항목 클릭
        composeTestRule.onNodeWithText("이름")
            .onParent()
            .performClick()
        
        // Then: 이름 변경 콜백이 호출되었는지 확인
        assert(changeNameClicked) { "이름 행 클릭 시 onChangeNameClick 콜백이 호출되지 않음" }
    }
    
    /**
     * 상태 메시지 수정 가능 테스트
     */
    @Test
    fun profileInfoRow_statusMessageIsEditable() {
        // Given: 사용자 프로필 정보를 가진 UI 상태와 클릭 콜백 모니터링 변수
        val user = User(
            userId = "test123",
            email = "test@example.com",
            name = "테스트 사용자",
            profileImageUrl = null,
            statusMessage = "테스트 중입니다"
        )
        
        var changeStatusClicked = false
        
        val uiState = EditProfileUiState(
            user = user,
            isLoading = false,
            isUploading = false
        )
        
        // When: EditProfileContent 렌더링
        composeTestRule.setContent {
            EditProfileContent(
                uiState = uiState,
                onSelectImageClick = {},
                onRemoveImageClick = {},
                onChangeNameClick = {},
                onChangeStatusClick = { changeStatusClicked = true }
            )
        }
        
        // 상태 메시지 항목 클릭
        composeTestRule.onNodeWithText("상태 메시지")
            .onParent()
            .performClick()
        
        // Then: 상태 메시지 변경 콜백이 호출되었는지 확인
        assert(changeStatusClicked) { "상태 메시지 행 클릭 시 onChangeStatusClick 콜백이 호출되지 않음" }
    }
    
    /**
     * 프로필 이미지 선택 버튼 테스트
     */
    @Test
    fun editProfileContent_whenSelectImageClicked_triggersCallback() {
        // Given: 사용자 프로필 정보를 가진 UI 상태와 클릭 콜백 모니터링 변수
        val user = User(
            userId = "test123",
            email = "test@example.com",
            name = "테스트 사용자",
            profileImageUrl = null,
            statusMessage = "테스트 중입니다"
        )
        
        var selectImageClicked = false
        
        val uiState = EditProfileUiState(
            user = user,
            isLoading = false,
            isUploading = false
        )
        
        // When: EditProfileContent 렌더링
        composeTestRule.setContent {
            EditProfileContent(
                uiState = uiState,
                onSelectImageClick = { selectImageClicked = true },
                onRemoveImageClick = {},
                onChangeNameClick = {},
                onChangeStatusClick = {}
            )
        }
        
        // 이미지 선택 버튼 클릭
        composeTestRule.onNodeWithContentDescription("프로필 이미지 변경").performClick()
        
        // Then: 이미지 선택 콜백이 호출되었는지 확인
        assert(selectImageClicked) { "이미지 선택 버튼 클릭 시 onSelectImageClick 콜백이 호출되지 않음" }
    }
    
    /**
     * 프로필 이미지 제거 버튼 테스트
     */
    @Test
    fun editProfileContent_whenRemoveImageClicked_triggersCallback() {
        // Given: 프로필 이미지가 있는 사용자 정보와 클릭 콜백 모니터링 변수
        val user = User(
            userId = "test123",
            email = "test@example.com",
            name = "테스트 사용자",
            profileImageUrl = "https://example.com/profile.jpg", // 프로필 이미지 URL 있음
            statusMessage = "테스트 중입니다"
        )
        
        var removeImageClicked = false
        
        val uiState = EditProfileUiState(
            user = user,
            isLoading = false,
            isUploading = false
        )
        
        // When: EditProfileContent 렌더링
        composeTestRule.setContent {
            EditProfileContent(
                uiState = uiState,
                onSelectImageClick = {},
                onRemoveImageClick = { removeImageClicked = true },
                onChangeNameClick = {},
                onChangeStatusClick = {}
            )
        }
        
        // 이미지 제거 버튼 클릭
        composeTestRule.onNodeWithText("프로필 이미지 제거").performClick()
        
        // Then: 이미지 제거 콜백이 호출되었는지 확인
        assert(removeImageClicked) { "이미지 제거 버튼 클릭 시 onRemoveImageClick 콜백이 호출되지 않음" }
    }
    
    /**
     * 프로필 업로드 중 상태 표시 테스트
     */
    @Test
    fun editProfileContent_whenUploading_showsProgressIndicator() {
        // Given: 업로드 중인 UI 상태
        val user = User(
            userId = "test123",
            email = "test@example.com",
            name = "테스트 사용자",
            profileImageUrl = null,
            statusMessage = "테스트 중입니다"
        )
        
        val uiState = EditProfileUiState(
            user = user,
            isLoading = false,
            isUploading = true // 업로드 중 상태
        )
        
        // When: EditProfileContent 렌더링
        composeTestRule.setContent {
            EditProfileContent(
                uiState = uiState,
                onSelectImageClick = {},
                onRemoveImageClick = {},
                onChangeNameClick = {},
                onChangeStatusClick = {}
            )
        }
        
        // Then: 업로드 중 인디케이터가 표시되는지 확인
        composeTestRule.onNode(hasRole(androidx.compose.ui.semantics.Role.ProgressBar)).assertIsDisplayed()
    }
    
    /**
     * 이미지가 없을 때 이미지 제거 버튼 비활성화 테스트
     */
    @Test
    fun editProfileContent_whenNoProfileImage_removeButtonIsDisabled() {
        // Given: 프로필 이미지가 없는 사용자 정보
        val user = User(
            userId = "test123",
            email = "test@example.com",
            name = "테스트 사용자",
            profileImageUrl = null, // 프로필 이미지 없음
            statusMessage = "테스트 중입니다"
        )
        
        val uiState = EditProfileUiState(
            user = user,
            isLoading = false,
            isUploading = false
        )
        
        // When: EditProfileContent 렌더링
        composeTestRule.setContent {
            EditProfileContent(
                uiState = uiState,
                onSelectImageClick = {},
                onRemoveImageClick = {},
                onChangeNameClick = {},
                onChangeStatusClick = {}
            )
        }
        
        // Then: 이미지 제거 버튼이 비활성화되어 있는지 확인
        composeTestRule.onNodeWithText("프로필 이미지 제거").assertIsNotEnabled()
    }
} 