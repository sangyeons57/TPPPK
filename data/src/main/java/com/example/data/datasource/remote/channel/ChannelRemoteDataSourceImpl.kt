package com.example.data.datasource.remote.channel

import android.util.Log
import com.example.core_common.constants.FirestoreConstants.ChannelFields
import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.ChannelDmDataFields
import com.example.core_common.constants.FirestoreConstants.ChannelProjectDataFields
import com.example.data.model.mapper.ChannelMapper
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.example.core_common.util.DateTimeUtil
import com.google.firebase.firestore.FieldValue
import com.example.data.model.mapper.toDtoWithTime
import com.example.core_common.constants.FirestoreConstants

/**
 * 채널 관련 원격 데이터 소스 구현체
 * Firestore의 'channels' 컬렉션 및 관련 하위 컬렉션과의 상호작용을 정의합니다.
 * 모든 메소드는 Result 또는 Flow를 반환하여 성공/실패 또는 실시간 업데이트를 나타냅니다.
 * 
 * @param firestore Firebase Firestore 인스턴스
 * @param auth Firebase Auth 인스턴스
 * @param channelMapper Channel 데이터 매핑을 위한 매퍼
 */
@Singleton
class ChannelRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val channelMapper: ChannelMapper
) : ChannelRemoteDataSource {

    private val ioDispatcher = Dispatchers.IO
    private val TAG = "ChannelRemoteImpl"

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    override suspend fun getChannel(channelId: String): Result<Channel> = withContext(ioDispatcher) {
        Result.runCatching {
            val document = firestore.collection(Collections.CHANNELS).document(channelId).get().await()
            if (!document.exists()) throw NoSuchElementException("Channel $channelId not found")
            channelMapper.mapToDomain(document) ?: throw IllegalStateException("Failed to map channel $channelId")
        }
    }

    override fun getChannelStream(channelId: String): Flow<Channel> = callbackFlow {
        val listener = firestore.collection(Collections.CHANNELS).document(channelId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                    try {
                        val channel = channelMapper.mapToDomain(snapshot)
                        if (channel != null) {
                            trySend(channel).isSuccess
                        } else {
                            Log.w(TAG, "Channel stream: Failed to map channel ${snapshot.id}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Channel stream: Error mapping channel ${snapshot.id}", e)
                        close(e)
                    }
                } else {
                    Log.w(TAG, "Channel stream: Document $channelId does not exist or was deleted.")
                }
            }
        awaitClose { listener.remove() }
    }
    
    override suspend fun createChannel(channel: Channel): Result<Channel> = withContext(ioDispatcher) {
        Result.runCatching {
            val channelId = if (channel.id.isBlank()) UUID.randomUUID().toString() else channel.id
            val dto = channel.copy(id = channelId).toDtoWithTime()
            val data = dto.toMap().toMutableMap()

            val nowAsFirebaseTimestamp = DateTimeUtil.nowFirebaseTimestamp()

            if (!data.containsKey(ChannelFields.CREATED_AT) || data[ChannelFields.CREATED_AT] == null) {
                 data[ChannelFields.CREATED_AT] = nowAsFirebaseTimestamp
            }
            if (!data.containsKey(ChannelFields.UPDATED_AT) || data[ChannelFields.UPDATED_AT] == null) {
                data[ChannelFields.UPDATED_AT] = nowAsFirebaseTimestamp
            }
             if (channel.createdBy.isNullOrBlank() && (!data.containsKey(ChannelFields.CREATED_BY) || data[ChannelFields.CREATED_BY] == null)) {
                data[ChannelFields.CREATED_BY] = currentUserId
            }

            firestore.collection(Collections.CHANNELS).document(channelId).set(data).await()
            val newDoc = firestore.collection(Collections.CHANNELS).document(channelId).get().await()
            channelMapper.mapToDomain(newDoc) ?: throw IllegalStateException("Failed to map newly created channel $channelId")
        }
    }

    override suspend fun updateChannel(channel: Channel): Result<Unit> = withContext(ioDispatcher) {
        Result.runCatching {
            val dto = channel.toDtoWithTime()
            val data = dto.toMap().toMutableMap()
            data[ChannelFields.UPDATED_AT] = DateTimeUtil.nowFirebaseTimestamp()
            firestore.collection(Collections.CHANNELS).document(channel.id).set(data, SetOptions.merge()).await()
            Unit
        }
    }

    override suspend fun deleteChannel(channelId: String): Result<Unit> = withContext(ioDispatcher) {
        Result.runCatching {
            firestore.collection(Collections.CHANNELS).document(channelId).delete().await()
            Unit
        }
    }
    
    private fun buildChannelsQuery(
        baseQuery: Query,
        type: ChannelType? = null,
        userId: String? = null,
        projectId: String? = null,
        categoryId: String? = null
    ): Query {
        Log.d(TAG, "buildChannelsQuery: type=$type, userId=$userId, projectId=$projectId, categoryId=$categoryId")
        var query = baseQuery
        type?.let { query = query.whereEqualTo(ChannelFields.CHANNEL_TYPE, it.name) }
        
        if (type == ChannelType.DM && userId != null) {
            Log.d(TAG, "buildChannelsQuery: Adding DM filter for userId=$userId")
            query = query.whereArrayContains( 
                ChannelFields.dmDataPath(ChannelDmDataFields.PARTICIPANT_IDS),
                userId
            )
        }
            
        if (type == ChannelType.PROJECT) {
            projectId?.let { 
                query = query.whereEqualTo(
                    ChannelFields.projectDataPath(ChannelProjectDataFields.PROJECT_ID), 
                    it
                ) 
            }
            categoryId?.let { 
                query = query.whereEqualTo(
                    ChannelFields.projectDataPath(ChannelProjectDataFields.CATEGORY_ID),
                    it
                ) 
            }
        }
        Log.d(TAG, "buildChannelsQuery: Final query created for type=$type")
        return query
    }

    override suspend fun getUserChannels(userId: String, type: ChannelType?): Result<List<Channel>> = withContext(ioDispatcher) {
        Result.runCatching {
            val queries = mutableListOf<Query>()

            if (type == null || type == ChannelType.DM) {
                queries.add(buildChannelsQuery(firestore.collection(Collections.CHANNELS), ChannelType.DM, userId = userId))
            }
            if (type == null || type == ChannelType.PROJECT) {
                queries.add(buildChannelsQuery(firestore.collection(Collections.CHANNELS), ChannelType.PROJECT))
            }
            
            val allChannels = mutableListOf<Channel>()
            for (q in queries) {
                val snapshot = q.get().await()
                allChannels.addAll(snapshot.documents.mapNotNull { channelMapper.mapToDomain(it) })
            }
            allChannels.distinctBy { it.id }
        }
    }

    override fun getUserChannelsStream(userId: String, type: ChannelType?): Flow<List<Channel>> = callbackFlow {
        val query = if (type == ChannelType.DM || type == null) {
            buildChannelsQuery(firestore.collection(Collections.CHANNELS), ChannelType.DM, userId = userId)
        } else {
            buildChannelsQuery(firestore.collection(Collections.CHANNELS), type)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { channelMapper.mapToDomain(it) }).isSuccess
            }
        }
        awaitClose { listener.remove() }
    }
    
    override suspend fun getChannelsByType(type: ChannelType, userId: String?): Result<List<Channel>> = withContext(ioDispatcher) {
        Result.runCatching {
            val query = buildChannelsQuery(firestore.collection(Collections.CHANNELS), type, userId = if (type == ChannelType.DM) userId else null)
            val snapshot = query.get().await()
            snapshot.documents.mapNotNull { channelMapper.mapToDomain(it) }
        }
    }

    override fun getChannelsByTypeStream(type: ChannelType, userId: String?): Flow<List<Channel>> = callbackFlow {
        Log.d(TAG, "getChannelsByTypeStream: Starting for type=$type, userId=$userId")
        val query = buildChannelsQuery(firestore.collection(Collections.CHANNELS), type, userId = if (type == ChannelType.DM) userId else null)
        Log.d(TAG, "getChannelsByTypeStream: Query built, adding snapshot listener")
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) { 
                Log.e(TAG, "getChannelsByTypeStream: Error in snapshot listener", error)
                close(error); 
                return@addSnapshotListener 
            }
            if (snapshot != null) {
                Log.d(TAG, "getChannelsByTypeStream: Snapshot received with ${snapshot.documents.size} documents")
                val channels = snapshot.documents.mapNotNull { channelMapper.mapToDomain(it) }
                Log.d(TAG, "getChannelsByTypeStream: Mapped to ${channels.size} channels, IDs: ${channels.map { it.id }}")
                trySend(channels).isSuccess
            } else {
                Log.w(TAG, "getChannelsByTypeStream: Null snapshot received")
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { 
            Log.d(TAG, "getChannelsByTypeStream: Flow cancelled, removing listener")
            listener.remove() 
        }
    }

    override suspend fun addDmParticipant(channelId: String, userId: String): Result<Unit> = withContext(ioDispatcher) {
        Result.runCatching {
             val channelDoc = firestore.collection(Collections.CHANNELS).document(channelId).get().await()
             val channelType = channelDoc.getString(ChannelFields.CHANNEL_TYPE)
             if (channelType != ChannelType.DM.name) { 
                 throw IllegalArgumentException("Channel $channelId is not a DM channel. Type is $channelType")
             }
             firestore.collection(Collections.CHANNELS).document(channelId).update(
                ChannelFields.dmDataPath(ChannelDmDataFields.PARTICIPANT_IDS), 
                FieldValue.arrayUnion(userId)
            ).await()
            Unit
        }
    }

    override suspend fun removeDmParticipant(channelId: String, userId: String): Result<Unit> = withContext(ioDispatcher) {
         Result.runCatching {
             val channelDoc = firestore.collection(Collections.CHANNELS).document(channelId).get().await()
             val channelType = channelDoc.getString(ChannelFields.CHANNEL_TYPE)
             if (channelType != ChannelType.DM.name) { 
                 throw IllegalArgumentException("Channel $channelId is not a DM channel. Type is $channelType")
             }
            firestore.collection(Collections.CHANNELS).document(channelId).update(
                ChannelFields.dmDataPath(ChannelDmDataFields.PARTICIPANT_IDS), 
                FieldValue.arrayRemove(userId)
            ).await()
            Unit
        }
    }

    override suspend fun getDmParticipants(channelId: String): Result<List<String>> = withContext(ioDispatcher) {
        Result.runCatching {
            val document = firestore.collection(Collections.CHANNELS).document(channelId).get().await()
            if (!document.exists()) throw NoSuchElementException("Channel $channelId not found for DM participants")
            val channel = channelMapper.mapToDomain(document) 
                ?: throw IllegalStateException("Failed to map channel $channelId for DM participants")
            
            if (channel.type != ChannelType.DM) {
                throw IllegalArgumentException("Channel $channelId is not a DM channel.")
            }
            channel.dmSpecificData?.participantIds ?: emptyList()
        }
    }

    override fun getDmParticipantsStream(channelId: String): Flow<List<String>> = callbackFlow {
        val listener = firestore.collection(Collections.CHANNELS).document(channelId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val channel = channelMapper.mapToDomain(snapshot)
                        if (channel?.type == ChannelType.DM) {
                            trySend(channel.dmSpecificData?.participantIds ?: emptyList()).isSuccess
                        } else if (channel != null) {
                            Log.w(TAG, "getDmParticipantsStream: Channel $channelId is not a DM channel.")
                            trySend(emptyList()).isSuccess
                        } else {
                             Log.w(TAG, "getDmParticipantsStream: Failed to map channel $channelId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in getDmParticipantsStream for $channelId", e)
                        close(e)
                     }
                } else {
                     Log.w(TAG, "getDmParticipantsStream: Channel $channelId does not exist.")
                     trySend(emptyList()).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }
}