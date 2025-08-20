package com.example.feature_chat.service

import android.content.Context
import com.example.core_navigation.core.NavigationManger
import com.example.domain_repository.base.MessageRepository
import com.example.domain_usecase.provider.auth.AuthSessionUseCaseProvider
import com.example.domain_usecase.provider.chat.ChatUseCaseProvider
import com.example.domain_usecase.provider.dm.DMUseCaseProvider
import com.example.domain_usecase.provider.file.FileManagementUseCaseProvider
import com.example.domain_usecase.provider.project.ProjectMemberUseCaseProvider
import com.example.domain_usecase.provider.project.ProjectRoleUseCaseProvider
import com.example.domain_usecase.provider.user.UserUseCaseProvider
import com.example.feature_chat.queue.OfflineMessageQueue
import com.example.websocket.usecase.WebSocketUseCaseProvider
import com.example.websocket.usecase.SendMessageUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * ChatViewModel의 Service들을 제공하는 Provider 클래스
 * Hilt의 DI를 통해 필요한 Service들을 생성하여 제공합니다.
 */
class ChatServiceProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatUseCaseProvider: ChatUseCaseProvider,
    private val authSessionUseCaseProvider: AuthSessionUseCaseProvider,
    private val userUseCaseProvider: UserUseCaseProvider,
    private val fileUseCaseProvider: FileManagementUseCaseProvider,
    private val dmUseCaseProvider: DMUseCaseProvider,
    private val projectMemberUseCaseProvider: ProjectMemberUseCaseProvider,
    private val projectRoleUseCaseProvider: ProjectRoleUseCaseProvider,
    private val webSocketUseCaseProvider: WebSocketUseCaseProvider,
    private val offlineMessageQueue: OfflineMessageQueue,
    private val navigationManger: NavigationManger,
    private val messageRepository: MessageRepository,
    private val sendMessageUseCase: SendMessageUseCase,
) {
    
    /**
     * ChatViewModel에서 사용할 Service들을 묶어서 제공하는 데이터 클래스
     */
    data class ChatServices(
        val messageService: MessageService,
        val userProfileService: UserProfileService,
        val navigationService: NavigationService,
        val profileUpdates: kotlinx.coroutines.flow.StateFlow<Int>,
        val participantService: ParticipantService? = null, // For DM channels
        val memberService: MemberService? = null, // For project channels
        val roleService: RoleService? = null // For project channels
    )
    
    /**
     * 프로젝트 채널용 Service들을 생성하여 반환
     */
    fun createForProjectChannel(projectId: String, channelId: String): ChatServices {
        val userUseCases = userUseCaseProvider.createForUser()
        val fileUseCases = fileUseCaseProvider.create()

        val roomId = channelId  // 접두사 제거 - 단순히 channelId만 사용
        

        val userProfileService = UserProfileService(
            userUseCases = userUseCases,
            fileUseCases = fileUseCases
        )
        
        val messageService = MessageService(
            context = context,
            webSocketUseCaseProvider = webSocketUseCaseProvider,
            offlineMessageQueue = offlineMessageQueue,
            messageRepository = messageRepository,
            userProfileService = userProfileService,
            fileUseCases = fileUseCases,
            sendMessageUseCase = sendMessageUseCase,
            roomId = roomId,
            projectId = projectId
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
            messageService = messageService,
            userProfileService = userProfileService,
            navigationService = navigationService,
            profileUpdates = userProfileService.profileUpdates(),
            participantService = null, // Not needed for project channels
            memberService = memberService,
            roleService = roleService
        )
    }
    
    /**
     * DM 채널용 Service들을 생성하여 반환
     */
    fun createForDMChannel(channelId: String): ChatServices {
        val authUseCases = authSessionUseCaseProvider.create()
        val userUseCases = userUseCaseProvider.createForUser()
        val fileUseCases = fileUseCaseProvider.create()

        val roomId = channelId  // 접두사 제거 - 단순히 channelId만 사용
        
        val userProfileService = UserProfileService(
            userUseCases = userUseCases,
            fileUseCases = fileUseCases
        )
        
        val messageService = MessageService(
            context = context,
            webSocketUseCaseProvider = webSocketUseCaseProvider,
            offlineMessageQueue = offlineMessageQueue,
            messageRepository = messageRepository,
            userProfileService = userProfileService,
            fileUseCases = fileUseCases,
            sendMessageUseCase = sendMessageUseCase,
            roomId = roomId,
            projectId = null
        )
        
        val navigationService = NavigationService(navigationManger)
        
        val participantService = ParticipantService(
            dmUseCaseProvider = dmUseCaseProvider,
            channelId = channelId,
            authUseCases = authUseCases,
            userProfileService = userProfileService
        )
        
        return ChatServices(
            messageService = messageService,
            userProfileService = userProfileService,
            navigationService = navigationService,
            profileUpdates = userProfileService.profileUpdates(),
            participantService = participantService,
            memberService = null, // Not needed for DM channels
            roleService = null // Not needed for DM channels
        )
    }
}
