package com.example.domain.provider

import com.example.domain.repository.local.AuthLocalRepository
import com.example.domain.repository.local.MessageLocalRepository
import com.example.domain.repository.local.ChatCacheLocalRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 채팅 기능 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 채팅 채널별 메시지 관리 기능을 담당합니다.
 * 프로젝트 채널과 DM 채널 모두를 지원합니다.
 */
@Singleton
class ChatUseCaseProvider @Inject constructor(
    private val messageLocalRepository: MessageLocalRepository,
    private val chatCacheLocalRepository: ChatCacheLocalRepository,
    private val authLocalRepository: AuthLocalRepository
) {

    /**
     * 프로젝트 채널용 채팅 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 채널 채팅 UseCase 그룹
     */
    fun createForProjectChannel(): ChatLocalProjectChannelUseCases {
        return ChatLocalProjectChannelUseCases(
            // TODO: 향후 project channel chat local use cases 추가
            messageLocalRepository = messageLocalRepository,
            chatCacheLocalRepository = chatCacheLocalRepository,
            authLocalRepository = authLocalRepository
        )
    }

    /**
     * DM 채널용 채팅 관련 UseCase들을 생성합니다.
     * 
     * @return DM 채널 채팅 UseCase 그룹
     */
    fun createForDMChannel(): ChatLocalDMChannelUseCases {
        return ChatLocalDMChannelUseCases(
            // TODO: 향후 DM channel chat local use cases 추가
            messageLocalRepository = messageLocalRepository,
            chatCacheLocalRepository = chatCacheLocalRepository,
            authLocalRepository = authLocalRepository
        )
    }

    /**
     * 통합 채팅 관련 UseCase들을 생성합니다.
     * 
     * @return 통합 채팅 UseCase 그룹
     */
    fun createUnified(): ChatLocalUnifiedUseCases {
        return ChatLocalUnifiedUseCases(
            // TODO: 향후 unified chat local use cases 추가
            messageLocalRepository = messageLocalRepository,
            chatCacheLocalRepository = chatCacheLocalRepository,
            authLocalRepository = authLocalRepository
        )
    }
}

/**
 * 프로젝트 채널 채팅 Local UseCase 그룹
 */
data class ChatLocalProjectChannelUseCases(
    val messageLocalRepository: MessageLocalRepository,
    val chatCacheLocalRepository: ChatCacheLocalRepository,
    val authLocalRepository: AuthLocalRepository
)

/**
 * DM 채널 채팅 Local UseCase 그룹
 */
data class ChatLocalDMChannelUseCases(
    val messageLocalRepository: MessageLocalRepository,
    val chatCacheLocalRepository: ChatCacheLocalRepository,
    val authLocalRepository: AuthLocalRepository
)

/**
 * 통합 채팅 Local UseCase 그룹
 */
data class ChatLocalUnifiedUseCases(
    val messageLocalRepository: MessageLocalRepository,
    val chatCacheLocalRepository: ChatCacheLocalRepository,
    val authLocalRepository: AuthLocalRepository
) 