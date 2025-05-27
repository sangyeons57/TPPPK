
package com.example.data.datasource.remote

import android.net.Uri
import com.example.data.model._remote.UserDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRemoteDataSource {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val PROFILE_IMAGES_PATH = "profile_images"
    }

    private val usersCollection = firestore.collection(USERS_COLLECTION)
    
    private fun getCurrentUserIdOrThrow(): String {
        return auth.currentUser?.uid ?: throw Exception("User not logged in.")
    }

    override suspend fun getMyUserInfo(): Result<UserDTO> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()
            val document = usersCollection.document(uid).get().await()
            document.toObject(UserDTO::class.java)
                ?: throw Exception("User data could not be parsed.")
        }
    }

    override fun observeUser(userId: String): Flow<Result<UserDTO>> = callbackFlow {
        val listenerRegistration = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(UserDTO::class.java)
                    if (user != null) {
                        trySend(Result.success(user))
                    } else {
                        trySend(Result.failure(Exception("Failed to parse user data.")))
                    }
                } else {
                     trySend(Result.failure(Exception("User document does not exist.")))
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun createUser(user: UserDTO): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            usersCollection.document(user.uid).set(user).await()
            Unit
        }
    }

    override suspend fun updateUserProfile(name: String, profileImageUrl: String?): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()
            val updates = mutableMapOf<String, Any?>()
            updates["name"] = name
            updates["updatedAt"] = FieldValue.serverTimestamp()
            profileImageUrl?.let { updates["profileImageUrl"] = it }

            usersCollection.document(uid).update(updates).await()
            Unit
        }
    }
    
    override suspend fun updateFcmToken(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()
            usersCollection.document(uid).update("fcmToken", token).await()
            Unit
        }
    }

    override suspend fun uploadProfileImage(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()
            val storageRef = storage.reference
            val imageRef = storageRef.child("$PROFILE_IMAGES_PATH/$uid/${UUID.randomUUID()}.jpg")

            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            downloadUrl.toString()
        }
    }

    override suspend fun searchUsersByName(nameQuery: String): Result<List<UserDTO>> = withContext(Dispatchers.IO) {
        resultTry {
            // Firestore에서 효율적인 검색을 구현하려면 Algolia 같은 외부 검색 서비스를 연동하는 것이 좋습니다.
            // "name" 필드에 대한 색인이 필요합니다.
            val querySnapshot = usersCollection
                .orderBy("name")
                .startAt(nameQuery)
                .endAt(nameQuery + "\uf8ff")
                .limit(20) 
                .get()
                .await()
            
            querySnapshot.toObjects()
        }
    }

    override suspend fun checkNicknameAvailability(nickname: String): Result<Boolean> = withContext(Dispatchers.IO) {
        resultTry {
            val querySnapshot = usersCollection
                .whereEqualTo("name", nickname)
                .limit(1)
                .get()
                .await()
            
            querySnapshot.isEmpty
        }
    }

    override suspend fun updateUserStatus(status: String): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()
            usersCollection.document(uid).update(mapOf(
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()
            Unit
        }
    }

    override suspend fun updateUserAccountStatus(accountStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()
            usersCollection.document(uid).update(mapOf(
                "accountStatus" to accountStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()
            Unit
        }
    }

    override suspend fun updateUserMemo(memo: String): Result<Unit> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()
            usersCollection.document(uid).update(mapOf(
                "memo" to memo,
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()
            Unit
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

