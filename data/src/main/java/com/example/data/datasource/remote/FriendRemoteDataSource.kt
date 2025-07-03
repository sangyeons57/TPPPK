
package com.example.data.datasource.remote


import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.core_common.util.DateTimeUtil
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.FriendDTO
import com.example.domain.model.AggregateRoot
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.vo.CollectionPath
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 간의 친구 관계 데이터에 접근하기 위한 인터페이스입니다.
 * DefaultDatasource를 확장하여 특정 사용자의 친구 목록 내 개별 친구 문서에 대한 CRUD 및 관찰 기능을 제공합니다.
 * 친구 데이터는 `users/{userId}/friends/{friendUid}` 경로에 저장되므로,
 * 대부분의 작업 전에 `setCollection(userId)`를 호출하여 주 사용자 컨텍스트를 설정해야 합니다.
 * 일부 작업(예: 친구 요청, 수락)은 두 사용자의 데이터에 영향을 미칠 수 있으며, 구현 시 이를 고려해야 합니다.
 */
interface FriendRemoteDataSource : DefaultDatasource {

    /**
     * 특정 사용자에게 온 친구 요청("pending" 상태) 목록을 실시간으로 관찰합니다.
     * @param userId 조회할 사용자의 ID
     */
    fun observeFriendRequests(): Flow<CustomResult<List<FriendDTO>, Exception>>
    
    /**
     * 특정 사용자의 친구 목록을 실시간으로 관찰합니다.
     */
    fun observeFriendsList(): Flow<CustomResult<List<FriendDTO>, Exception>>
    
    /**
     * 사용자 이름으로 친구를 검색합니다.
     * @param username 검색할 사용자 이름
     */
    suspend fun searchFriendsByUsername(username: String): CustomResult<List<FriendDTO>, Exception>
    
    /**
     * 친구 요청을 전송합니다.
     * @param fromUserId 요청을 보내는 사용자 ID
     * @param toUsername 요청을 받을 사용자 이름
     */
    suspend fun sendFriendRequest(fromUserId: String, toUsername: String): CustomResult<Unit, Exception>
    
    /**
     * 친구 요청을 수락합니다.
     * @param userId 현재 사용자 ID
     * @param friendId 친구 ID
     */
    suspend fun acceptFriendRequest(userId: String, friendId: String): CustomResult<Unit, Exception>
    
    /**
     * 친구 요청을 거절합니다.
     * @param userId 현재 사용자 ID
     * @param friendId 친구 ID
     */
    suspend fun declineFriendRequest(userId: String, friendId: String): CustomResult<Unit, Exception>
    
    /**
     * 사용자를 차단합니다.
     * @param userId 현재 사용자 ID
     * @param friendId 차단할 사용자 ID
     */
    suspend fun blockUser(userId: String, friendId: String): CustomResult<Unit, Exception>
    
    /**
     * 친구를 삭제합니다.
     * @param userId 현재 사용자 ID
     * @param friendId 삭제할 친구 ID
     */
    suspend fun removeFriend(userId: String, friendId: String): CustomResult<Unit, Exception>
}

@Singleton
class FriendRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<FriendDTO>(firestore, FriendDTO::class.java), FriendRemoteDataSource {

    override fun observeFriendRequests(): Flow<CustomResult<List<FriendDTO>, Exception>> {
        return callbackFlow {
            val listener = collection
                .whereEqualTo(FriendDTO.STATUS, FriendStatus.PENDING.name)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { trySend(CustomResult.Failure(error)); close(error); return@addSnapshotListener }
                    val list = snapshot?.documents?.mapNotNull { it.toObject(FriendDTO::class.java) } ?: emptyList()
                    trySend(CustomResult.Success(list))
                }
            awaitClose { listener.remove() }
        }.flowOn(Dispatchers.IO)
    }
    
    override fun observeFriendsList(): Flow<CustomResult<List<FriendDTO>, Exception>> {
        return callbackFlow {
            val listener = collection
                .whereEqualTo(FriendDTO.STATUS, FriendStatus.ACCEPTED)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { trySend(CustomResult.Failure(error)); close(error); return@addSnapshotListener }
                    val list = snapshot?.documents?.mapNotNull { it.toObject(FriendDTO::class.java) } ?: emptyList()
                    trySend(CustomResult.Success(list))
                }
            awaitClose { listener.remove() }
        }.flowOn(Dispatchers.IO)
    }
    
    override suspend fun searchFriendsByUsername(username: String): CustomResult<List<FriendDTO>, Exception> {
        return resultTry {
            val snapshot = firestore.collection(CollectionPath.users.value)
                .whereEqualTo("username", username)
                .get()
                .await()
            
            val users = snapshot.documents.mapNotNull { doc ->
                FriendDTO(
                    id = doc.id,
                    name = doc.getString("username") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    status = FriendStatus.UNKNOWN,
                    createdAt = DateTimeUtil.nowDate(),
                    updatedAt = DateTimeUtil.nowDate()
                )
            }
            users
        }
    }
    
    override suspend fun sendFriendRequest(fromUserId: String, toUsername: String): CustomResult<Unit, Exception> {
        return resultTry {
            withContext(Dispatchers.IO) {
                val usersSnapshot = firestore.collection(CollectionPath.users.value)
                    .whereEqualTo("username", toUsername)
                    .get()
                    .await()
                
                if (usersSnapshot.isEmpty) {
                    throw Exception("User not found")
                }
                
                val targetUser = usersSnapshot.documents.first()
                val targetUserId = targetUser.id
                
                val fromUserDoc = firestore.collection(CollectionPath.users.value)
                    .document(fromUserId)
                    .get()
                    .await()
                
                if (!fromUserDoc.exists()) {
                    throw Exception("Current user not found")
                }
                
                val fromUserData = fromUserDoc.data
                val fromUsername = fromUserData?.get("username") as? String ?: ""
                val fromProfileImageUrl = fromUserData?.get("profileImageUrl") as? String ?: ""
                
                val targetUsername = targetUser.getString("username") ?: ""
                val targetProfileImageUrl = targetUser.getString("profileImageUrl") ?: ""
                
                val batch = firestore.batch()
                
                val fromUserFriendRef = firestore.collection(CollectionPath.userFriends(fromUserId).value)
                    .document(targetUserId)
                
                val toUserFriendRef = firestore.collection(CollectionPath.userFriends(targetUserId).value)
                    .document(fromUserId)
                
                val now = DateTimeUtil.nowDate()
                
                val fromUserFriendData = FriendDTO(
                    id = targetUserId,
                    name = targetUsername,
                    profileImageUrl = targetProfileImageUrl,
                    status = FriendStatus.REQUESTED,
                    createdAt = now,
                    updatedAt = now
                )
                
                val toUserFriendData = FriendDTO(
                    id = fromUserId,
                    name = fromUsername,
                    profileImageUrl = fromProfileImageUrl,
                    status = FriendStatus.PENDING,
                    createdAt = now,
                    updatedAt = now
                )
                
                batch.set(fromUserFriendRef, fromUserFriendData)
                batch.set(toUserFriendRef, toUserFriendData)
                
                batch.commit().await()
            }
        }
    }
    
    override suspend fun acceptFriendRequest(userId: String, friendId: String): CustomResult<Unit, Exception> {
        return resultTry {
            withContext(Dispatchers.IO) {
                val batch = firestore.batch()
                
                val userFriendRef = firestore.collection(CollectionPath.userFriends(userId).value)
                    .document(friendId)
                
                val friendUserRef = firestore.collection(CollectionPath.userFriends(friendId).value)
                    .document(userId)
                
                val now = Timestamp.now()
                
                batch.update(userFriendRef, mapOf(
                    FriendDTO.STATUS to FriendStatus.ACCEPTED.name,
                    AggregateRoot.KEY_UPDATED_AT to now
                ))
                
                batch.update(friendUserRef, mapOf(
                    FriendDTO.STATUS to FriendStatus.ACCEPTED.name,
                    AggregateRoot.KEY_UPDATED_AT to now
                ))
                
                batch.commit().await()
            }
        }
    }
    
    override suspend fun declineFriendRequest(userId: String, friendId: String): CustomResult<Unit, Exception> {
        return resultTry {
            withContext(Dispatchers.IO) {
                val batch = firestore.batch()
                
                val userFriendRef = firestore.collection(CollectionPath.userFriends(userId).value)
                    .document(friendId)
                
                val friendUserRef = firestore.collection(CollectionPath.userFriends(friendId).value)
                    .document(userId)
                
                batch.delete(userFriendRef)
                batch.delete(friendUserRef)
                
                batch.commit().await()
            }
        }
    }
    
    override suspend fun blockUser(userId: String, friendId: String): CustomResult<Unit, Exception> {
        return resultTry {
            withContext(Dispatchers.IO) {
                val batch = firestore.batch()
                
                val userFriendRef = firestore.collection(CollectionPath.userFriends(userId).value)
                    .document(friendId)
                
                val friendUserRef = firestore.collection(CollectionPath.userFriends(friendId).value)
                    .document(userId)
                
                val now = Timestamp.now()
                
                batch.update(userFriendRef, mapOf(
                    FriendDTO.STATUS to FriendStatus.BLOCKED.name,
                    AggregateRoot.KEY_UPDATED_AT to now
                ))
                
                batch.delete(friendUserRef)
                
                batch.commit().await()
            }
        }
    }
    
    override suspend fun removeFriend(userId: String, friendId: String): CustomResult<Unit, Exception> {
        return resultTry {
            withContext(Dispatchers.IO) {
                val batch = firestore.batch()
                
                val userFriendRef = firestore.collection(CollectionPath.userFriends(userId).value)
                    .document(friendId)
                
                val friendUserRef = firestore.collection(CollectionPath.userFriends(friendId).value)
                    .document(userId)
                
                batch.delete(userFriendRef)
                batch.delete(friendUserRef)
                
                batch.commit().await()
            }
        }
    }
}

