package com.example.data.repository

import android.util.Log
import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.dispatcher.DispatcherProvider
import com.example.data.datasource.remote.channel.ChannelRemoteDataSource
import com.example.data.model.mapper.ChannelMapper
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.example.domain.model.RolePermission
import com.example.domain.model.channel.DmSpecificData
import com.example.domain.model.channel.ProjectSpecificData
import com.example.domain.repository.ChannelRepository
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.*
import javax.inject.Inject
import kotlin.Result
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.ChannelMode

/**
 * ChannelRepository 인터페이스의 구현체입니다.
 * ChannelRemoteDataSource를 통해 원격 데이터 작업을 위임하고 결과를 도메인 모델로 매핑합니다.
 * 메시지 관련 기능은 MessageRepository로 이전되었습니다.
 */
class ChannelRepositoryImpl @Inject constructor(
    private val channelRemoteDataSource: ChannelRemoteDataSource,
    private val firestore: FirebaseFirestore,
    private val dispatcherProvider: DispatcherProvider,
    private val channelMapper: ChannelMapper
) : ChannelRepository {

    // ---------- 기본 채널 CRUD ----------

    override suspend fun createChannel(channel: Channel): Result<Channel> = withContext(dispatcherProvider.io) {
        channelRemoteDataSource.createChannel(channel)
    }

    override suspend fun getChannel(channelId: String): Result<Channel> = withContext(dispatcherProvider.io) {
        channelRemoteDataSource.getChannel(channelId)
    }

    override suspend fun updateChannel(channel: Channel): Result<Unit> = withContext(dispatcherProvider.io) {
        channelRemoteDataSource.updateChannel(channel)
    }

    override suspend fun deleteChannel(channelId: String): Result<Unit> = withContext(dispatcherProvider.io) {
        channelRemoteDataSource.deleteChannel(channelId)
    }

    override fun getChannelStream(channelId: String): Flow<Channel> {
        return channelRemoteDataSource.getChannelStream(channelId)
    }

    // ---------- 채널 필터링 및 조회 ----------

    override suspend fun getUserChannels(userId: String, type: ChannelType?): Result<List<Channel>> = withContext(dispatcherProvider.io) {
        channelRemoteDataSource.getUserChannels(userId, type)
    }

    override fun getUserChannelsStream(userId: String, type: ChannelType?): Flow<List<Channel>> {
        return channelRemoteDataSource.getUserChannelsStream(userId, type)
    }

    override suspend fun getChannelsByType(
        type: ChannelType,
        userId: String?
    ): Result<List<Channel>> = withContext(dispatcherProvider.io) {
        channelRemoteDataSource.getChannelsByType(type, userId)
    }

    override fun getChannelsByTypeStream(
        type: ChannelType,
        userId: String?
    ): Flow<List<Channel>> {
        return channelRemoteDataSource.getChannelsByTypeStream(type, userId)
    }

    // ---------- DM 채널 참가자 관리 ----------

    override suspend fun addDmParticipant(channelId: String, userId: String): Result<Unit> = withContext(dispatcherProvider.io) {
        channelRemoteDataSource.addDmParticipant(channelId, userId)
    }

    override suspend fun removeDmParticipant(channelId: String, userId: String): Result<Unit> = withContext(dispatcherProvider.io) {
        channelRemoteDataSource.removeDmParticipant(channelId, userId)
    }

    override suspend fun getDmParticipants(channelId: String): Result<List<String>> = withContext(dispatcherProvider.io) {
        channelRemoteDataSource.getDmParticipants(channelId)
    }

    override fun getDmParticipantsStream(channelId: String): Flow<List<String>> {
        return channelRemoteDataSource.getDmParticipantsStream(channelId)
    }

    // ---------- 채널 권한 관리 (Override) ----------
    private fun getPermissionOverridesCollection(channelId: String) =
        firestore.collection(FirestoreConstants.Collections.CHANNELS)
               .document(channelId)
               .collection(FirestoreConstants.PERMISSION_OVERRIDES)

    override suspend fun setChannelPermissionOverride(
        channelId: String,
        userId: String,
        permission: RolePermission,
        value: Boolean?
    ): Result<Unit> = withContext(dispatcherProvider.io) {
        Result.runCatching {
            val overrideRef = getPermissionOverridesCollection(channelId).document(userId)
            val field = "${FirestoreConstants.ChannelPermissionOverrideFields.PERMISSIONS}.${permission.name}"
            val updateData = mutableMapOf<String, Any?>(
                FirestoreConstants.ChannelPermissionOverrideFields.USER_ID to userId,
                FirestoreConstants.ChannelPermissionOverrideFields.UPDATED_AT to DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.nowInstant())
            )
            if (value == null) {
                updateData[field] = FieldValue.delete()
            } else {
                updateData[field] = value
            }
            overrideRef.set(updateData, SetOptions.merge()).await()
            Unit
        }
    }

    override suspend fun getChannelPermissionOverridesForUser(
        channelId: String,
        userId: String
    ): Result<Map<RolePermission, Boolean>> = withContext(dispatcherProvider.io) {
        Result.runCatching {
            val snapshot = getPermissionOverridesCollection(channelId).document(userId).get().await()
            if (!snapshot.exists()) {
                emptyMap<RolePermission, Boolean>()
            } else {
                val permissionsData = snapshot.get(FirestoreConstants.ChannelPermissionOverrideFields.PERMISSIONS) as? Map<*, *>
                permissionsData?.mapNotNull { (key, value) ->
                    val permissionKey = key as? String
                    val permissionValue = value as? Boolean
                    if (permissionKey != null && permissionValue != null) {
                        try {
                            RolePermission.valueOf(permissionKey) to permissionValue
                        } catch (e: IllegalArgumentException) {
                            Log.w("ChannelRepoImpl", "Invalid permission key in Firestore: $permissionKey")
                            null
                        }
                    } else {
                        null
                    }
                }?.toMap() ?: emptyMap()
            }
        }
    }

    override suspend fun getAllChannelPermissionOverrides(channelId: String): Result<Map<String, Map<RolePermission, Boolean>>> = withContext(dispatcherProvider.io) {
        Result.runCatching {
            val snapshot = getPermissionOverridesCollection(channelId).get().await()
            snapshot.documents.associate { doc ->
                val userId = doc.id
                val permissionsData = doc.get(FirestoreConstants.ChannelPermissionOverrideFields.PERMISSIONS) as? Map<*, *>
                val userOverrides = permissionsData?.mapNotNull { (key, value) ->
                    val permissionKey = key as? String
                    val permissionValue = value as? Boolean
                    if (permissionKey != null && permissionValue != null) {
                        try {
                            RolePermission.valueOf(permissionKey) to permissionValue
                        } catch (e: IllegalArgumentException) {
                            Log.w("ChannelRepoImpl", "Invalid permission key for user $userId: $permissionKey")
                            null
                        }
                    } else {
                        null
                    }
                }?.toMap() ?: emptyMap()
                userId to userOverrides
            }
        }
    }

    // ---------- 채널 타입별 특수 기능 ----------

    override suspend fun createProjectChannel(
        name: String,
        projectId: String,
        categoryId: String?,
        channelMode: ChannelMode,
        description: String?,
        order: Int?
    ): Result<Channel> = withContext(dispatcherProvider.io) {
        val newChannelId = firestore.collection(FirestoreConstants.Collections.CHANNELS).document().id
        val channel = Channel(
            id = newChannelId,
            name = name,
            description = description,
            type = if (categoryId == null) ChannelType.PROJECT else ChannelType.CATEGORY,
            projectSpecificData = ProjectSpecificData(
                projectId = projectId,
                categoryId = categoryId,
                order = order ?: 0,
                channelMode = channelMode
            ),
            dmSpecificData = null,
            lastMessagePreview = null,
            lastMessageTimestamp = null,
            createdAt = Instant.now(),
            createdBy = null,
            updatedAt = Instant.now()
        )
        val createResult = channelRemoteDataSource.createChannel(channel)
        createResult
    }

    override suspend fun getOrCreateDmChannel(myUserId: String, otherUserId: String): Result<Channel> = withContext(dispatcherProvider.io) {
        val sortedUserIds = listOf(myUserId, otherUserId).sorted()
        val potentialChannelId = "dm_${sortedUserIds[0]}_${sortedUserIds[1]}"

        val channelsCollection = firestore.collection(FirestoreConstants.Collections.CHANNELS)
        val query = channelsCollection
            .whereEqualTo("type", ChannelType.DM.name)
            .whereArrayContainsAny("dmSpecificData.participantIds", listOf(myUserId, otherUserId))

        val snapshot = query.get().await()
        val foundChannel = snapshot.documents.mapNotNull { doc -> channelMapper.mapToDomain(doc) }
            .find { channel -> 
                channel.dmSpecificData?.participantIds?.let {
                    it.size == 2 && it.containsAll(listOf(myUserId, otherUserId))
                } == true
            }

        if (foundChannel != null) {
            Result.success(foundChannel)
        } else {
            val newChannelId = potentialChannelId
            val dmChannel = Channel(
                id = newChannelId,
                name = "DM: $myUserId & $otherUserId",
                type = ChannelType.DM,
                dmSpecificData = DmSpecificData(participantIds = sortedUserIds),
                projectSpecificData = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                lastMessagePreview = null,
                lastMessageTimestamp = null,
                description = null,
                createdBy = myUserId 
            )
            channelRemoteDataSource.createChannel(dmChannel)
        }
    }
}