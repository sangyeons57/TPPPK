package com.example.data.datasource.remote.chat

import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.MessageFields
import com.example.data.model.remote.chat.ChatMessageDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatRemoteDataSource 인터페이스의 Firestore 구현체입니다.
 */
@Singleton
class ChatRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRemoteDataSource {

    // 채팅 채널별 서브컬렉션 사용
    private fun getChatCollection(channelId: String) = 
        firestore.collection(Collections.CHAT_CHANNELS).document(channelId).collection(Collections.MESSAGES)

    /**
     * 특정 채널의 메시지 목록을 가져옵니다. 페이징 처리가 가능합니다.
     * 
     * @param channelId 메시지를 가져올 채널 ID
     * @param beforeMessageId 이 ID보다 이전 메시지만 가져옵니다. null이면 최신 메시지부터 가져옵니다.
     * @param limit 가져올 메시지 최대 개수
     * @return 채팅 메시지 DTO 목록
     */
    override suspend fun getMessages(channelId: String, beforeMessageId: Int?, limit: Int): List<ChatMessageDto> {
        var query = getChatCollection(channelId)
            .orderBy(MessageFields.TIMESTAMP, Query.Direction.DESCENDING) // 최신 메시지부터 정렬

        if (beforeMessageId != null) {
            // 메시지 ID로 해당 메시지 문서 가져오기 시도
            val lastVisibleSnapshot = getChatCollection(channelId)
                .whereEqualTo(MessageFields.CHAT_ID, beforeMessageId)
                .limit(1)
                .get()
                .await()
                
            if (!lastVisibleSnapshot.isEmpty) {
                query = query.startAfter(lastVisibleSnapshot.documents[0])
            } else {
                // ID가 존재하지 않으면 chatId 필드가 beforeMessageId보다 작은 메시지 가져오기
                query = query.whereLessThan(MessageFields.CHAT_ID, beforeMessageId)
            }
        }

        val querySnapshot = query.limit(limit.toLong()).get().await()
        return querySnapshot.documents.mapNotNull { 
            it.toObject(ChatMessageDto::class.java)?.copy(id = it.id) 
        }.reversed() // 다시 시간순 정렬
    }

    /**
     * 새 메시지를 채팅 채널에 전송합니다.
     * 
     * @param channelId 메시지를 전송할 채널 ID
     * @param messageDto 전송할 메시지 DTO
     * @return 전송된 메시지 DTO (서버에서 할당된 ID 포함)
     */
    override suspend fun sendMessage(channelId: String, messageDto: ChatMessageDto): ChatMessageDto {
        val documentReference = getChatCollection(channelId).add(messageDto).await()
        
        // 서버에서 생성된 ID로 메시지 DTO 복사
        val addedMessageSnapshot = documentReference.get().await()
        return addedMessageSnapshot.toObject(ChatMessageDto::class.java)
            ?.copy(id = documentReference.id) 
            ?: throw IllegalStateException("Failed to retrieve sent message.")
    }

    /**
     * 기존 메시지 내용을 수정합니다.
     * 
     * @param channelId 메시지가 속한 채널 ID
     * @param messageId 수정할 메시지 ID
     * @param newContent 새 메시지 내용
     */
    override suspend fun editMessage(channelId: String, messageId: Int, newContent: String) {
        val query = getChatCollection(channelId)
            .whereEqualTo(MessageFields.CHAT_ID, messageId)
            .limit(1)
            .get()
            .await()
            
        if (!query.isEmpty) {
            val document = query.documents[0]
            document.reference.update(
                mapOf(
                    MessageFields.CONTENT to newContent,
                    MessageFields.IS_MODIFIED to true
                )
            ).await()
        } else {
            throw NoSuchElementException("Message with ID $messageId not found in channel $channelId")
        }
    }

    /**
     * 메시지를 삭제합니다.
     * 
     * @param channelId 메시지가 속한 채널 ID
     * @param messageId 삭제할 메시지 ID
     */
    override suspend fun deleteMessage(channelId: String, messageId: Int) {
        val query = getChatCollection(channelId)
            .whereEqualTo(MessageFields.CHAT_ID, messageId)
            .limit(1)
            .get()
            .await()
            
        if (!query.isEmpty) {
            val document = query.documents[0]
            document.reference.delete().await()
        } else {
            throw NoSuchElementException("Message with ID $messageId not found in channel $channelId")
        }
    }
} 