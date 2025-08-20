package com.example.domain_usecase.provider.chat

import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.MessageRepository
import com.example.domain_usecase.usecase.message.DeleteMessageUseCase
import com.example.domain_usecase.usecase.message.EditMessageUseCase
import com.example.domain_usecase.usecase.message.FetchNewerMessagesUseCase
import com.example.domain_usecase.usecase.message.FetchPastMessagesUseCase
import com.example.domain_usecase.usecase.message.GetMessagesStreamUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 채팅 채널 타입을 구분하는 열거형
 */
enum class ChannelType {
    PROJECT_CHANNEL,
    DM_CHANNEL
}

/**
 * 채팅 및 메시지 관련 UseCase들을 제공하는 Provider
 *
 * 프로젝트 채널과 DM 채널 모두에서 메시지 전송, 수정, 삭제, 실시간 메시지 스트림 등의 기능을 지원합니다.
 * 두 채널 타입 모두 동일한 Message 엔티티를 서브컬렉션으로 사용하므로 통합된 UseCase를 제공합니다.
 *
 * 사용 예시:
 * - 프로젝트 채널: chatUseCaseProvider.createForChannel(projectId, channelId)
 * - DM 채널: chatUseCaseProvider.createForDMChannel(dmChannelId)
 * - 타입 기반: chatUseCaseProvider.createForChannel(ChannelType.PROJECT_CHANNEL, channelId, projectId)
 */
@Singleton
class ChatUseCaseProvider @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository
) {

    /**
     * 특정 프로젝트 채널의 채팅 관련 UseCase들을 생성합니다.
     *
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 채팅 관련 UseCase 그룹
     */
    fun createForChannel(projectId: String, channelId: String): ChatUseCases {
        val collectionPath = CollectionPath.projectChannelMessages(projectId, channelId)

        messageRepository.setCollection(collectionPath)

        return ChatUseCases(
            editMessageUseCase = EditMessageUseCase(this.messageRepository),
            deleteMessageUseCase = DeleteMessageUseCase(this.messageRepository),
            getMessagesStreamUseCase = GetMessagesStreamUseCase(this.messageRepository),
            fetchPastMessagesUseCase = FetchPastMessagesUseCase(this.messageRepository),
            fetchNewerMessagesUseCase = FetchNewerMessagesUseCase(this.messageRepository)
        )
    }

    /**
     * 특정 DM 채널의 채팅 관련 UseCase들을 생성합니다.
     *
     * @param dmChannelId DM 채널 ID
     * @return 채팅 관련 UseCase 그룹
     */
    fun createForDMChannel(dmChannelId: String): ChatUseCases {
        val collectionPath = CollectionPath.dmChannelMessages(dmChannelId)

        messageRepository.setCollection(collectionPath)

        return ChatUseCases(
            editMessageUseCase = EditMessageUseCase(this.messageRepository),
            deleteMessageUseCase = DeleteMessageUseCase(this.messageRepository),
            getMessagesStreamUseCase = GetMessagesStreamUseCase(this.messageRepository),
            fetchPastMessagesUseCase = FetchPastMessagesUseCase(this.messageRepository),
            fetchNewerMessagesUseCase = FetchNewerMessagesUseCase(this.messageRepository)
        )
    }

    /**
     * 채널 타입에 따라 적절한 채팅 관련 UseCase들을 생성합니다.
     *
     * @param channelType 채널 타입 (PROJECT_CHANNEL 또는 DM_CHANNEL)
     * @param channelId 채널 ID (프로젝트 채널의 경우 채널 ID, DM 채널의 경우 DM 채널 ID)
     * @param projectId 프로젝트 ID (PROJECT_CHANNEL 타입인 경우에만 필요)
     * @return 채팅 관련 UseCase 그룹
     * @throws IllegalArgumentException PROJECT_CHANNEL 타입인데 projectId가 null인 경우
     */
    fun createForChannel(
        channelType: ChannelType,
        channelId: String,
        projectId: String? = null
    ): ChatUseCases {
        return when (channelType) {
            ChannelType.PROJECT_CHANNEL -> {
                requireNotNull(projectId) { "ProjectId is required for PROJECT_CHANNEL type" }
                createForChannel(projectId, channelId)
            }
            ChannelType.DM_CHANNEL -> {
                createForDMChannel(channelId)
            }
        }
    }

    /**
     * 현재 사용자의 채팅 관련 UseCase들을 생성합니다.
     *
     * @return 채팅 관련 UseCase 그룹 (현재 사용자 기준)
     */
    fun createForCurrentUser(): ChatUseCases {
        // TODO: 현재 사용자 기반 로직 구현
        return createForChannel("current-user-project", "current-user-channel") // 임시 처리
    }
}

/**
 * 채팅 관련 UseCase 그룹
 */
data class ChatUseCases(
    val editMessageUseCase: EditMessageUseCase,
    val deleteMessageUseCase: DeleteMessageUseCase,
    val getMessagesStreamUseCase: GetMessagesStreamUseCase,
    val fetchPastMessagesUseCase: FetchPastMessagesUseCase,
    val fetchNewerMessagesUseCase: FetchNewerMessagesUseCase
)