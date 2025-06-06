package com.example.data.datasource.remote

import android.net.Uri
import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.model.remote.UserDTO
import com.example.data.model.remote.ProjectsWrapperDTO
import com.example.data.model.remote.DMWrapperDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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


    private val usersCollection = firestore.collection(FirestoreConstants.Collections.USERS)
    
    private fun getCurrentUserIdOrThrow(): String {
        return auth.currentUser?.uid ?: throw Exception("User not logged in.")
    }

    override suspend fun getMyUserInfo(): CustomResult<UserDTO, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()
            val document = usersCollection.document(uid).get().await()
            document.toObject(UserDTO::class.java)
                ?: throw Exception("User data could not be parsed.")
        }
    }

    override fun observeUser(userId: String): Flow<CustomResult<UserDTO, Exception>> = callbackFlow {
        val listenerRegistration = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(CustomResult.Failure(error))
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(UserDTO::class.java)
                    if (user != null) {
                        trySend(CustomResult.Success(user))
                    } else {
                        trySend(CustomResult.Failure(Exception("Failed to parse user data.")))
                    }
                } else {
                     trySend(CustomResult.Failure(Exception("User document does not exist.")))
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun createUser(user: UserDTO): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            usersCollection.document(user.uid).set(user).await()
            Unit
        }
    }

    override suspend fun updateUserProfile(name: String, userDTO: UserDTO): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()

            usersCollection.document(uid).set(userDTO, SetOptions.merge()).await()
            Unit
        }
    }
    
    override suspend fun updateFcmToken(token: String): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()
            usersCollection.document(uid).update("fcmToken", token).await()
            Unit
        }
    }

    override suspend fun uploadProfileImage(imageUri: Uri): CustomResult<String, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()
            val storageRef = storage.reference
            val imageRef = storageRef.child("profile_images/$uid/${UUID.randomUUID()}.jpg")

            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            downloadUrl.toString()
        }
    }

    override suspend fun searchUsersByName(nameQuery: String): CustomResult<List<UserDTO>, Exception> = withContext(Dispatchers.IO) {
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
            
            querySnapshot.map{snapshot -> snapshot.toObject(UserDTO::class.java)}
        }
    }

    override suspend fun searchUsersByName(
        nameQuery: String,
        maxResults: Int
    ): CustomResult<List<UserDTO>, Exception> {
        TODO("Not yet implemented")
    }

    override suspend fun checkNicknameAvailability(nickname: String): CustomResult<Boolean, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val querySnapshot = usersCollection
                .whereEqualTo("name", nickname)
                .limit(1)
                .get()
                .await()
            
            querySnapshot.isEmpty
        }
    }

    override suspend fun updateUserStatus(status: String): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()
            usersCollection.document(uid).update(mapOf(
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()
            Unit
        }
    }

    override suspend fun updateUserAccountStatus(accountStatus: String): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()
            usersCollection.document(uid).update(mapOf(
                "accountStatus" to accountStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()
            Unit
        }
    }

    override suspend fun updateUserMemo(memo: String): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val uid = getCurrentUserIdOrThrow()
            usersCollection.document(uid).update(mapOf(
                "memo" to memo,
                "updatedAt" to FieldValue.serverTimestamp()
            )).await()
            Unit
        }
    }

    override suspend fun updateUserProfileImageUrl(userId: String, imageUrl: String?): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val userDocRef = usersCollection.document(userId)
            val updateData = hashMapOf<String, Any?>()

            // FirestoreConstants.Users.PROFILE_IMAGE_URL를 사용합니다.
            updateData[FirestoreConstants.Users.PROFILE_IMAGE_URL] = imageUrl
            
            // FirestoreConstants.Users.UPDATED_AT를 사용합니다.
            updateData[FirestoreConstants.Users.UPDATED_AT] = FieldValue.serverTimestamp()

            userDocRef.update(updateData).await()
            Unit
        }
    }

    override fun getProjectWrappersStream(userId: String): Flow<CustomResult<List<ProjectsWrapperDTO>, Exception>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(CustomResult.Success(emptyList()))
            awaitClose { }
            return@callbackFlow
        }

        val wrappersCollection = usersCollection.document(userId)
            .collection(FirestoreConstants.Users.ProjectsWrappers.COLLECTION_NAME)

        val listenerRegistration = wrappersCollection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(CustomResult.Failure(error))
                close(error)
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val wrappers = snapshots.toObjects(ProjectsWrapperDTO::class.java)
                trySend(CustomResult.Success(wrappers))
            } else {
                trySend(CustomResult.Failure(Exception("Project wrappers snapshot was null.")))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    override fun getDmWrappersStream(userId: String): Flow<CustomResult<List<DMWrapperDTO>, Exception>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(CustomResult.Success(emptyList()))
            awaitClose { }
            return@callbackFlow
        }

        val wrappersCollection = usersCollection.document(userId)
            .collection(FirestoreConstants.Users.DMWrappers.COLLECTION_NAME)

        val listenerRegistration = wrappersCollection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(CustomResult.Failure(error))
                close(error)
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val wrappers = snapshots.toObjects(DMWrapperDTO::class.java)
                trySend(CustomResult.Success(wrappers))
            } else {
                trySend(CustomResult.Failure(Exception("DM wrappers snapshot was null.")))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

}

