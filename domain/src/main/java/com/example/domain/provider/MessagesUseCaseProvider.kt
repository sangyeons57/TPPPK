package com.example.domain.provider

import com.example.domain.repository.local.MessageLocalRepository
import com.example.domain.usecase.local.messages.DeleteMessageLocalUseCase
import com.example.domain.usecase.local.messages.DeleteMessageLocalUseCaseImpl
import com.example.domain.usecase.local.messages.DeleteMessageUseCase
import com.example.domain.usecase.local.messages.DeleteMessageUseCaseImpl
import com.example.domain.usecase.local.messages.EditMessageLocalUseCase
import com.example.domain.usecase.local.messages.EditMessageLocalUseCaseImpl
import com.example.domain.usecase.local.messages.FetchNewerMessagesLocalUseCase
import com.example.domain.usecase.local.messages.FetchNewerMessagesLocalUseCaseImpl
import com.example.domain.usecase.local.messages.FetchPastMessagesLocalUseCase
import com.example.domain.usecase.local.messages.FetchPastMessagesLocalUseCaseImpl
import com.example.domain.usecase.local.messages.GetMessagesByUserUseCase
import com.example.domain.usecase.local.messages.GetMessagesByUserUseCaseImpl
import com.example.domain.usecase.local.messages.GetMessagesStreamLocalUseCase
import com.example.domain.usecase.local.messages.GetMessagesStreamLocalUseCaseImpl
import com.example.domain.usecase.local.messages.GetMessagesUseCase
import com.example.domain.usecase.local.messages.GetMessagesUseCaseImpl
import com.example.domain.usecase.local.messages.GetMessageUseCase
import com.example.domain.usecase.local.messages.GetMessageUseCaseImpl
import com.example.domain.usecase.local.messages.InsertMessageUseCase
import com.example.domain.usecase.local.messages.InsertMessageUseCaseImpl
import com.example.domain.usecase.local.messages.SendMessageLocalUseCase
import com.example.domain.usecase.local.messages.SendMessageLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 메시지 관리 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 메시지 송수신, 수정, 삭제, 조회 등의 기능을 담당합니다.
 */
@Singleton
class MessagesUseCaseProvider @Inject constructor(
    private val messageLocalRepository: MessageLocalRepository
) {

    /**
     * 메시지 기본 CRUD 관련 UseCase들을 생성합니다.
     * 
     * @return 메시지 기본 관리 UseCase 그룹
     */
    fun createBasicUseCases(): MessagesLocalBasicUseCases {
        return MessagesLocalBasicUseCases(
            // 메시지 조회
            getMessageUseCase = GetMessageUseCaseImpl(
                messageLocalRepository = messageLocalRepository
            ),
            
            getMessagesUseCase = GetMessagesUseCaseImpl(
                messageLocalRepository = messageLocalRepository
            ),
            
            getMessagesByUserUseCase = GetMessagesByUserUseCaseImpl(
                messageLocalRepository = messageLocalRepository
            ),
            
            // 메시지 관리
            insertMessageUseCase = InsertMessageUseCaseImpl(
                messageLocalRepository = messageLocalRepository
            ),
            
            deleteMessageUseCase = DeleteMessageUseCaseImpl(
                messageLocalRepository = messageLocalRepository
            ),
            
            messageLocalRepository = messageLocalRepository
        )
    }

    /**
     * 메시지 실시간 처리 관련 UseCase들을 생성합니다.
     * 
     * @return 메시지 실시간 처리 UseCase 그룹
     */
    fun createRealtimeUseCases(): MessagesLocalRealtimeUseCases {
        return MessagesLocalRealtimeUseCases(
            // 메시지 전송
            sendMessageLocalUseCase = SendMessageLocalUseCaseImpl(
                messageLocalRepository = messageLocalRepository
            ),
            
            // 메시지 수정
            editMessageLocalUseCase = EditMessageLocalUseCaseImpl(
                messageLocalRepository = messageLocalRepository
            ),
            
            // 메시지 삭제 (실시간)
            deleteMessageLocalUseCase = DeleteMessageLocalUseCaseImpl(
                messageLocalRepository = messageLocalRepository
            ),
            
            // 실시간 스트림
            getMessagesStreamLocalUseCase = GetMessagesStreamLocalUseCaseImpl(
                messageLocalRepository = messageLocalRepository
            ),
            
            messageLocalRepository = messageLocalRepository
        )
    }

    /**
     * 메시지 페이지네이션 관련 UseCase들을 생성합니다.
     * 
     * @return 메시지 페이지네이션 UseCase 그룹
     */
    fun createPaginationUseCases(): MessagesLocalPaginationUseCases {
        return MessagesLocalPaginationUseCases(
            // 과거 메시지 조회
            fetchPastMessagesLocalUseCase = FetchPastMessagesLocalUseCaseImpl(
                messageLocalRepository = messageLocalRepository
            ),
            
            // 최신 메시지 조회
            fetchNewerMessagesLocalUseCase = FetchNewerMessagesLocalUseCaseImpl(
                messageLocalRepository = messageLocalRepository
            ),
            
            messageLocalRepository = messageLocalRepository
        )
    }
}

/**
 * 메시지 기본 관리 Local UseCase 그룹
 */
data class MessagesLocalBasicUseCases(
    // 메시지 조회
    val getMessageUseCase: GetMessageUseCase,
    val getMessagesUseCase: GetMessagesUseCase,
    val getMessagesByUserUseCase: GetMessagesByUserUseCase,
    
    // 메시지 관리
    val insertMessageUseCase: InsertMessageUseCase,
    val deleteMessageUseCase: DeleteMessageUseCase,
    
    val messageLocalRepository: MessageLocalRepository
)

/**
 * 메시지 실시간 처리 Local UseCase 그룹
 */
data class MessagesLocalRealtimeUseCases(
    // 메시지 전송
    val sendMessageLocalUseCase: SendMessageLocalUseCase,
    
    // 메시지 수정
    val editMessageLocalUseCase: EditMessageLocalUseCase,
    
    // 메시지 삭제 (실시간)
    val deleteMessageLocalUseCase: DeleteMessageLocalUseCase,
    
    // 실시간 스트림
    val getMessagesStreamLocalUseCase: GetMessagesStreamLocalUseCase,
    
    val messageLocalRepository: MessageLocalRepository
)

/**
 * 메시지 페이지네이션 Local UseCase 그룹
 */
data class MessagesLocalPaginationUseCases(
    // 과거 메시지 조회
    val fetchPastMessagesLocalUseCase: FetchPastMessagesLocalUseCase,
    
    // 최신 메시지 조회
    val fetchNewerMessagesLocalUseCase: FetchNewerMessagesLocalUseCase,
    
    val messageLocalRepository: MessageLocalRepository
) 