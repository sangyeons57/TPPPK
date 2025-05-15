package com.example.data.datasource.remote.message

import com.example.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 메시지 관련 원격 데이터 소스 인터페이스.
 * Firestore의 'channels/{channelId}/messages' 하위 컬렉션과의 상호작용을 정의합니다.
 */
interface MessageRemoteDataSource {
    /**
     * 특정 채널에 새 메시지를 전송합니다.
     * @param chatMessage 전송할 메시지 정보를 담은 [ChatMessage] 객체.
     * @return 성공 시 생성된 메시지 정보([ChatMessage])를 담은 [Result].
     */
    suspend fun sendMessage(chatMessage: ChatMessage): Result<ChatMessage>

    /**
     * 특정 채널의 메시지 변경 사항을 실시간으로 구독합니다.
     * @param channelId 구독할 채널의 ID.
     * @param limit 구독할 최신 메시지 수.
     * @return 메시지 목록([ChatMessage])의 변경 사항을 방출하는 [Flow].
     */
    fun getMessagesStream(channelId: String, limit: Int): Flow<List<ChatMessage>>

    /**
     * 특정 채널에서 특정 메시지를 조회합니다.
     * @param channelId 메시지가 속한 채널의 ID.
     * @param messageId 조회할 메시지의 ID.
     * @return 성공 시 조회된 메시지 정보([ChatMessage])를 담은 [Result].
     */
    suspend fun getMessage(channelId: String, messageId: String): Result<ChatMessage>

    /**
     * 특정 채널의 메시지 내용을 업데이트합니다. (예: 수정)
     * @param chatMessage 업데이트할 내용을 담은 [ChatMessage] 객체. (id 필드 필수)
     * @return 성공 시 [Result]<Unit>.
     */
    suspend fun updateMessage(chatMessage: ChatMessage): Result<Unit>

    /**
     * 특정 채널의 메시지를 논리적으로 삭제합니다.
     * @param channelId 메시지가 속한 채널의 ID.
     * @param messageId 삭제할 메시지의 ID.
     * @return 성공 시 [Result]<Unit>.
     */
    suspend fun deleteMessage(channelId: String, messageId: String): Result<Unit>

    /**
     * 특정 채널의 메시지 목록을 조회합니다 (페이징).
     * @param channelId 조회할 채널의 ID.
     * @param limit 한 번에 조회할 최대 메시지 수.
     * @param before 페이징 기준점. 이 시간([Instant]) 이전에 전송된 메시지만 조회. null이면 최신부터.
     * @return 성공 시 조회된 메시지 목록([ChatMessage])을 담은 [Result].
     */
    suspend fun getMessages(channelId: String, limit: Int, before: Instant?): Result<List<ChatMessage>>

    /**
     * 특정 메시지에 리액션을 추가/업데이트합니다.
     * @param channelId 메시지가 속한 채널의 ID.
     * @param messageId 리액션을 추가할 메시지의 ID.
     * @param userId 리액션을 추가하는 사용자의 ID.
     * @param emoji 추가할 리액션 이모지 문자열.
     * @return 성공 시 [Result]<Unit>.
     */
    suspend fun addReaction(channelId: String, messageId: String, userId: String, emoji: String): Result<Unit>

    /**
     * 특정 메시지에서 사용자의 리액션을 제거합니다.
     * @param channelId 메시지가 속한 채널의 ID.
     * @param messageId 리액션을 제거할 메시지의 ID.
     * @param userId 리액션을 제거하는 사용자의 ID.
     * @param emoji 제거할 리액션 이모지 문자열.
     * @return 성공 시 [Result]<Unit>.
     */
    suspend fun removeReaction(channelId: String, messageId: String, userId: String, emoji: String): Result<Unit>
    
    // TODO: 다음 기능들은 Message 모듈과 Channel 모듈 간의 책임 소재를 더 명확히 한 후 위치 결정 필요
    // /**
    //  * 채널을 읽음으로 표시하고 마지막 읽은 시간을 업데이트합니다.
    //  * @param channelId 대상 채널 ID.
    //  * @param userId 사용자 ID.
    //  * @param lastReadAt 마지막으로 읽은 시간.
    //  * @return 성공 시 [Result]<Unit>.
    //  */
    // suspend fun markChannelAsRead(channelId: String, userId: String, lastReadAt: Instant): Result<Unit>
    //
    // /**
    //  * 특정 채널의 안 읽은 메시지 정보를 실시간으로 구독합니다.
    //  * @param channelId 대상 채널 ID.
    //  * @param userId 사용자 ID.
    //  * @return 채널 안 읽은 정보([ChannelUnreadInfo]) 스트림.
    //  */
    // fun getChannelUnreadInfoStream(channelId: String, userId: String): Flow<ChannelUnreadInfo>
    //
    // /**
    //  * 특정 채널의 안 읽은 메시지 수를 가져옵니다.
    //  * @param channelId 대상 채널 ID.
    //  * @param userId 사용자 ID.
    //  * @return 성공 시 안 읽은 메시지 수.
    //  */
    // suspend fun getChannelUnreadCount(channelId: String, userId: String): Result<Int>

    // searchMessages는 우선 제외. 필요시 나중에 추가.
} 