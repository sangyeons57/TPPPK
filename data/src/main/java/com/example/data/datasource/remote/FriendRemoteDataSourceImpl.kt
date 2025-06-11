
package com.example.data.datasource.remote

import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.data.model.remote.FriendDTO
import com.example.domain.model.enum.FriendStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : FriendRemoteDataSource {

    // FirestoreConstants에서 정의된 상수 사용
    
    private fun getCurrentUserId(): Result<String> {
        val uid = auth.currentUser?.uid
        return if (uid != null) Result.success(uid) else Result.failure(Exception("User not logged in."))
    }

    // 현재 로그인한 사용자의 friends 컬렉션 참조
    private fun getMyFriendsCollectionRef() = auth.currentUser?.uid?.let { uid ->
        firestore.collection(FirestoreConstants.Collections.USERS).document(uid).collection(FirestoreConstants.Users.Friends.COLLECTION_NAME)
    }

    // 특정 사용자의 friends 컬렉션 참조 (상대방의 컬렉션을 조작할 때 사용)
    private fun getOthersFriendsCollectionRef(otherUserId: String) =
        firestore.collection(FirestoreConstants.Collections.USERS).document(otherUserId).collection(FirestoreConstants.Users.Friends.COLLECTION_NAME)


    override fun observeFriends(): Flow<CustomResult<List<FriendDTO>, Exception>> {
        return observeFriends(auth.currentUser?.uid ?: "")
    }
    
    override fun observeFriends(userId: String): Flow<CustomResult<List<FriendDTO>, Exception>> {
        return flow {
            try {
                if (userId.isEmpty()) {
                    emit(CustomResult.Failure(Exception("User ID is empty")))
                    return@flow
                }
                
                val friendsCollection = firestore.collection(FirestoreConstants.Collections.USERS)
                    .document(userId)
                    .collection(FirestoreConstants.Users.Friends.COLLECTION_NAME)
                    .whereEqualTo(FirestoreConstants.Users.Friends.STATUS, FriendStatus.ACCEPTED)
                    .snapshots()
                
                friendsCollection.collect { snapshot ->
                    val friendDTOs = snapshot.documents.mapNotNull { it.toObject(FriendDTO::class.java) }
                    emit(CustomResult.Success(friendDTOs))
                }
            } catch (e: Exception) {
                emit(CustomResult.Failure(e))
            }
        }
    }
    
    override fun observeFriendRequests(userId: String): Flow<CustomResult<List<FriendDTO>, Exception>> {
        return flow {
            try {
                if (userId.isEmpty()) {
                    emit(CustomResult.Failure(Exception("User ID is empty")))
                    return@flow
                }
                
                val friendsCollection = firestore.collection(FirestoreConstants.Collections.USERS)
                    .document(userId)
                    .collection(FirestoreConstants.Users.Friends.COLLECTION_NAME)
                    .whereEqualTo(FirestoreConstants.Users.Friends.STATUS, FriendStatus.PENDING)
                    .snapshots()
                
                friendsCollection.collect { snapshot ->
                    val friendDTOs = snapshot.documents.mapNotNull { it.toObject(FriendDTO::class.java) }
                    emit(CustomResult.Success(friendDTOs))
                }
            } catch (e: Exception) {
                emit(CustomResult.Failure(e))
            }
        }
    }

    override suspend fun requestFriend(
        friendDTO: FriendDTO,
        myDTO: FriendDTO
    ): CustomResult<Unit, Exception> {
        return resultTry {
            val myUid = getCurrentUserId().getOrThrow() // 나의 User ID

            firestore.runTransaction { transaction ->
                val now = Timestamp.now()
                
                // 1. 나의 friends 컬렉션에 상대방 정보를 저장 (상태: requested)
                //    문서 ID는 상대방의 UID (friendId)
                //    friendName, friendProfileImageUrl 필드에는 상대방의 정보를 저장해야 하나,
                //    이 단계에서는 알 수 없으므로 Repository에서 User 정보를 조회 후 업데이트하거나,
                //    Cloud Function으로 처리하는 것이 좋습니다. 여기서는 임시값을 넣습니다.
                val myFriendDocRef = getMyFriendsCollectionRef()?.document(friendDTO.friendUid)
                    ?: throw Exception("Failed to get my friends collection reference.")
                val myFriendData = FriendDTO(
                    status = FriendStatus.PENDING,
                    requestedAt = now,
                    acceptedAt = null
                )
                transaction.set(myFriendDocRef, myFriendData)

                // 2. 상대방의 friends 컬렉션에 나의 정보를 저장 (상태: pending)
                //    문서 ID는 나의 UID (myUid)
                //    friendName, friendProfileImageUrl 필드에는 나의 정보를 저장.
                val theirFriendDocRef = getOthersFriendsCollectionRef(friendDTO.friendUid).document(myDTO.friendUid)
                val theirFriendData = FriendDTO(
                    friendUid = myDTO.friendUid,
                    status = FriendStatus.PENDING,
                    requestedAt = now,
                    acceptedAt = null
                )
                transaction.set(theirFriendDocRef, theirFriendData)
            }.await()
        }
    }

    override suspend fun acceptFriendRequest(
        requesterId: String // 나에게 친구 요청을 보낸 사람의 User ID (내 friends 컬렉션의 문서 ID)
    ): CustomResult<Unit, Exception> {
        return resultTry {
            val myUid = getCurrentUserId().getOrThrow()

            firestore.runBatch { batch ->
                val now = Timestamp.now()
                val updateData = mapOf(FirestoreConstants.Users.Friends.STATUS to FriendStatus.ACCEPTED, FirestoreConstants.Users.Friends.ACCEPTED_AT to now)

                // 1. 나의 friends 컬렉션에서 해당 요청 문서의 상태를 "accepted"로 변경
                val myFriendDocRef = getMyFriendsCollectionRef()?.document(requesterId)
                    ?: throw Exception("Failed to get my friends collection reference for requester.")
                batch.update(myFriendDocRef, updateData)

                // 2. 상대방(요청자)의 friends 컬렉션에서 나의 문서 상태를 "accepted"로 변경
                val theirFriendDocRef = getOthersFriendsCollectionRef(requesterId).document(myUid)
                batch.update(theirFriendDocRef, updateData)
            }.await()
        }
    }

    override suspend fun removeOrDenyFriend(
        friendId: String // 나와의 관계를 끊을 상대방 User ID (내 friends 컬렉션의 문서 ID)
    ): CustomResult<Unit, Exception> {
        return resultTry {
            val myUid = getCurrentUserId().getOrThrow()

            firestore.runBatch { batch ->
                // 1. 나의 friends 컬렉션에서 상대방 문서 삭제
                val myFriendDocRef = getMyFriendsCollectionRef()?.document(friendId)
                    ?: throw Exception("Failed to get my friends collection reference for friend.")
                batch.delete(myFriendDocRef)

                //2. 상대방의 friends 컬렉션에서 나의 문서 삭제
                val theirFriendDocRef = getOthersFriendsCollectionRef(friendId).document(myUid)
                batch.delete(theirFriendDocRef) 

            }.await()
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

