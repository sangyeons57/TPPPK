package com.example.teamnovapersonalprojectprojectingkotlin.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.Friend
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.FriendRequest
import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.User
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.FriendRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FriendRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : FriendRepository {

    private val usersCollection = firestore.collection("users")
    private val requestsCollection = firestore.collection("friendRequests")

    // Helper function to get current user UID
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getFriendsListStream(): Flow<List<Friend>> {
        val userId = getCurrentUserId() ?: return flowOf(emptyList())

        return callbackFlow {
            val friendsCollection = usersCollection.document(userId).collection("friends")
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

                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
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
                val profileSnapshots = usersCollection.whereIn("userId", chunk).get().await()
                for (doc in profileSnapshots) {
                    val userProfile = doc.toObject<User>()
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
            val friendsSnapshot = usersCollection.document(userId).collection("friends").get().await()
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
                .whereEqualTo("name", username)
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
            val areFriends = usersCollection.document(senderUid).collection("friends").document(receiverUid).get().await().exists()
            if (areFriends) {
                return Result.failure(Exception("You are already friends with $username."))
            }

            // Check if request already exists
            val existingRequest = requestsCollection
                .whereEqualTo("senderUid", senderUid)
                .whereEqualTo("receiverUid", receiverUid)
                .get()
                .await()

            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("Friend request to $username already exists."))
            }

            // 2. Create friend request document
            val requestData = hashMapOf(
                "senderUid" to senderUid,
                "receiverUid" to receiverUid,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp()
            )
            requestsCollection.add(requestData).await()
            Result.success("Friend request sent to $username.")

        } catch (e: Exception) {
            println("Error sending friend request: $e")
            Result.failure(e)
        }
    }

    override suspend fun getFriendRequests(): Result<List<FriendRequest>> {
        val receiverUid = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        return try {
            val querySnapshot = requestsCollection
                .whereEqualTo("receiverUid", receiverUid)
                .whereEqualTo("status", "pending")
                .get()
                .await()

            val requests = mutableListOf<FriendRequest>()
            for (doc in querySnapshot.documents) {
                val senderUid = doc.getString("senderUid") ?: continue

                val senderProfileDoc = usersCollection.document(senderUid).get().await()
                val senderName = senderProfileDoc.getString("name") ?: "Unknown User"
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
        return try {
            firestore.runTransaction { transaction ->
                val requestQuery = requestsCollection
                    .whereEqualTo("senderUid", senderUserId)
                    .whereEqualTo("receiverUid", receiverUid)
                    .whereEqualTo("status", "pending")
                    .limit(1)
                    .get()
                    .addOnSuccessListener { result ->
                        if (result.isEmpty) {
                            throw Exception("Friend request not found or already handled.")
                        }
                        val requestDocRef = result.documents.first().reference
                        transaction.update(requestDocRef, "status", "accepted")

                        val receiverFriendRef = usersCollection.document(receiverUid).collection("friends").document(senderUserId)
                        val senderFriendRef = usersCollection.document(senderUserId).collection("friends").document(receiverUid)

                        transaction.set(receiverFriendRef, mapOf("addedAt" to FieldValue.serverTimestamp()))
                        transaction.set(senderFriendRef, mapOf("addedAt" to FieldValue.serverTimestamp()))

                    }
                    .addOnFailureListener { exception ->
                    }

                null // 트랜잭션이 성공적으로 완료되면 null을 반환합니다.
            }.await() // runTransaction 전체에 await()를 호출하여 트랜잭션의 완료를 기다립니다.
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error accepting friend request: $e")
            Result.failure(e)
        }
    }

    override suspend fun denyFriendRequest(senderUserId: String): Result<Unit> {
        val receiverUid = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        return try {
            val requestQuery = requestsCollection
                .whereEqualTo("senderUid", senderUserId)
                .whereEqualTo("receiverUid", receiverUid)
                .whereEqualTo("status", "pending")
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
}
