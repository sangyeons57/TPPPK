package com.example.data.repository

import com.example.core_logging.SentryUtil
import com.example.domain.model.Friend
import com.example.domain.model.FriendRequest
import com.example.domain.model.User
import com.example.domain.repository.FriendRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.example.data.util.FirestoreConstants as FC

class FriendRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : FriendRepository {

    private val usersCollection = firestore.collection(FC.Collections.USERS)
    private val requestsCollection = firestore.collection(FC.Collections.FRIEND_REQUESTS)

    // Helper function to get current user UID
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getFriendsListStream(): Flow<List<Friend>> {
        val userId = getCurrentUserId() ?: return flowOf(emptyList())

        return callbackFlow {
            val friendsCollection = usersCollection.document(userId).collection(FC.Users.FriendsSubcollection.NAME)
            val listenerRegistration = friendsCollection.addSnapshotListener { snapshots, error ->
                if (error != null) {
                    println("Error listening to friends: $error")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshots == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val friendUids = snapshots.documents.map { it.id }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val friendsList = fetchFriendProfiles(friendUids)
                        trySend(friendsList)
                    } catch (e: Exception) {
                        println("Error fetching friend profiles: $e")
                        trySend(emptyList())
                    }
                }
            }
            awaitClose {
                println("Closing friends listener")
                listenerRegistration.remove()
            }
        }
    }

    // Helper to fetch profiles for a list of UIDs
    private suspend fun fetchFriendProfiles(friendUids: List<String>): List<Friend> {
        if (friendUids.isEmpty()) return emptyList()
        val friends = mutableListOf<Friend>()
        friendUids.chunked(30).forEach { chunk ->
            try {
                val profileSnapshots = usersCollection.whereIn(FC.Users.Fields.USER_ID, chunk).get().await()
                for (doc in profileSnapshots) {
                    val userProfile = doc.toUser()
                    if (userProfile != null) {
                        friends.add(
                            Friend(
                                userId = doc.id,
                                userName = userProfile.name,
                                profileImageUrl = userProfile.profileImageUrl, // Assuming this field exists
                                status = userProfile.status ?: "Offline" // Assuming this field exists
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                println("Error fetching profile chunk: $e")
            }
        }
        return friends
    }

    override suspend fun fetchFriendsList(): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        return try {
            val friendsSnapshot = usersCollection.document(userId).collection(FC.Users.FriendsSubcollection.NAME).get().await()
            val friendUids = friendsSnapshot.documents.map { it.id }
            println("Fetched ${friendUids.size} friend UIDs for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error fetching friends list manually: $e")
            Result.failure(e)
        }
    }

    override suspend fun sendFriendRequest(username: String): Result<String> {
        val senderUid = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))

        return try {
            // 1. Find the user by username
            val querySnapshot = usersCollection
                .whereEqualTo(FC.Users.Fields.NAME, username)
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("User '$username' not found."))
            }
            val receiverDoc = querySnapshot.documents.first()
            val receiverUid = receiverDoc.id

            if (senderUid == receiverUid) {
                return Result.failure(Exception("You cannot send a friend request to yourself."))
            }

            // Check if already friends
            val areFriends = usersCollection.document(senderUid).collection(FC.Users.FriendsSubcollection.NAME).document(receiverUid).get().await().exists()
            if (areFriends) {
                return Result.failure(Exception("You are already friends with $username."))
            }

            // Check if request already exists
            val existingRequest = requestsCollection
                .whereEqualTo(FC.FriendRequests.Fields.SENDER_UID, senderUid)
                .whereEqualTo(FC.FriendRequests.Fields.RECEIVER_UID, receiverUid)
                .get()
                .await()

            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("Friend request to $username already exists."))
            }

            // 2. Create friend request document
            val requestData = hashMapOf(
                FC.FriendRequests.Fields.SENDER_UID to senderUid,
                FC.FriendRequests.Fields.RECEIVER_UID to receiverUid,
                FC.FriendRequests.Fields.STATUS to FC.FriendRequests.StatusValues.PENDING,
                FC.FriendRequests.Fields.CREATED_AT to FieldValue.serverTimestamp()
            )
            requestsCollection.add(requestData).await()
            Result.success("Friend request sent to $username.")

        } catch (e: Exception) {
            println("Error sending friend request: $e")
            SentryUtil.captureError(e, "Error sending friend request")
            Result.failure(e)
        }
    }

    override suspend fun getFriendRequests(): Result<List<FriendRequest>> {
        val receiverUid = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        return try {
            val querySnapshot = requestsCollection
                .whereEqualTo(FC.FriendRequests.Fields.RECEIVER_UID, receiverUid)
                .whereEqualTo(FC.FriendRequests.Fields.STATUS, FC.FriendRequests.StatusValues.PENDING)
                .get()
                .await()

            val requests = mutableListOf<FriendRequest>()
            for (doc in querySnapshot.documents) {
                val senderUid = doc.getString(FC.FriendRequests.Fields.SENDER_UID) ?: continue

                val senderProfileDoc = usersCollection.document(senderUid).get().await()
                val senderName = senderProfileDoc.getString(FC.Users.Fields.NAME) ?: "Unknown User"
                val senderProfileImageUrl = senderProfileDoc.getString("profileImageUrl")

                requests.add(
                    FriendRequest(
                        userId = senderUid,
                        userName = senderName,
                        profileImageUrl = senderProfileImageUrl
                    )
                )
            }
            Result.success(requests)
        } catch (e: Exception) {
            println("Error getting friend requests: $e")
            Result.failure(e)
        }
    }

    override suspend fun acceptFriendRequest(senderUserId: String): Result<Unit> {
        val receiverUid = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))

        // 트랜잭션 외부에서 처리할 쿼리 객체 생성
        val requestQuery: Query = requestsCollection
            .whereEqualTo(FC.FriendRequests.Fields.SENDER_UID, senderUserId)
            .whereEqualTo(FC.FriendRequests.Fields.RECEIVER_UID, receiverUid)
            .whereEqualTo(FC.FriendRequests.Fields.STATUS, FC.FriendRequests.StatusValues.PENDING)
            .limit(1)

        return try {
            // 1. 트랜잭션 외부: 먼저 요청 문서가 있는지 확인하고 DocumentReference 얻기
            val requestSnapshot = requestQuery.get().await() // QuerySnapshot 가져오기 (await 사용)

            if (requestSnapshot.isEmpty) {
                // 처리할 요청이 없으면 여기서 실패 처리
                return Result.failure(Exception("Friend request not found or already handled."))
            }
            // 요청 문서의 DocumentReference 가져오기
            val requestDocRef: DocumentReference = requestSnapshot.documents.first().reference

            // 2. 트랜잭션 시작
            firestore.runTransaction { transaction ->
                // 2a. 트랜잭션 내부: 외부에서 얻은 참조로 문서를 다시 읽어 최신 상태 확인
                val freshRequestDoc = transaction.get(requestDocRef) // DocumentReference 사용!

                // 2b. 문서 존재 및 상태 재확인
                if (!freshRequestDoc.exists()) {
                    // 트랜잭션 도중 문서가 삭제된 경우
                    throw FirebaseFirestoreException(
                        "Friend request document disappeared during transaction.",
                        FirebaseFirestoreException.Code.ABORTED
                    )
                }
                val currentStatus = freshRequestDoc.getString(FC.FriendRequests.Fields.STATUS)
                if (currentStatus != FC.FriendRequests.StatusValues.PENDING) {
                    // 트랜잭션 도중 다른 곳에서 이미 처리한 경우
                    throw FirebaseFirestoreException(
                        "Friend request was already handled.",
                        FirebaseFirestoreException.Code.ABORTED
                    )
                }

                // 3. 요청 문서 상태 업데이트 (transaction 사용)
                transaction.update(requestDocRef, FC.FriendRequests.Fields.STATUS, FC.FriendRequests.StatusValues.ACCEPTED)

                // 4. 양쪽 사용자 friends 서브컬렉션에 친구 정보 추가 (transaction 사용)
                val receiverFriendRef = usersCollection.document(receiverUid)
                    .collection(FC.Users.FriendsSubcollection.NAME).document(senderUserId)
                val senderFriendRef = usersCollection.document(senderUserId)
                    .collection(FC.Users.FriendsSubcollection.NAME).document(receiverUid)

                val friendData = mapOf(FC.Users.FriendsSubcollection.Fields.ADDED_AT to FieldValue.serverTimestamp())

                transaction.set(receiverFriendRef, friendData)
                transaction.set(senderFriendRef, friendData)

                // 성공 시 null 반환
                null
            }.await() // runTransaction Task 완료 기다림

            // 트랜잭션 성공
            Result.success(Unit)

        } catch (e: Exception) {
            // 트랜잭션 실패 또는 외부 .await() 호출 실패 처리
            println("Error accepting friend request: $e")
            SentryUtil.captureError(e, "Error accepting friend request")
            Result.failure(e)
        }
    }

    override suspend fun denyFriendRequest(senderUserId: String): Result<Unit> {
        val receiverUid = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        return try {
            val requestQuery = requestsCollection
                .whereEqualTo(FC.FriendRequests.Fields.SENDER_UID, senderUserId)
                .whereEqualTo(FC.FriendRequests.Fields.RECEIVER_UID, receiverUid)
                .whereEqualTo(FC.FriendRequests.Fields.STATUS, FC.FriendRequests.StatusValues.PENDING)
                .limit(1)
                .get()
                .await()

            if (!requestQuery.isEmpty) {
                val requestDocRef = requestQuery.documents.first().reference
                requestDocRef.delete().await()
            } else {
                println("Deny request: Request already handled or not found.")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error denying friend request: $e")
            Result.failure(e)
        }
    }

    override suspend fun getDmChannelId(friendUserId: String): Result<String> {
        val userUid = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        val channelId = if (userUid < friendUserId) {
            "${userUid}_${friendUserId}"
        } else {
            "${friendUserId}_${userUid}"
        }
        println("Generated DM Channel ID: $channelId")
        return Result.success(channelId)
    }

    fun DocumentSnapshot.toUser(): User? {
        return data?.let { data ->
            User(
                userId = id,
                name = data[FC.Users.Fields.NAME] as? String ?: "", // 예시 필드, 실제 필드에 맞게 수정
                profileImageUrl = data[FC.Users.Fields.PROFILE_IMAGE_URL] as? String,
                status = data[FC.Users.Fields.STATUS] as? String ?: FC.Users.StatusValues.OFFLINE
                // ... 기타 필드
            )
        }
    }
}
