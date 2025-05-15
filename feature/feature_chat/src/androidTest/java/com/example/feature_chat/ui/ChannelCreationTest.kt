package com.example.feature_chat.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.domain.model.Channel
import com.example.domain.repository.ChannelRepository
import com.example.feature_chat.viewmodel.ChannelListViewModel
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
 * 채널 생성 및 참여에 관한 UI 통합 테스트
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ChannelCreationTest {

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
    fun createNewChannel_shouldAppearInChannelList() {
        // Given - Launch the channel creation screen
        composeTestRule.onNodeWithText("채널").performClick()
        composeTestRule.onNodeWithContentDescription("채널 추가").performClick()

        // When - Fill in channel details and create
        val channelName = "테스트 채널 ${System.currentTimeMillis()}"
        composeTestRule.onNodeWithText("채널 이름").performTextInput(channelName)
        composeTestRule.onNodeWithText("채널 설명").performTextInput("통합 테스트용 채널")
        
        composeTestRule.onNodeWithText("공개").performClick()
        composeTestRule.onNodeWithText("생성").performClick()

        // Then - Channel should appear in the channel list
        composeTestRule.waitUntil(5000) {
            composeTestRule
                .onAllNodesWithText(channelName)
                .fetchSemanticsNodes().size == 1
        }

        // Verify channel exists in repository
        runBlocking {
            val channels = channelRepository.getUserChannels(
                composeTestRule.activity.viewModel.getCurrentUserId()
            ).first().getOrNull() ?: emptyList()
            
            assert(channels.any { it.name == channelName }) {
                "새로 생성한 채널이 repository에 존재하지 않습니다."
            }
        }
    }

    @Test
    fun joinExistingChannel_shouldShowInChannelList() {
        // Given - Create a channel to join
        val channelName = "참여 테스트 채널 ${System.currentTimeMillis()}"
        var channelId: String
        
        runBlocking {
            // Create a public channel
            val result = channelRepository.createChannel(
                name = channelName,
                description = "참여 테스트용 채널",
                ownerId = composeTestRule.activity.viewModel.getCurrentUserId(),
                participantIds = listOf(composeTestRule.activity.viewModel.getCurrentUserId()),
                metadata = mapOf("isPublic" to true)
            )
            
            channelId = result.getOrNull()?.id ?: ""
            assert(channelId.isNotEmpty()) { "테스트용 채널 생성 실패" }
        }
        
        // When - Navigate to channel browser and join the channel
        composeTestRule.onNodeWithText("채널").performClick()
        composeTestRule.onNodeWithText("채널 찾기").performClick()
        
        // Search for the channel
        composeTestRule.onNodeWithText("채널 검색").performTextInput(channelName)
        
        // Join channel
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(channelName).fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithText(channelName).performClick()
        composeTestRule.onNodeWithText("참여").performClick()
        
        // Then - Go back to channel list and verify the channel appears
        composeTestRule.onNodeWithContentDescription("뒤로 가기").performClick()
        
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(channelName).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Verify user is now a participant in the channel
        runBlocking {
            val channel = channelRepository.getChannel(channelId).getOrNull()
            assert(channel != null && channel.participantIds.contains(
                composeTestRule.activity.viewModel.getCurrentUserId()
            )) {
                "유저가 채널 참여자 목록에 추가되지 않았습니다."
            }
        }
    }
    
    @Test
    fun leaveChannel_shouldRemoveFromChannelList() {
        // Given - Create a channel and ensure we're in it
        val channelName = "퇴장 테스트 채널 ${System.currentTimeMillis()}"
        var channelId: String
        
        runBlocking {
            // Create a channel
            val result = channelRepository.createChannel(
                name = channelName,
                description = "퇴장 테스트용 채널",
                ownerId = composeTestRule.activity.viewModel.getCurrentUserId(),
                participantIds = listOf(composeTestRule.activity.viewModel.getCurrentUserId()),
                metadata = null
            )
            
            channelId = result.getOrNull()?.id ?: ""
            assert(channelId.isNotEmpty()) { "테스트용 채널 생성 실패" }
        }
        
        // Refresh channel list
        composeTestRule.onNodeWithText("채널").performClick()
        
        // Wait for channel to appear
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(channelName).fetchSemanticsNodes().isNotEmpty()
        }
        
        // When - Long press on the channel and select leave
        composeTestRule.onNodeWithText(channelName).performLongClick()
        composeTestRule.onNodeWithText("채널 나가기").performClick()
        composeTestRule.onNodeWithText("확인").performClick()
        
        // Then - Channel should disappear from list
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText(channelName).fetchSemanticsNodes().isEmpty()
        }
        
        // Verify user is removed from channel participants
        runBlocking {
            val channel = channelRepository.getChannel(channelId).getOrNull()
            assert(channel != null && !channel.participantIds.contains(
                composeTestRule.activity.viewModel.getCurrentUserId()
            )) {
                "유저가 채널 참여자 목록에서 제거되지 않았습니다."
            }
        }
    }
} 