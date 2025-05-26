package com.example.data.datasource.remote.friend

import android.util.Log
import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.FriendFields
import com.example.core_common.constants.FirestoreConstants.UserFields
import com.example.core_common.dispatcher.DispatcherProvider
import com.example.core_common.util.DateTimeUtil
import com.example.data.model.mapper.toFriendDomain
import com.example.domain.model.Friend
import com.example.domain.model.FriendRequestStatus
import com.example.domain.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import kotlin.Result

/**
 * Firestore 'users/{userId}/friends/{friendId}' 경로의 친구 관계 데이터 CRUD를 담당하는 데이터 소스 구현체입니다.
 */
class FriendRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val dispatcherProvider: DispatcherProvider
    // private val auth: FirebaseAuth // 직접적인 auth 사용 대신 userId를 파라미터로 받음
) : FriendRemoteDataSource {

    private fun getUserFriendsCollection(userId: String) = firestore.collection(Collections.USERS).document(userId).collection(Collections.FRIENDS)

    override fun getFriendsStream(userId: String): Flow<Result<List<Friend>>> = callbackFlow {
        val listenerRegistration = getUserFriendsCollection(userId)
            .whereEqualTo(FriendFields.STATUS, FriendRequestStatus.ACCEPTED.name) // Firestore에는 Enum 이름(String)으로 저장 가정
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error)).isSuccess
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val friends = mutableListOf<Friend>()
                    // 각 friendId에 대해 User 정보 조회 필요
                    // 이 부분은 복잡성을 증가시키므로 Friend 모델에 userName, profileImageUrl 등을 직접 저장하는 것을 고려하거나,
                    // Repository 레벨에서 User 정보를 조합하는 것이 나을 수 있음.
                    // 여기서는 Friend 문서 자체의 정보를 사용하고, 추가 정보는 Friend 모델 내 필드로 가정.
                    snapshot.documents.forEach { doc ->
                        doc.toFriendDomain(doc.id)?.let { friends.add(it) }
                    }
                    trySend(Result.success(friends)).isSuccess
                } else {
                    trySend(Result.success(emptyList())).isSuccess // 스냅샷이 null인 경우 빈 리스트
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun getFriend(userId: String, friendUserId: String): Result<Friend?> = withContext(dispatcherProvider.io) {
        runCatching {
            val friendDoc = getUserFriendsCollection(userId).document(friendUserId).get().await()
            if (friendDoc.exists()) {
                friendDoc.toFriendDomain(friendDoc.id)
            } else {
                null
            }
        }
    }

    override suspend fun sendFriendRequest(userId: String, targetUserId: String, requestTimestamp: Instant): Result<Unit> = withContext(dispatcherProvider.io) {
        runCatching {
            if (userId == targetUserId) throw IllegalArgumentException("Cannot send friend request to oneself.")

            val userFriendRef = getUserFriendsCollection(userId).document(targetUserId)
            val targetUserFriendRef = getUserFriendsCollection(targetUserId).document(userId)

            firestore.runTransaction { transaction ->
                val userFriendDoc = transaction.get(userFriendRef)
                if (userFriendDoc.exists()) {
                    throw IllegalStateException("Friend relationship already exists or pending for user: $userId to $targetUserId")
                }
                val targetUserFriendDoc = transaction.get(targetUserFriendRef)
                if (targetUserFriendDoc.exists()) {
                    throw IllegalStateException("Friend relationship already exists or pending for user: $targetUserId to $userId")
                }

                val requestData = hashMapOf(
                    FriendFields.USER_ID to targetUserId, // friends 컬렉션의 문서 ID가 friendUserId이므로, 필드에는 상대방 ID 저장
                    FriendFields.STATUS to FriendRequestStatus.PENDING_SENT.name,
                    FriendFields.TIMESTAMP to DateTimeUtil.instantToFirebaseTimestamp(requestTimestamp)
                    // acceptedAt은 PENDING_SENT 상태에서는 null
                )
                val receiveData = hashMapOf(
                    FriendFields.USER_ID to userId,
                    FriendFields.STATUS to FriendRequestStatus.PENDING_RECEIVED.name,
                    FriendFields.TIMESTAMP to DateTimeUtil.instantToFirebaseTimestamp(requestTimestamp)
                )
                transaction.set(userFriendRef, requestData)
                transaction.set(targetUserFriendRef, receiveData)
                null
            }.await()
        }
    }

    override suspend fun acceptFriendRequest(userId: String, requesterId: String, acceptTimestamp: Instant): Result<Unit> = withContext(dispatcherProvider.io) {
        runCatching {
            val userFriendRef = getUserFriendsCollection(userId).document(requesterId)
            val requesterFriendRef = getUserFriendsCollection(requesterId).document(userId)

            firestore.runTransaction { transaction ->
                val userFriendDoc = transaction.get(userFriendRef)
                val requesterFriendDoc = transaction.get(requesterFriendRef)

                if (!userFriendDoc.exists() || userFriendDoc.getString(FriendFields.STATUS) != FriendRequestStatus.PENDING_RECEIVED.name) {
                    throw IllegalStateException("No pending friend request to accept from $requesterId for user $userId.")
                }
                if (!requesterFriendDoc.exists() || requesterFriendDoc.getString(FriendFields.STATUS) != FriendRequestStatus.PENDING_SENT.name) {
                    throw IllegalStateException("No sent friend request found for $requesterId to user $userId.")
                }

                val acceptedAtTimestamp = DateTimeUtil.instantToFirebaseTimestamp(acceptTimestamp)
                val updates = mapOf(
                    FriendFields.STATUS to FriendRequestStatus.ACCEPTED.name,
                    FriendFields.ACCEPTED_AT to acceptedAtTimestamp
                )
                transaction.update(userFriendRef, updates)
                transaction.update(requesterFriendRef, updates)
                null
            }.await()
        }
    }

    override suspend fun removeFriendOrRequest(userId: String, friendUserId: String): Result<Unit> = withContext(dispatcherProvider.io) {
        runCatching {
            val userFriendRef = getUserFriendsCollection(userId).document(friendUserId)
            val friendUserRef = getUserFriendsCollection(friendUserId).document(userId)

            firestore.runTransaction { transaction ->
                transaction.delete(userFriendRef)
                transaction.delete(friendUserRef)
                null
            }.await()
        }
    }

    override suspend fun getFriendRequests(userId: String, status: FriendRequestStatus?): Result<List<Friend>> = withContext(dispatcherProvider.io) {
        runCatching {
            var query = getUserFriendsCollection(userId).whereNotEqualTo(FriendFields.STATUS, FriendRequestStatus.ACCEPTED.name) // 기본적으로 ACCEPTED 제외
            if (status != null) {
                query = getUserFriendsCollection(userId).whereEqualTo(FriendFields.STATUS, status.name)
            }

            val snapshot = query.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toFriendDomain(doc.id) // doc.id가 friendUserId
            }
        }
    }
} 