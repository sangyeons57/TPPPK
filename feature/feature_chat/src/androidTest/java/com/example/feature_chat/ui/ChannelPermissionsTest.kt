package com.example.feature_chat.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.domain.repository.ChannelRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * 채널 권한 및 관리 기능에 관한 UI 통합 테스트
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ChannelPermissionsTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestChannelActivity>()

    @Inject
    lateinit var channelRepository: ChannelRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun changeChannelName_asOwner_shouldUpdateChannelName() {
        // Given - Create a test channel as owner
        val initialName = "권한 테스트 채널 ${System.currentTimeMillis()}"
        val updatedName = "이름 변경된 채널 ${System.currentTimeMillis()}"
        var channelId: String
        
        runBlocking {
            val result = channelRepository.createChannel(
                name = initialName,
                description = "권한 테스트용 채널",
                ownerId = composeTestRule.activity.viewModel.getCurrentUserId(),
                participantIds = listOf(composeTestRule.activity.viewModel.getCurrentUserId()),
                metadata = null
            )
            
            channelId = result.getOrNull()?.id ?: ""
            assert(channelId.isNotEmpty()) { "테스트용 채널 생성 실패" }
        }
        
        // Navigate to channel list and refresh
        composeTestRule.onNodeWithText("채널").performClick()
        
        // Wait for channel to appear
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(initialName).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Navigate to channel
        composeTestRule.onNodeWithText(initialName).performClick()
        
        // When - Open channel settings and change name
        composeTestRule.onNodeWithContentDescription("채널 설정").performClick()
        composeTestRule.onNodeWithText("채널 이름 변경").performClick()
        
        // Clear input and type new name
        composeTestRule.onNode(hasText(initialName) and hasSetTextAction()).performTextClearance()
        composeTestRule.onNode(hasSetTextAction()).performTextInput(updatedName)
        composeTestRule.onNodeWithText("저장").performClick()
        
        // Then - Channel name should be updated
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(updatedName).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Verify channel name was updated in repository
        runBlocking {
            val channel = channelRepository.getChannel(channelId).getOrNull()
            assert(channel?.name == updatedName) {
                "채널 이름이 업데이트되지 않았습니다."
            }
        }
    }

    @Test
    fun addModerator_asOwner_shouldUpdatePermissions() {
        // Given - Create a test channel as owner
        val channelName = "관리자 테스트 채널 ${System.currentTimeMillis()}"
        val testModeratorId = "test_moderator_id" // Normally would be a real user ID
        var channelId: String
        
        runBlocking {
            val result = channelRepository.createChannel(
                name = channelName,
                description = "권한 테스트용 채널",
                ownerId = composeTestRule.activity.viewModel.getCurrentUserId(),
                participantIds = listOf(
                    composeTestRule.activity.viewModel.getCurrentUserId(),
                    testModeratorId
                ),
                metadata = mapOf(
                    "permissions" to mapOf(
                        "moderators" to listOf<String>() // Empty initially
                    )
                )
            )
            
            channelId = result.getOrNull()?.id ?: ""
            assert(channelId.isNotEmpty()) { "테스트용 채널 생성 실패" }
        }
        
        // Navigate to channel list
        composeTestRule.onNodeWithText("채널").performClick()
        
        // Wait for channel to appear
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(channelName).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Navigate to channel
        composeTestRule.onNodeWithText(channelName).performClick()
        
        // When - Open channel settings and add moderator
        composeTestRule.onNodeWithContentDescription("채널 설정").performClick()
        composeTestRule.onNodeWithText("권한 관리").performClick()
        composeTestRule.onNodeWithText("참여자 권한 설정").performClick()
        
        // Find the test user and promote to moderator
        composeTestRule.onNodeWithText("테스트 모더레이터").performClick()
        composeTestRule.onNodeWithText("권한 변경").performClick()
        composeTestRule.onNodeWithText("채널 관리자").performClick()
        composeTestRule.onNodeWithText("저장").performClick()
        
        // Go back to channel
        composeTestRule.onNodeWithContentDescription("뒤로 가기").performClick()
        composeTestRule.onNodeWithContentDescription("뒤로 가기").performClick()
        
        // Then - Verify moderator was added in repository
        runBlocking {
            val channel = channelRepository.getChannel(channelId).getOrNull()
            val metadata = channel?.metadata as? Map<*, *>
            val permissions = metadata?.get("permissions") as? Map<*, *>
            val moderators = permissions?.get("moderators") as? List<*>
            
            assert(moderators?.contains(testModeratorId) == true) {
                "사용자가 채널 관리자로 추가되지 않았습니다."
            }
        }
    }

    @Test
    fun makeChannelPrivate_asOwner_shouldUpdateVisibility() {
        // Given - Create a public test channel as owner
        val channelName = "공개 설정 테스트 채널 ${System.currentTimeMillis()}"
        var channelId: String
        
        runBlocking {
            val result = channelRepository.createChannel(
                name = channelName,
                description = "공개 설정 테스트용 채널",
                ownerId = composeTestRule.activity.viewModel.getCurrentUserId(),
                participantIds = listOf(composeTestRule.activity.viewModel.getCurrentUserId()),
                metadata = mapOf("isPublic" to true)
            )
            
            channelId = result.getOrNull()?.id ?: ""
            assert(channelId.isNotEmpty()) { "테스트용 채널 생성 실패" }
        }
        
        // Navigate to channel list
        composeTestRule.onNodeWithText("채널").performClick()
        
        // Wait for channel to appear
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(channelName).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Navigate to channel
        composeTestRule.onNodeWithText(channelName).performClick()
        
        // When - Open channel settings and change visibility to private
        composeTestRule.onNodeWithContentDescription("채널 설정").performClick()
        composeTestRule.onNodeWithText("채널 공개 설정").performClick()
        composeTestRule.onNodeWithText("비공개").performClick()
        composeTestRule.onNodeWithText("저장").performClick()
        
        // Then - Verify channel visibility was updated in repository
        runBlocking {
            val channel = channelRepository.getChannel(channelId).getOrNull()
            val metadata = channel?.metadata as? Map<*, *>
            val isPublic = metadata?.get("isPublic") as? Boolean
            
            assert(isPublic == false) {
                "채널이 비공개로 변경되지 않았습니다."
            }
        }
    }

    @Test
    fun createChannelCategory_asOwner_shouldAddCategory() {
        // Given - Create a project channel as owner
        val projectId = "test_project_id"
        val channelName = "카테고리 테스트 채널 ${System.currentTimeMillis()}"
        val categoryName = "테스트 카테고리 ${System.currentTimeMillis()}"
        var channelId: String
        
        runBlocking {
            val result = channelRepository.createChannel(
                name = channelName,
                description = "카테고리 테스트용 채널",
                ownerId = composeTestRule.activity.viewModel.getCurrentUserId(),
                participantIds = listOf(composeTestRule.activity.viewModel.getCurrentUserId()),
                metadata = mapOf(
                    "source" to "project",
                    "projectId" to projectId
                )
            )
            
            channelId = result.getOrNull()?.id ?: ""
            assert(channelId.isNotEmpty()) { "테스트용 채널 생성 실패" }
        }
        
        // Navigate to channel list
        composeTestRule.onNodeWithText("채널").performClick()
        
        // Wait for channel to appear
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(channelName).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Navigate to project settings
        composeTestRule.onNodeWithText("프로젝트").performClick()
        composeTestRule.onNodeWithText("테스트 프로젝트").performClick()
        composeTestRule.onNodeWithText("프로젝트 설정").performClick()
        
        // When - Create a new category
        composeTestRule.onNodeWithText("채널 카테고리").performClick()
        composeTestRule.onNodeWithText("카테고리 추가").performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput(categoryName)
        composeTestRule.onNodeWithText("생성").performClick()
        
        // Then - Category should appear in the list
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(categoryName).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Move channel to category
        composeTestRule.onNodeWithText(categoryName).performClick()
        composeTestRule.onNodeWithText("채널 추가").performClick()
        composeTestRule.onNodeWithText(channelName).performClick()
        composeTestRule.onNodeWithText("추가").performClick()
        
        // Verify channel is in the category
        runBlocking {
            // In a real implementation, we would check the project_categories collection
            // For this test, we'll just verify the channel metadata was updated
            val channel = channelRepository.getChannel(channelId).getOrNull()
            val metadata = channel?.metadata as? Map<*, *>
            
            assert(metadata?.containsKey("categoryId") == true) {
                "채널이 카테고리에 추가되지 않았습니다."
            }
        }
    }
} 