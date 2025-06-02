
package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.constants.FirestorePaths
import com.example.core_common.result.CustomResult
import com.example.data.model.remote.MessageDTO
import com.example.data.model.remote.ReactionDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.dataObjects
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 메시지 원격 데이터 소스 구현체
 * Firestore를 통해 메시지 데이터를 관리합니다.
 */
@Singleton
class MessageRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : MessageRemoteDataSource {

    /**
     * 채널 경로에서 메시지 컬렉션 참조를 가져옵니다.
     * 채널 경로는 이미 완전한 Firestore 경로여야 합니다.
     */
    private fun getMessagesCollection(channelPath: String) =
        firestore.document(channelPath).collection(FirestoreConstants.MessageFields.COLLECTION_NAME)

    /**
     * 채널의 메시지 목록을 관찰합니다.
     */
    override fun observeMessages(channelPath: String, limit: Long): Flow<List<MessageDTO>> {
        if (channelPath.isBlank()) {
            return flow { throw IllegalArgumentException("Channel path cannot be empty.") }
        }
        
        return getMessagesCollection(channelPath)
            .orderBy(FirestoreConstants.MessageFields.SENT_AT, Query.Direction.DESCENDING)
            .limit(limit)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(MessageDTO::class.java) }
    }

    /**
     * 특정 메시지 정보를 가져옵니다.
     */
    override suspend fun getMessage(channelPath: String, messageId: String): CustomResult<MessageDTO?, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            if (channelPath.isBlank() || messageId.isBlank()) {
                throw IllegalArgumentException("Channel path and message ID cannot be empty.")
            }
            val documentSnapshot = getMessagesCollection(channelPath).document(messageId).get().await()
            documentSnapshot.toObject(MessageDTO::class.java)
        }
    }

    /**
     * 새 메시지를 전송합니다.
     */
    override suspend fun sendMessage(
        channelPath: String,
        content: String
    ): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val user = auth.currentUser ?: throw Exception("User not logged in.")
            
            val newMessage = MessageDTO(
                senderId = user.uid,
                senderName = user.displayName ?: "Unknown User",
                senderProfileImageUrl = user.photoUrl?.toString(),
                content = content
            )
            val docRef = getMessagesCollection(channelPath).add(newMessage).await()
            docRef.id
        }
    }

    /**
     * 메시지 내용을 업데이트합니다.
     */
    override suspend fun updateMessage(
        channelPath: String,
        messageId: String,
        newContent: String
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val updateData = mapOf<String, Any>(
                FirestoreConstants.MessageFields.SEND_MESSAGE to newContent,
                FirestoreConstants.MessageFields.UPDATED_AT to FieldValue.serverTimestamp()
            )
            getMessagesCollection(channelPath).document(messageId)
                .update(updateData).await()
            Unit
        }
    }

    /**
     * 메시지를 삭제 표시합니다.
     */
    override suspend fun deleteMessage(
        channelPath: String,
        messageId: String
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val deleteUpdate = mapOf(FirestoreConstants.MessageFields.IS_DELETED to true)
            getMessagesCollection(channelPath).document(messageId)
                .update(deleteUpdate).await()
            Unit
        }
    }

    /**
     * 메시지의 반응 컬렉션 참조를 가져옵니다.
     */
    private fun getReactionsCollection(channelPath: String, messageId: String) =
        getMessagesCollection(channelPath).document(messageId).collection(FirestoreConstants.MessageFields.Attachments.COLLECTION_NAME)

    /**
     * 메시지의 반응 목록을 관찰합니다.
     */
    override fun observeReactions(channelPath: String, messageId: String): Flow<List<ReactionDTO>> {
        return getReactionsCollection(channelPath, messageId).dataObjects()
    }

    /**
     * 메시지에 새 반응을 추가합니다.
     */
    override suspend fun addReaction(
        channelPath: String,
        messageId: String,
        emoji: String
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            val newReaction = ReactionDTO(userId = uid, emoji = emoji)
            // 동일 유저가 동일 이모지 중복 추가 방지 로직은 Repository에서 처리 (쿼리 후 추가)
            // 여기서는 단순 추가. 문서 ID는 Firestore 자동 생성
            getReactionsCollection(channelPath, messageId).add(newReaction).await()
            Unit
        }
    }

    /**
     * 메시지에서 반응을 제거합니다.
     */
    override suspend fun removeReaction(
        channelPath: String,
        messageId: String,
        emoji: String
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            
            val snapshot = getReactionsCollection(channelPath, messageId)
                .whereEqualTo("userId", uid)
                .whereEqualTo("emoji", emoji)
                .limit(1) // 특정 유저가 남긴 특정 이모지는 하나라고 가정
                .get()
                .await()

            if (!snapshot.isEmpty) {
                snapshot.documents.first().reference.delete().await()
            }
            // 삭제할 리액션이 없어도 성공으로 처리 (멱등성)
            Unit
        }
    }

    /**
     * 비동기 작업을 수행하고 결과를 CustomResult로 변환합니다.
     */
    private inline fun <T> resultTry(block: () -> T): CustomResult<T, Exception> {
        return try {
            CustomResult.Success(block())
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}

