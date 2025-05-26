
package com.example.data.datasource._remote

import com.example.data.model._remote.MessageDTO
import com.example.data.model._remote.ReactionDTO
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

    companion object {
        private const val MESSAGES_COLLECTION = "messages"
        private const val REACTIONS_COLLECTION = "reactions"
    }

    private fun getMessagesCollection(channelPath: String) =
        firestore.collection(channelPath).document().parent.collection(MESSAGES_COLLECTION)


    override fun observeMessages(channelPath: String, limit: Long): Flow<List<MessageDTO>> {
        if (channelPath.isBlank()) {
            return kotlinx.coroutines.flow.flow { throw IllegalArgumentException("Channel path cannot be empty.") }
        }
        return getMessagesCollection(channelPath)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(limit)
            .dataObjects()
    }

    override suspend fun sendMessage(
        channelPath: String,
        content: String
    ): Result<String> = withContext(Dispatchers.IO) {
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
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val updateData = mapOf(
                "content" to newContent,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            getMessagesCollection(channelPath).document(messageId)
                .update(updateData).await()
            Unit
        }
    }

    override suspend fun deleteMessage(
        channelPath: String,
        messageId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val deleteUpdate = mapOf("isDeleted" to true)
            getMessagesCollection(channelPath).document(messageId)
                .update(deleteUpdate).await()
            Unit
        }
    }

    private fun getReactionsCollection(channelPath: String, messageId: String) =
        getMessagesCollection(channelPath).document(messageId).collection(REACTIONS_COLLECTION)

    override fun observeReactions(channelPath: String, messageId: String): Flow<List<ReactionDTO>> {
        return getReactionsCollection(channelPath, messageId).dataObjects()
    }

    override suspend fun addReaction(
        channelPath: String,
        messageId: String,
        emoji: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            // 사용자가 동일한 이모지를 중복해서 추가할 수 없도록, (userId, emoji) 조합으로 쿼리하여 확인 후 추가하는 로직이
            // Repository 계층에 필요할 수 있습니다. DataSource는 단순 추가 기능만 담당합니다.
            val newReaction = ReactionDTO(userId = uid, emoji = emoji)
            getReactionsCollection(channelPath, messageId).add(newReaction).await()
            Unit
        }
    }

    override suspend fun removeReaction(
        channelPath: String,
        messageId: String,
        emoji: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            
            // 내가 남긴 특정 이모지 리액션을 찾아서 삭제
            val snapshot = getReactionsCollection(channelPath, messageId)
                .whereEqualTo("userId", uid)
                .whereEqualTo("emoji", emoji)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                snapshot.documents.first().reference.delete().await()
            }
            Unit
        }
    }

    private inline fun <T> resultTry(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Throwable) {
            if (e is java.util.concurrent.CancellationException) throw e
            Result.failure(e)
        }
    }
}

