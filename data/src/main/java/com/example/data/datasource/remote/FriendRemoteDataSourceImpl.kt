
package com.example.data.datasource.remote

import com.example.data.model._remote.FriendDTO
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.dataObjects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : FriendRemoteDataSource {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val FRIENDS_COLLECTION = "friends"
    }
    
    private fun getCurrentUserId(): Result<String> {
        val uid = auth.currentUser?.uid
        return if (uid != null) Result.success(uid) else Result.failure(Exception("User not logged in."))
    }

    // 현재 로그인한 사용자의 friends 컬렉션 참조
    private fun getMyFriendsCollectionRef() = auth.currentUser?.uid?.let { uid ->
        firestore.collection(USERS_COLLECTION).document(uid).collection(FRIENDS_COLLECTION)
    }

    // 특정 사용자의 friends 컬렉션 참조 (상대방의 컬렉션을 조작할 때 사용)
    private fun getOthersFriendsCollectionRef(otherUserId: String) =
        firestore.collection(USERS_COLLECTION).document(otherUserId).collection(FRIENDS_COLLECTION)


    override fun observeFriends(): Flow<List<FriendDTO>> {
        return getMyFriendsCollectionRef()
            ?.whereEqualTo("status", "accepted")
            ?.dataObjects()
            ?: kotlinx.coroutines.flow.flow { throw Exception("User not logged in or collection path is invalid.") }
    }

    override fun observeFriendRequests(): Flow<List<FriendDTO>> {
        // 이 함수는 "나에게 온 친구 요청"을 의미합니다.
        // 즉, 내 friends 컬렉션에서 status가 "pending"인 문서를 찾습니다.
        // 이 문서의 friendName, friendProfileImageUrl 필드에는 나에게 요청을 보낸 사람의 정보가 들어있어야 합니다.
        return getMyFriendsCollectionRef()
            ?.whereEqualTo("status", "pending")
            ?.dataObjects()
            ?: kotlinx.coroutines.flow.flow { throw Exception("User not logged in or collection path is invalid.") }
    }

    override suspend fun requestFriend(
        friendId: String, // 내가 요청을 보내는 상대방의 User ID
        myName: String,   // 상대방의 friends 컬렉션에 저장될 나의 이름
        myProfileImageUrl: String? // 상대방의 friends 컬렉션에 저장될 나의 프로필 이미지
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val myUid = getCurrentUserId().getOrThrow() // 나의 User ID

            firestore.runTransaction { transaction ->
                val now = Timestamp.now()
                
                // 1. 나의 friends 컬렉션에 상대방 정보를 저장 (상태: requested)
                //    문서 ID는 상대방의 UID (friendId)
                //    friendName, friendProfileImageUrl 필드에는 상대방의 정보를 저장해야 하나,
                //    이 단계에서는 알 수 없으므로 Repository에서 User 정보를 조회 후 업데이트하거나,
                //    Cloud Function으로 처리하는 것이 좋습니다. 여기서는 임시값을 넣습니다.
                val myFriendDocRef = getMyFriendsCollectionRef()?.document(friendId)
                    ?: throw Exception("Failed to get my friends collection reference.")
                val myFriendData = FriendDTO(
                    friendName = "Loading...", // 상대방 이름, 추후 업데이트 필요
                    friendProfileImageUrl = null, // 상대방 프로필 이미지, 추후 업데이트 필요
                    status = "requested",
                    requestedAt = now,
                    acceptedAt = null
                )
                transaction.set(myFriendDocRef, myFriendData)

                // 2. 상대방의 friends 컬렉션에 나의 정보를 저장 (상태: pending)
                //    문서 ID는 나의 UID (myUid)
                //    friendName, friendProfileImageUrl 필드에는 나의 정보를 저장.
                val theirFriendDocRef = getOthersFriendsCollectionRef(friendId).document(myUid)
                val theirFriendData = FriendDTO(
                    friendName = myName,
                    friendProfileImageUrl = myProfileImageUrl,
                    status = "pending",
                    requestedAt = now,
                    acceptedAt = null
                )
                transaction.set(theirFriendDocRef, theirFriendData)
            }.await()
        }
    }

    override suspend fun acceptFriendRequest(
        requesterId: String // 나에게 친구 요청을 보낸 사람의 User ID (내 friends 컬렉션의 문서 ID)
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val myUid = getCurrentUserId().getOrThrow()
            
            firestore.runBatch { batch ->
                val now = Timestamp.now()
                val updateData = mapOf("status" to "accepted", "acceptedAt" to now)

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
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val myUid = getCurrentUserId().getOrThrow()

            firestore.runBatch { batch ->
                // 1. 나의 friends 컬렉션에서 상대방 문서 삭제
                val myFriendDocRef = getMyFriendsCollectionRef()?.document(friendId)
                    ?: throw Exception("Failed to get my friends collection reference for friend.")
                batch.delete(myFriendDocRef)
                
                // 2. 상대방의 friends 컬렉션에서 나의 문서 삭제
                val theirFriendDocRef = getOthersFriendsCollectionRef(friendId).document(myUid)
                batch.delete(theirFriendDocRef)
            }.await()
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

