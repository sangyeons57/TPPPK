package com.example.data.datasource.remote.dm

import android.util.Log
import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.dispatcher.DispatcherProvider
import com.example.core_common.util.DateTimeUtil
import com.example.data.datasource.remote.channel.ChannelRemoteDataSource
import com.example.domain.model.Channel
import com.example.domain.model.ChannelType
import com.example.domain.model.channel.DmSpecificData
import com.example.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn

/**
 * DM 채널 관련 원격 데이터 소스 구현체입니다.
 *
 * @param firestore Firebase Firestore 인스턴스
 * @param auth Firebase Auth 인스턴스
 * @param userRepository 사용자 정보 조회를 위한 Repository
 * @param channelRemoteDataSource 채널 생성 및 조회를 위한 데이터 소스
 * @param dispatcherProvider 코루틴 디스패처 제공자
 */
@Singleton
class DmRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository, // 사용자 이름 등을 가져오기 위해 필요할 수 있습니다.
    private val channelRemoteDataSource: ChannelRemoteDataSource, // 채널 생성은 중앙화된 로직 사용
    private val dispatcherProvider: DispatcherProvider
) : DmRemoteDataSource {

    private val TAG = "DmRemoteDataSourceImpl"

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    /**
     * 두 사용자 ID를 정렬하여 고유한 DM 채널 문서 ID를 생성합니다.
     * 예: "userId1_userId2" (항상 동일한 순서 보장)
     */
    private fun generateDmChannelDocumentId(userId1: String, userId2: String): String {
        return listOf(userId1, userId2).sorted().joinToString("_")
    }

    override suspend fun getDmChannelWithUser(targetUserId: String): Result<Channel?> = withContext(dispatcherProvider.io) {
        Result.runCatching {
            val dmChannelDocId = generateDmChannelDocumentId(currentUserId, targetUserId)
            val channelResult = channelRemoteDataSource.getChannel(dmChannelDocId)
            
            if (channelResult.isSuccess) {
                val channel = channelResult.getOrNull()
                // DM 채널이고 참여자가 실제로 두 명인지 추가 확인 가능
                if (channel?.type == ChannelType.DM && channel.dmSpecificData?.participantIds?.containsAll(listOf(currentUserId, targetUserId)) == true) {
                    channel
                } else {
                    null // DM 채널이 아니거나 참여자가 일치하지 않으면 null
                }
            } else {
                // Firestore에서 문서를 찾지 못한 경우 (NoSuchElementException 등) null로 처리
                if (channelResult.exceptionOrNull() is NoSuchElementException) {
                    null
                } else {
                    throw channelResult.exceptionOrNull() ?: IllegalStateException("Failed to get DM channel")
                }
            }
        }
    }

    override suspend fun getDmChannelId(targetUserId: String): Result<String?> = withContext(dispatcherProvider.io) {
        Result.runCatching {
            val dmChannelDocId = generateDmChannelDocumentId(currentUserId, targetUserId)
            // 채널 존재 여부만 빠르게 확인하기 위해 Firestore 직접 접근 (getChannel은 매핑 비용 발생)
            val documentSnapshot = firestore.collection(FirestoreConstants.Collections.CHANNELS)
                .document(dmChannelDocId)
                .get()
                .await()
            
            if (documentSnapshot.exists()) {
                // Firestore 규칙에 부합하는지 간단히 확인 (type == DM, participantIds 포함 여부)
                val channelType = documentSnapshot.getString(FirestoreConstants.ChannelFields.CHANNEL_TYPE)
                val dmSpecificData = documentSnapshot.get(FirestoreConstants.ChannelFields.DM_SPECIFIC_DATA) as? Map<*, *>
                val participantIds = dmSpecificData?.get(FirestoreConstants.ChannelDmDataFields.PARTICIPANT_IDS) as? List<*>

                if (channelType == ChannelType.DM.name && 
                    participantIds?.containsAll(listOf(currentUserId, targetUserId)) == true &&
                    participantIds.size == 2) {
                    dmChannelDocId
                } else {
                    Log.w(TAG, "Channel $dmChannelDocId exists but is not a valid DM channel for these users.")
                    null // 문서는 존재하지만 유효한 DM 채널이 아님
                }
            } else {
                null // 채널 문서 자체가 없음
            }
        }
    }

    override suspend fun createDmChannel(targetUserId: String, channelName: String?): Result<Channel> = withContext(dispatcherProvider.io) {
        Result.runCatching {
            if (currentUserId == targetUserId) {
                throw IllegalArgumentException("Cannot create DM channel with oneself.")
            }

            // 0. 기존 DM 채널 확인 (중복 생성 방지)
            val existingChannel = getDmChannelWithUser(targetUserId).getOrNull()
            if (existingChannel != null) {
                Log.i(TAG, "DM channel with user $targetUserId already exists: ${existingChannel.id}")
                return@runCatching existingChannel // 이미 존재하면 해당 채널 반환
            }

            // 1. 채널 ID 생성
            val dmChannelDocId = generateDmChannelDocumentId(currentUserId, targetUserId)

            // 2. 채널 이름 결정 (상대방 사용자 이름 기반 또는 기본값)
            val finalChannelName = channelName ?: run {
                try {
                    userRepository.getUserStream(targetUserId).first().fold(
                        onSuccess = {
                            return@run it.name
                        },
                        onFailure =  {
                            return@run "DM with ${targetUserId.take(6)}"
                        }
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get target user's name for DM channel name.", e)
                    "DM ${dmChannelDocId.takeLast(6)}" // 사용자 이름 조회 실패 시 대체 이름
                }
            }
            
            // 3. Channel 도메인 객체 생성
            val now = DateTimeUtil.nowInstant()
            val newDmChannel = Channel(
                id = dmChannelDocId,
                name = finalChannelName,
                description = "Direct message channel", // 필요시 수정
                type = ChannelType.DM,
                dmSpecificData = DmSpecificData(
                    participantIds = listOf(currentUserId, targetUserId).sorted() // Firestore 규칙 준수
                ),
                projectSpecificData = null,
                lastMessagePreview = null,
                lastMessageTimestamp = null,
                createdBy = currentUserId,
                createdAt = now,
                updatedAt = now
            )

            // 4. 중앙화된 채널 생성 로직 사용
            channelRemoteDataSource.createChannel(newDmChannel).getOrThrow()
        }
    }

    override fun getCurrentUserDmChannelsStream(): Flow<Result<List<Channel>>> {
        // ChannelRemoteDataSource 에 getChannelsByTypeStream(type: ChannelType, userId: String?) 함수가 있다고 가정합니다.
        // 이 함수는 특정 사용자의 특정 타입 채널 목록 스트림을 반환해야 합니다.
        // channelRemoteDataSource.getChannelsByTypeStream의 반환 타입이 Flow<List<Channel>>이라고 가정하고,
        // 이를 Flow<Result<List<Channel>>>로 변환합니다.
        return channelRemoteDataSource.getChannelsByTypeStream(ChannelType.DM, currentUserId)
            .map { channels -> Result.success(channels) }
            .catch { exception -> emit(Result.failure(exception)) }
            .flowOn(dispatcherProvider.io) // UI 스레드에서 호출될 수 있으므로 IO 디스패처 사용
    }
} 