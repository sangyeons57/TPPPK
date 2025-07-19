package com.example.domain.provider.chat

import com.example.domain.usecase.message.SendMessageUseCase
import com.example.domain.usecase.message.EditMessageUseCase
import com.example.domain.usecase.message.DeleteMessageUseCase
import com.example.domain.usecase.message.GetMessagesStreamUseCase
import com.example.domain.usecase.message.FetchPastMessagesUseCase
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.MessageRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.MessageRepositoryFactoryContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 채팅 및 메시지 관련 UseCase들을 제공하는 Provider
 *
 * 메시지 전송, 수정, 삭제, 실시간 메시지 스트림 등의 기능을 담당합니다.
 *
 * TODO: 다음 주에 구현 예정
 */
@Singleton
class ChatUseCaseProvider @Inject constructor(
    private val messageRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<MessageRepositoryFactoryContext, MessageRepository>,
    private val authRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * 특정 채널의 채팅 관련 UseCase들을 생성합니다.
     *
     * @param channelId 채널 ID
     * @return 채팅 관련 UseCase 그룹
     */
    fun createForChannel(channelId: String): ChatUseCases {
        val messageRepository = messageRepositoryFactory.create(
            MessageRepositoryFactoryContext(
                collectionPath = TODO("CollectionPath 미구현")
            )
        )

        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        return ChatUseCases(
            sendMessageUseCase = SendMessageUseCase(messageRepository),
            editMessageUseCase = EditMessageUseCase(messageRepository),
            deleteMessageUseCase = DeleteMessageUseCase(messageRepository),
            getMessagesStreamUseCase = GetMessagesStreamUseCase(messageRepository),
            fetchPastMessagesUseCase = FetchPastMessagesUseCase(messageRepository),

            // 공통 Repository
            authRepository = authRepository,
            messageRepository = messageRepository
        )
    }

    /**
     * 현재 사용자의 채팅 관련 UseCase들을 생성합니다.
     *
     * @return 채팅 관련 UseCase 그룹 (현재 사용자 기준)
     */
    fun createForCurrentUser(): ChatUseCases {
        // TODO: 현재 사용자 기반 로직 구현
        return createForChannel("current-user-channel") // 임시 처리
    }
}

/**
 * 채팅 관련 UseCase 그룹
 */
data class ChatUseCases(
    val sendMessageUseCase: SendMessageUseCase,
    val editMessageUseCase: EditMessageUseCase,
    val deleteMessageUseCase: DeleteMessageUseCase,
    val getMessagesStreamUseCase: GetMessagesStreamUseCase,
    val fetchPastMessagesUseCase: FetchPastMessagesUseCase,

    // 공통 Repository
    val authRepository: AuthRepository,
    val messageRepository: MessageRepository
)