
package com.example.data.datasource._remote

import android.net.Uri
import com.example.data.model._remote.UserDTO
import com.google.firebase.auth.FirebaseAuth
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
    private val storage: FirebaseStorage // FirebaseStorage 의존성 추가
) : UserRemoteDataSource {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val PROFILE_IMAGES_PATH = "profile_images"
    }

    private val usersCollection = firestore.collection(USERS_COLLECTION)

    override suspend fun getMyUserInfo(): Result<UserDTO> {
        return resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
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
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun createUser(user: UserDTO): Result<Unit> {
        return resultTry {
            usersCollection.document(user.uid).set(user).await()
            Unit
        }
    }

    override suspend fun updateUserProfile(name: String, profileImageUrl: String?): Result<Unit> {
        return resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            val updates = mapOf(
                "name" to name,
                "profileImageUrl" to profileImageUrl
            )
            usersCollection.document(uid).update(updates).await()
            Unit
        }
    }
    
    override suspend fun updateFcmToken(token: String): Result<Unit> {
        return resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            usersCollection.document(uid).update("fcmToken", token).await()
            Unit
        }
    }

    override suspend fun uploadProfileImage(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = auth.currentUser?.uid ?: throw Exception("User not logged in.")
            val storageRef = storage.reference
            // 파일 경로를 "profile_images/{userId}/{randomUuid}.jpg" 와 같이 하여 중복을 피합니다.
            val imageRef = storageRef.child("$PROFILE_IMAGES_PATH/$uid/${UUID.randomUUID()}.jpg")

            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            downloadUrl.toString()
        }
    }

    override suspend fun searchUsersByName(nameQuery: String): Result<List<UserDTO>> = withContext(Dispatchers.IO) {
        resultTry {
            // 참고: Firestore에서 효율적인 검색을 구현하려면 Algolia 같은 외부 검색 서비스를 연동하는 것이 좋습니다.
            // 아래는 간단한 "시작 문자열" 검색 예시이며, `name` 필드에 대한 색인이 필요합니다.
            val querySnapshot = usersCollection
                .orderBy("name")
                .startAt(nameQuery)
                .endAt(nameQuery + "\uf8ff")
                .limit(20) // 검색 결과 개수 제한
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

    private inline fun <T> resultTry(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Throwable) {
            if (e is java.util.concurrent.CancellationException) throw e
            Result.failure(e)
        }
    }
}

