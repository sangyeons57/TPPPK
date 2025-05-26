
package com.example.data.datasource._remote

import com.example.data.model._remote.FriendDTO
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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

    private fun getMyFriendsCollection() = auth.currentUser?.uid?.let { uid ->
        firestore.collection(USERS_COLLECTION).document(uid).collection(FRIENDS_COLLECTION)
    }

    override fun observeFriends(): Flow<List<FriendDTO>> {
        return getMyFriendsCollection()?.whereEqualTo("status", "accepted")?.dataObjects()
            ?: kotlinx.coroutines.flow.flow { throw Exception("User not logged in.") }
    }

    override fun observeFriendRequests(): Flow<List<FriendDTO>> {
        return getMyFriendsCollection()?.whereEqualTo("status", "pending")?.dataObjects()
            ?: kotlinx.coroutines.flow.flow { throw Exception("User not logged in.") }
    }

    override suspend fun requestFriend(
        friendId: String,
        myName: String,
        myProfileImageUrl: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val myUid = getCurrentUserId().getOrThrow()

            firestore.runTransaction { transaction ->
                val now = Timestamp.now()
                // 내 친구 목록에 상대방 추가 (상태: requested)
                val myFriendDocRef = firestore.collection(USERS_COLLECTION).document(myUid)
                    .collection(FRIENDS_COLLECTION).document(friendId)
                val myFriendData = FriendDTO(
                    friendUid = friendId,
                    status = "requested",
                    requestedAt = now
                )
                transaction.set(myFriendDocRef, myFriendData)

                // 상대방의 친구 목록에 나를 추가 (상태: pending)
                val theirFriendDocRef = firestore.collection(USERS_COLLECTION).document(friendId)
                    .collection(FRIENDS_COLLECTION).document(myUid)
                val theirFriendData = FriendDTO(
                    friendUid = myUid,
                    friendName = myName,
                    friendProfileImageUrl = myProfileImageUrl,
                    status = "pending",
                    requestedAt = now
                )
                transaction.set(theirFriendDocRef, theirFriendData)
            }.await()
        }
    }

    override suspend fun acceptFriend(friendId: String): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val myUid = getCurrentUserId().getOrThrow()
            
            firestore.runBatch { batch ->
                val now = Timestamp.now()
                val updateData = mapOf("status" to "accepted", "acceptedAt" to now)

                // 내 목록에서 친구 상태 변경
                val myFriendDocRef = firestore.collection(USERS_COLLECTION).document(myUid)
                    .collection(FRIENDS_COLLECTION).document(friendId)
                batch.update(myFriendDocRef, updateData)

                // 상대방 목록에서 내 상태 변경
                val theirFriendDocRef = firestore.collection(USERS_COLLECTION).document(friendId)
                    .collection(FRIENDS_COLLECTION).document(myUid)
                batch.update(theirFriendDocRef, updateData)
            }.await()
        }
    }

    override suspend fun removeFriend(friendId: String): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val myUid = getCurrentUserId().getOrThrow()

            firestore.runBatch { batch ->
                // 내 목록에서 친구 삭제
                val myFriendDocRef = firestore.collection(USERS_COLLECTION).document(myUid)
                    .collection(FRIENDS_COLLECTION).document(friendId)
                batch.delete(myFriendDocRef)
                
                // 상대방 목록에서 나를 삭제
                val theirFriendDocRef = firestore.collection(USERS_COLLECTION).document(friendId)
                    .collection(FRIENDS_COLLECTION).document(myUid)
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

