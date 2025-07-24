package com.example.feature_chat.service

import com.example.core_navigation.core.NavigationManger
import com.example.domain.provider.auth.AuthSessionUseCaseProvider
import com.example.domain.provider.chat.ChatUseCaseProvider
import com.example.domain.provider.dm.DMUseCaseProvider
import com.example.domain.provider.file.FileManagementUseCaseProvider
import com.example.domain.provider.project.ProjectMemberUseCaseProvider
import com.example.domain.provider.project.ProjectRoleUseCaseProvider
import com.example.domain.provider.user.UserUseCaseProvider
import com.example.feature_chat.queue.OfflineMessageQueue
import com.example.feature_chat.websocket.ChatWebSocketClient
import com.example.core_common.config.FeatureFlags
import com.example.data.cache.ChatCacheManager
import javax.inject.Inject

/**
 * ChatViewModel의 Service들을 제공하는 Provider 클래스
 * Hilt의 DI를 통해 필요한 Service들을 생성하여 제공합니다.
 */
class ChatServiceProvider @Inject constructor(
    private val chatUseCaseProvider: ChatUseCaseProvider,
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider,
    private val fileUseCaseProvider: FileManagementUseCaseProvider,
    private val dmUseCaseProvider: DMUseCaseProvider,
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider,
    private val projectRoleUseCaseProvider: ProjectRoleUseCaseProvider,
    private val webSocketClient: ChatWebSocketClient,
    private val offlineMessageQueue: OfflineMessageQueue,
    private val navigationManger: NavigationManger,
    private val chatCacheManager: ChatCacheManager? = null // 선택적 의존성으로 점진적 롤아웃 지원
) {
    
    /**
     * ChatViewModel에서 사용할 Service들을 묶어서 제공하는 데이터 클래스
     */
    data class ChatServices(
        val authenticationService: AuthenticationService,
        val messageService: MessageService,
        val connectionService: ConnectionService,
        val userProfileService: UserProfileService,
        val navigationService: NavigationService,
        val participantService: ParticipantService? = null, // For DM channels
        val memberService: MemberService? = null, // For project channels
        val roleService: RoleService? = null // For project channels
    )
    
    /**
     * 프로젝트 채널용 Service들을 생성하여 반환
     */
    fun createForProjectChannel(projectId: String, channelId: String): ChatServices {
        val chatUseCases = chatUseCaseProvider.createForChannel(projectId, channelId)
        val authUseCases = authSessionUseCaseProvider.create()
        val userUseCases = userUseCaseProvider.createForUser()
        val fileUseCases = fileUseCaseProvider.create()
        
        val roomId = "chat_room_$channelId"
        
        val authenticationService = AuthenticationService(
            authUseCases = authUseCases,
            webSocketClient = webSocketClient,
            roomId = roomId
        )
        
        val userProfileService = UserProfileService(
            userUseCases = userUseCases,
            fileUseCases = fileUseCases
        )
        
        val messageService = MessageService(
            chatUseCases = chatUseCases,
            webSocketClient = webSocketClient,
            offlineMessageQueue = offlineMessageQueue,
            userProfileService = userProfileService,
            roomId = roomId,
            chatCacheManager = if (FeatureFlags.ENABLE_LOCAL_CHAT_CACHE) chatCacheManager else null
        )
        
        val connectionService = ConnectionService(
            webSocketClient = webSocketClient,
            offlineMessageQueue = offlineMessageQueue
        )
        
        val navigationService = NavigationService(navigationManger)
        
        val memberService = MemberService(
            projectMemberUseCaseProvider = projectMemberUseCaseProvider,
            projectId = projectId,
            userProfileService = userProfileService
        )
        
        val roleService = RoleService(
            projectRoleUseCaseProvider = projectRoleUseCaseProvider,
            projectId = projectId
        )
        
        return ChatServices(
            authenticationService = authenticationService,
            messageService = messageService,
            connectionService = connectionService,
            userProfileService = userProfileService,
            navigationService = navigationService,
            participantService = null, // Not needed for project channels
            memberService = memberService,
            roleService = roleService
        )
    }
    
    /**
     * DM 채널용 Service들을 생성하여 반환
     */
    fun createForDMChannel(channelId: String): ChatServices {
        val chatUseCases = chatUseCaseProvider.createForDMChannel(channelId)
        val authUseCases = authSessionUseCaseProvider.create()
        val userUseCases = userUseCaseProvider.createForUser()
        val fileUseCases = fileUseCaseProvider.create()
        
        val roomId = "chat_room_$channelId"
        
        val authenticationService = AuthenticationService(
            authUseCases = authUseCases,
            webSocketClient = webSocketClient,
            roomId = roomId
        )
        
        val userProfileService = UserProfileService(
            userUseCases = userUseCases,
            fileUseCases = fileUseCases
        )
        
        val messageService = MessageService(
            chatUseCases = chatUseCases,
            webSocketClient = webSocketClient,
            offlineMessageQueue = offlineMessageQueue,
            userProfileService = userProfileService,
            roomId = roomId,
            chatCacheManager = if (FeatureFlags.ENABLE_LOCAL_CHAT_CACHE) chatCacheManager else null
        )
        
        val connectionService = ConnectionService(
            webSocketClient = webSocketClient,
            offlineMessageQueue = offlineMessageQueue
        )
        
        val navigationService = NavigationService(navigationManger)
        
        val participantService = ParticipantService(
            dmUseCaseProvider = dmUseCaseProvider,
            channelId = channelId,
            userProfileService = userProfileService
        )
        
        return ChatServices(
            authenticationService = authenticationService,
            messageService = messageService,
            connectionService = connectionService,
            userProfileService = userProfileService,
            navigationService = navigationService,
            participantService = participantService,
            memberService = null, // Not needed for DM channels
            roleService = null // Not needed for DM channels
        )
    }
}