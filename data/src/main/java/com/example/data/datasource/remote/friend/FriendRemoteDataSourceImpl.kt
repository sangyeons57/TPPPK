package com.example.data.datasource.remote.friend

import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.FriendFields
import com.example.core_common.constants.FirestoreConstants.UserFields
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.remote.friend.FriendRelationshipDto
import com.example.domain.model.Friend
import com.example.domain.model.FriendRequest
import com.example.domain.model.FriendRequestStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.Result
import java.time.Instant

/**
 * 친구 관련 원격 데이터 소스 구현
 * Firebase Firestore를 사용하여 친구 목록 및 친구 요청 관련 기능을 구현합니다.
 * @param firestore Firebase Firestore 인스턴스
 * @param auth Firebase Auth 인스턴스
 */
class FriendRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : FriendRemoteDataSource {

    // 현재 사용자 ID를 가져오는 헬퍼 함수
    private val currentUserId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")

    /**
     * 친구 목록 실시간 스트림을 가져옵니다.
     * @return 친구 목록의 Flow
     */
    override fun getFriendsListStream(): Flow<List<Friend>> = callbackFlow {
        // 현재 사용자의 친구 컬렉션에 대한 참조
        val friendsRef = firestore.collection(Collections.USERS).document(currentUserId)
            .collection(Collections.FRIENDS)
            .whereEqualTo(FriendFields.STATUS, FriendRequestStatus.ACCEPTED) // 수락된 친구만 필터링
        
        // 실시간 스냅샷 리스너 설정
        val subscription = friendsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // 에러 발생 시 예외 전파
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            if (snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            // 비동기 작업을 위한 코루틴 시작
            launch(Dispatchers.IO) {
                try {
                    // 스냅샷에서 ID 목록 추출
                    val friendIds = snapshot.documents.map { it.id }
                    val friendsList = mutableListOf<Friend>()
                    
                    // 각 친구 ID에 대해 사용자 정보 가져오기
                    for (friendId in friendIds) {
                        try {
                            val friendUserDoc = firestore.collection(Collections.USERS)
                                .document(friendId)
                                .get()
                                .await()
                            
                            val friend = Friend(
                                userId = friendId,
                                userName = friendUserDoc.getString(UserFields.NAME) ?: "알 수 없음",
                                profileImageUrl = friendUserDoc.getString(UserFields.PROFILE_IMAGE_URL),
                                status = friendUserDoc.getString(FriendFields.STATUS) ?: "",
                            )
                            
                            friendsList.add(friend)
                        } catch (e: Exception) {
                            // 오류 발생 시 해당 친구는 건너뜀
                            continue
                        }
                    }
                    
                    // 친구 목록 전송
                    trySend(friendsList)
                } catch (e: Exception) {
                    // 예외 발생 시 빈 목록 전송
                    trySend(emptyList())
                }
            }
        }
        
        // 구독 취소 시 스냅샷 리스너 제거
        awaitClose { subscription.remove() }
    }

    /**
     * 친구 목록을 Firestore에서 가져옵니다.
     * @return 작업 성공 여부
     */
    override suspend fun fetchFriendsList(): Result<Unit> = try {
        // 친구 목록을 가져오는 로직 (실시간 스트림 외에 필요한 경우)
        // 여기서는 실시간 스트림 구현이 있으므로 단순히 성공을 반환
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 사용자 이름으로 친구 요청을 보냅니다.
     * @param username 친구 요청을 보낼 사용자 이름
     * @return 성공 시 메시지 또는 실패 시 예외
     */
    override suspend fun sendFriendRequest(username: String): Result<String> = try {
        val userQuery = firestore.collection(Collections.USERS)
            .whereEqualTo(UserFields.NAME, username)
            .limit(1)
            .get()
            .await()
        
        if (userQuery.isEmpty) {
            Result.failure(IllegalArgumentException("사용자를 찾을 수 없습니다: $username"))
        } else {
            val targetUser = userQuery.documents.first()
            val targetUserId = targetUser.id
            
            if (targetUserId == currentUserId) {
                Result.failure(IllegalArgumentException("자신에게 친구 요청을 보낼 수 없습니다."))
            } else {
                val existingFriendDoc = firestore.collection(Collections.USERS).document(currentUserId)
                    .collection(Collections.FRIENDS).document(targetUserId)
                    .get()
                    .await()
                
                if (existingFriendDoc.exists()) {
                    val status = FriendRequestStatus.fromString(existingFriendDoc.getString(FriendFields.STATUS))
                    when (status) {
                        FriendRequestStatus.ACCEPTED -> Result.failure(IllegalArgumentException("이미 친구입니다."))
                        FriendRequestStatus.PENDING_SENT -> Result.failure(IllegalArgumentException("이미 친구 요청을 보냈습니다."))
                        FriendRequestStatus.PENDING_RECEIVED -> Result.failure(IllegalArgumentException("상대방이 이미 친구 요청을 보냈습니다."))
                        else -> Result.failure(IllegalArgumentException("알 수 없는 상태입니다: $status"))
                    }
                } else {
                    val nowAsTimestamp = DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.nowInstant())
                    
                    val senderRequestDto = FriendRelationshipDto(
                        status = FriendRequestStatus.PENDING_SENT,
                        timestamp = nowAsTimestamp,
                        acceptedAt = null
                    )
                    val receiverRequestDto = FriendRelationshipDto(
                        status = FriendRequestStatus.PENDING_RECEIVED,
                        timestamp = nowAsTimestamp,
                        acceptedAt = null
                    )
                    
                    firestore.collection(Collections.USERS).document(currentUserId)
                        .collection(Collections.FRIENDS).document(targetUserId).set(senderRequestDto).await()
                    
                    firestore.collection(Collections.USERS).document(targetUserId)
                        .collection(Collections.FRIENDS).document(currentUserId).set(receiverRequestDto).await()
                    
                    Result.success("친구 요청을 보냈습니다.")
                }
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 받은 친구 요청 목록을 가져옵니다.
     * @return 친구 요청 목록
     */
    override suspend fun getFriendRequests(): Result<List<FriendRequest>> = try {
        // 받은 친구 요청 쿼리
        val requestsQuery = firestore.collection(Collections.USERS).document(currentUserId)
            .collection(Collections.FRIENDS)
            .whereEqualTo(FriendFields.STATUS, FriendRequestStatus.PENDING_RECEIVED)
            .get()
            .await()
        
        // 친구 요청 목록 매핑
        val requests = requestsQuery.documents.mapNotNull { doc ->
            val requesterId = doc.id
            try {
                // 요청자 정보 가져오기
                val requesterDoc = firestore.collection(Collections.USERS).document(requesterId).get().await()
                val requesterDocData = requesterDoc.data
                if (requesterDocData != null) {
                    FriendRequest(
                        userId = requesterId,
                        userName = requesterDocData[UserFields.NAME] as? String ?: "알 수 없음",
                        profileImageUrl = requesterDocData[UserFields.PROFILE_IMAGE_URL] as? String?,
                        timestamp = DateTimeUtil.firebaseTimestampToInstant(doc.getTimestamp(FriendFields.TIMESTAMP)) ?: Instant.now()
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null // 오류 발생 시 해당 요청은 건너뜀
            }
        }
        
        Result.success(requests)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 친구 요청을 수락합니다.
     * @param userId 수락할 친구 요청의 사용자 ID
     * @return 작업 성공 여부
     */
    override suspend fun acceptFriendRequest(userId: String): Result<Unit> = try {
        // 받은 요청 상태 확인
        val requestDoc = firestore.collection(Collections.USERS).document(currentUserId)
            .collection(Collections.FRIENDS).document(userId)
            .get()
            .await()

        val status = FriendRequestStatus.fromString(requestDoc.getString(FriendFields.STATUS))
        if (!requestDoc.exists() || status != FriendRequestStatus.PENDING_RECEIVED) {
            Result.failure(IllegalArgumentException("유효한 친구 요청이 없습니다."))
        } else {
            // 내 친구 목록 업데이트
            firestore.collection(Collections.USERS).document(currentUserId)
                .collection(Collections.FRIENDS).document(userId)
                .update(mapOf(
                    FriendFields.STATUS to FriendRequestStatus.ACCEPTED,
                    FriendFields.ACCEPTED_AT to DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.nowInstant())
                ))
                .await()
            
            // 상대방 친구 목록 업데이트
            firestore.collection(Collections.USERS).document(userId)
                .collection(Collections.FRIENDS).document(currentUserId)
                .update(mapOf(
                    FriendFields.STATUS to FriendRequestStatus.ACCEPTED,
                    FriendFields.ACCEPTED_AT to DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.nowInstant())
                ))
                .await()
            
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 친구 요청을 거절합니다.
     * @param userId 거절할 친구 요청의 사용자 ID
     * @return 작업 성공 여부
     */
    override suspend fun denyFriendRequest(userId: String): Result<Unit> = try {
        // 받은 요청 상태 확인
        val requestDoc = firestore.collection(Collections.USERS).document(currentUserId)
            .collection(Collections.FRIENDS).document(userId)
            .get()
            .await()

        val status = FriendRequestStatus.fromString(requestDoc.getString(FriendFields.STATUS))

        if (!requestDoc.exists() || status != FriendRequestStatus.PENDING_RECEIVED) {
            Result.failure(IllegalArgumentException("유효한 친구 요청이 없습니다."))
        } else {
            // 내 친구 목록에서 삭제
            firestore.collection(Collections.USERS).document(currentUserId)
                .collection(Collections.FRIENDS).document(userId)
                .delete()
                .await()
            
            // 상대방 친구 목록에서 삭제
            firestore.collection(Collections.USERS).document(userId)
                .collection(Collections.FRIENDS).document(currentUserId)
                .delete()
                .await()
            
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getFriendRelationshipsStream(currentUserId: String): Flow<Result<List<Pair<String, FriendRelationshipDto>>>> = callbackFlow {
        val userFriendsCollection = firestore.collection(Collections.USERS)
            .document(currentUserId)
            .collection(Collections.FRIENDS)

        val listenerRegistration = userFriendsCollection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(Result.failure(error))
                close(error)
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val relationships = snapshots.documents.mapNotNull { doc ->
                    doc.toObject<FriendRelationshipDto>()?.let { dto ->
                        Pair(doc.id, dto)
                    }
                }
                trySend(Result.success(relationships))
            } else {
                trySend(Result.failure(IllegalStateException("Snapshots and error were both null")))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun sendFriendRequest(currentUserId: String, targetUserId: String): Result<Unit> = try {
        val now = DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.nowInstant())
        val requestDto = FriendRelationshipDto(status = FriendRequestStatus.PENDING_SENT, timestamp = now, acceptedAt = null)
        val receivedDto = FriendRelationshipDto(status = FriendRequestStatus.PENDING_RECEIVED, timestamp = now, acceptedAt = null)

        firestore.batch().apply {
            set(
                firestore.collection(Collections.USERS).document(currentUserId)
                    .collection(Collections.FRIENDS).document(targetUserId),
                requestDto
            )
            set(
                firestore.collection(Collections.USERS).document(targetUserId)
                    .collection(Collections.FRIENDS).document(currentUserId),
                receivedDto
            )
        }.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun acceptFriendRequest(currentUserId: String, requesterId: String): Result<Unit> = try {
        val now = DateTimeUtil.instantToFirebaseTimestamp(DateTimeUtil.nowInstant())
        val updates = mapOf(
            FriendFields.STATUS to FriendRequestStatus.ACCEPTED,
            FriendFields.ACCEPTED_AT to now
        )

        firestore.batch().apply {
            update(
                firestore.collection(Collections.USERS).document(currentUserId)
                    .collection(Collections.FRIENDS).document(requesterId),
                updates
            )
            update(
                firestore.collection(Collections.USERS).document(requesterId)
                    .collection(Collections.FRIENDS).document(currentUserId),
                updates
            )
        }.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removeOrDenyFriend(currentUserId: String, friendId: String): Result<Unit> = try {
        firestore.batch().apply {
            delete(
                firestore.collection(Collections.USERS).document(currentUserId)
                    .collection(Collections.FRIENDS).document(friendId)
            )
            delete(
                firestore.collection(Collections.USERS).document(friendId)
                    .collection(Collections.FRIENDS).document(currentUserId)
            )
        }.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
} 