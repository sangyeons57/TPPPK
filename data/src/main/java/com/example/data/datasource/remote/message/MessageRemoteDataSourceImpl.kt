package com.example.data.datasource.remote.message

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.dispatcher.DispatcherProvider
import com.example.data.model.mapper.ChatMessageMapper // TODO: Create if not exists
import com.example.domain.model.ChatMessage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import android.util.Log
import com.example.core_common.util.DateTimeUtil // Import DateTimeUtil
import com.example.data.model.mapper.toDtoWithTime

class MessageRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val chatMessageMapper: ChatMessageMapper, // Assumes this will be provided/created
    private val dispatcherProvider: DispatcherProvider
) : MessageRemoteDataSource {

    private companion object {
        private const val TAG = "MessageRemoteDSImpl"
    }

    private fun getMessagesCollection(channelId: String) =
        firestore.collection(FirestoreConstants.Collections.CHANNELS)
            .document(channelId)
            .collection(FirestoreConstants.Collections.MESSAGES)

    override suspend fun sendMessage(chatMessage: ChatMessage): Result<ChatMessage> = withContext(dispatcherProvider.io) {
        Result.runCatching {
            val messageRef = getMessagesCollection(chatMessage.channelId).document()
            
            // Convert Domain ChatMessage to DTO, then to Map
            val dto = chatMessage.toDtoWithTime()
            val messageData = dto.toMap().toMutableMap() // Ensure ChatMessageDto has a toMap() method

            // Add/overwrite server timestamps for consistency
            messageData[FirestoreConstants.MessageFields.SENT_AT] = FieldValue.serverTimestamp()
            messageData[FirestoreConstants.MessageFields.UPDATED_AT] = FieldValue.serverTimestamp()
            
            // Ensure fields like isEdited and isDeleted are correctly set by toDtoWithTime/toMap or set them here if defaults are needed for new messages.
            // For a new message, isEdited and isDeleted are typically false.
            // If not already handled by toDtoWithTime or toMap with correct defaults:
            if (!messageData.containsKey(FirestoreConstants.MessageFields.IS_EDITED)) {
                messageData[FirestoreConstants.MessageFields.IS_EDITED] = false
            }
            if (!messageData.containsKey(FirestoreConstants.MessageFields.IS_DELETED)) {
                messageData[FirestoreConstants.MessageFields.IS_DELETED] = false
            }
            
            messageRef.set(messageData).await()
            Log.d(TAG, "Message sent with ID: ${messageRef.id} in channel ${chatMessage.channelId}")
            
            // Return the original chatMessage with the new ID.
            // Timestamps will be server-generated and not reflected here, but domain model should reflect client-side instant initially.
            // Fetching the doc again to get server timestamps would be an option for more accuracy if needed immediately.
            chatMessage.copy(id = messageRef.id)
        }
    }

    override fun getMessagesStream(channelId: String, limit: Int): Flow<List<ChatMessage>> = callbackFlow {
        val query = getMessagesCollection(channelId)
            // Ensure this matches the field name used for server timestamp in sendMessage
            .orderBy(FirestoreConstants.MessageFields.SENT_AT, Query.Direction.DESCENDING)
            .limit(limit.toLong())

        val listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to message stream for channel $channelId", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val messages = snapshots.documents.mapNotNull {
                    chatMessageMapper.fromSnapshotToDomain(it)
                }.reversed() 
                Log.d(TAG, "Message stream for channel $channelId emitted ${messages.size} messages")
                trySend(messages).isSuccess
            } else {
                Log.d(TAG, "Message stream for channel $channelId: snapshots was null")
            }
        }
        awaitClose { 
            Log.d(TAG, "Closing message stream for channel $channelId")
            listenerRegistration.remove() 
        }
    }.flowOn(dispatcherProvider.io)

    override suspend fun getMessage(channelId: String, messageId: String): Result<ChatMessage> = withContext(dispatcherProvider.io) {
        Result.runCatching {
            val document = getMessagesCollection(channelId).document(messageId).get().await()
            chatMessageMapper.fromSnapshotToDomain(document)
                ?: throw NoSuchElementException("Message $messageId not found in channel $channelId")
        }
    }

    override suspend fun updateMessage(chatMessage: ChatMessage): Result<Unit> = withContext(dispatcherProvider.io) {
        Result.runCatching {
            val updates = mutableMapOf<String, Any?>()
            
            // Required fields for an update
            updates[FirestoreConstants.MessageFields.MESSAGE] = chatMessage.text // Assuming text is what's being "updated"
            updates[FirestoreConstants.MessageFields.IS_EDITED] = true
            updates[FirestoreConstants.MessageFields.UPDATED_AT] = FieldValue.serverTimestamp()

            // Optional: if other fields can be updated, map them here from chatMessage domain object
            // Example for attachments (if they can be part of an update operation)
            // chatMessage.attachments?.let {
            //    updates[FirestoreConstants.MessageFields.ATTACHMENTS] = chatMessageMapper.attachmentsToFirestoreList(it)
            // }
            // Example for reactions
            // chatMessage.reactions?.let { updates[FirestoreConstants.MessageFields.REACTIONS] = it }
            // Example for metadata
            // chatMessage.metadata?.let { updates[FirestoreConstants.MessageFields.METADATA] = it }


            getMessagesCollection(chatMessage.channelId).document(chatMessage.id)
                .update(updates) // Use the constructed map
                .await()
            Log.d(TAG, "Message updated with ID: ${chatMessage.id} in channel ${chatMessage.channelId}")
            Unit
        }
    }

    override suspend fun deleteMessage(channelId: String, messageId: String): Result<Unit> = withContext(dispatcherProvider.io) {
        Result.runCatching {
            getMessagesCollection(channelId).document(messageId)
                .update(mapOf(
                    FirestoreConstants.MessageFields.IS_DELETED to true,
                    FirestoreConstants.MessageFields.UPDATED_AT to FieldValue.serverTimestamp()
                )).await()
            Log.d(TAG, "Message soft-deleted with ID: $messageId in channel $channelId")
            Unit
        }
    }

    override suspend fun getMessages(channelId: String, limit: Int, before: Instant?): Result<List<ChatMessage>> = withContext(dispatcherProvider.io) {
        Result.runCatching {
            var query = getMessagesCollection(channelId)
                 // Ensure this matches the field name used for server timestamp in sendMessage
                .orderBy(FirestoreConstants.MessageFields.SENT_AT, Query.Direction.DESCENDING)
            
            before?.let {
                DateTimeUtil.instantToFirebaseTimestamp(it)?.let { firebaseTimestamp ->
                    query = query.startAfter(firebaseTimestamp)
                }
            }
            
            query = query.limit(limit.toLong())
            
            val snapshots = query.get().await()
            snapshots.documents.mapNotNull {
                chatMessageMapper.fromSnapshotToDomain(it)
            }.reversed() 
        }
    }

    override suspend fun addReaction(channelId: String, messageId: String, userId: String, emoji: String): Result<Unit> = withContext(dispatcherProvider.io) {
        Result.runCatching {
            val messageRef = getMessagesCollection(channelId).document(messageId)
            firestore.runTransaction {
                val snapshot = it.get(messageRef)
                val existingReactions = snapshot.get("${FirestoreConstants.MessageFields.REACTIONS}.$emoji") as? List<String>
                val newReactionUserList = existingReactions?.toMutableList() ?: mutableListOf()
                if (!newReactionUserList.contains(userId)) {
                    newReactionUserList.add(userId)
                }
                it.update(messageRef, "${FirestoreConstants.MessageFields.REACTIONS}.$emoji", newReactionUserList)
                null // Return type for Firestore transaction lambda
            }.await()
            Unit
        }
    }

    override suspend fun removeReaction(channelId: String, messageId: String, userId: String, emoji: String): Result<Unit> = withContext(dispatcherProvider.io) {
        Result.runCatching {
            val messageRef = getMessagesCollection(channelId).document(messageId)
            firestore.runTransaction {
                val snapshot = it.get(messageRef)
                val existingReactions = snapshot.get("${FirestoreConstants.MessageFields.REACTIONS}.$emoji") as? List<String>
                val newReactionUserList = existingReactions?.toMutableList() ?: mutableListOf()
                newReactionUserList.remove(userId)
                if (newReactionUserList.isEmpty()) {
                    it.update(messageRef, "${FirestoreConstants.MessageFields.REACTIONS}.$emoji", FieldValue.delete())
                } else {
                    it.update(messageRef, "${FirestoreConstants.MessageFields.REACTIONS}.$emoji", newReactionUserList)
                }
                null // Return type for Firestore transaction lambda
            }.await()
            Unit
        }
    }

    // TODO: Implement methods for unread count and marking messages as read if they become part of this DataSource's responsibility.
    // These were previously commented out in the interface.
} 