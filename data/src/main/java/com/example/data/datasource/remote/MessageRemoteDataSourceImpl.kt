
package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.data.model.remote.MessageDTO
import com.example.data.model.remote.ReactionDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.dataObjects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : MessageRemoteDataSource {

    // FirestoreConstants에서 정의된 상수 사용

    private fun getMessagesCollection(channelPath: String) =
        firestore.collection(channelPath).document().parent.collection(FirestoreConstants.MessageFields.Messages.COLLECTION_NAME)


    override fun observeMessages(channelPath: String, limit: Long): Flow<List<MessageDTO>> {
        if (channelPath.isBlank()) {
            return kotlinx.coroutines.flow.flow { throw IllegalArgumentException("Channel path cannot be empty.") }
        }
        return getMessagesCollection(channelPath)
            .orderBy(FirestoreConstants.MessageFields.SENT_AT, Query.Direction.DESCENDING)
            .limit(limit)
            .dataObjects()
    }

    override suspend fun getMessage(channelPath: String, messageId: String): CustomResult<MessageDTO?, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            if (channelPath.isBlank() || messageId.isBlank()) {
                throw IllegalArgumentException("Channel path and message ID cannot be empty.")
            }
            val document = getMessagesCollection(channelPath).document(messageId).get()
        }
    }

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

    override suspend fun updateMessage(
        channelPath: String,
        messageId: String,
        newContent: String
    ): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val updateData = mapOf(
                FirestoreConstants.MessageFields.SEND_MESSAGE to newContent,
                FirestoreConstants.MessageFields.UPDATED_AT to FieldValue.serverTimestamp()
            )
            getMessagesCollection(channelPath).document(messageId)
                .update(updateData).await()
            Unit
        }
    }

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

    private fun getReactionsCollection(channelPath: String, messageId: String) =
        getMessagesCollection(channelPath).document(messageId).collection(FirestoreConstants.MessageFields.Attachments.COLLECTION_NAME)

    override fun observeReactions(channelPath: String, messageId: String): Flow<List<ReactionDTO>> {
        return getReactionsCollection(channelPath, messageId).dataObjects()
    }

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

    private inline fun <T> resultTry(block: () -> T): CustomResult<T, Exception> {
        return try {
            CustomResult.Success(block())
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}

