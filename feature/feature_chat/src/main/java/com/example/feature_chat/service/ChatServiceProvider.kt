package com.example.feature_chat.service

import com.example.core_navigation.core.NavigationManger
import com.example.domain.provider.auth.AuthSessionUseCaseProvider
import com.example.domain.provider.chat.ChatUseCaseProvider
import com.example.domain.provider.file.FileManagementUseCaseProvider
import com.example.domain.provider.user.UserUseCaseProvider
import com.example.feature_chat.queue.OfflineMessageQueue
import com.example.feature_chat.websocket.ChatWebSocketClient
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
    private val webSocketClient: ChatWebSocketClient,
    private val offlineMessageQueue: OfflineMessageQueue,
    private val navigationManger: NavigationManger
) {
    
    /**
     * ChatViewModel에서 사용할 Service들을 묶어서 제공하는 데이터 클래스
     */
    data class ChatServices(
        val authenticationService: AuthenticationService,
        val messageService: MessageService,
        val connectionService: ConnectionService,
        val userProfileService: UserProfileService,
        val navigationService: NavigationService
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
            roomId = roomId
        )
        
        val connectionService = ConnectionService(
            webSocketClient = webSocketClient,
            offlineMessageQueue = offlineMessageQueue
        )
        
        val navigationService = NavigationService(navigationManger)
        
        return ChatServices(
            authenticationService = authenticationService,
            messageService = messageService,
            connectionService = connectionService,
            userProfileService = userProfileService,
            navigationService = navigationService
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
            roomId = roomId
        )
        
        val connectionService = ConnectionService(
            webSocketClient = webSocketClient,
            offlineMessageQueue = offlineMessageQueue
        )
        
        val navigationService = NavigationService(navigationManger)
        
        return ChatServices(
            authenticationService = authenticationService,
            messageService = messageService,
            connectionService = connectionService,
            userProfileService = userProfileService,
            navigationService = navigationService
        )
    }
}