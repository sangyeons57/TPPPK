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
import com.google.firebase.firestore.Source
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

    /**
     * Searches for a single user by their exact name and returns a Flow that emits the UserDTO.
     * The Flow will emit updates if the user's data changes in Firestore.
     * Emits [com.example.data.util.CustomResult.Success] with [com.example.data.dto.UserDTO] if a user is found.
     * Emits [com.example.data.util.CustomResult.Error] with [java.util.NoSuchElementException] if no user is found.
     * Emits [com.example.data.util.CustomResult.Error] with the underlying exception if a Firestore error occurs.
     *
     * @param nameQuery The exact name of the user to search for.
     * @return A Flow emitting the search result.
     */
    override suspend fun searchUserByName(nameQuery: String): Flow<CustomResult<UserDTO, Exception>> = callbackFlow {
        // Ensure usersCollection is available in this class scope (e.g., private val usersCollection = firestore.collection("users"))
        val query = usersCollection
            .whereEqualTo(FirestoreConstants.Users.NAME, nameQuery)
            .limit(1)

        val listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(CustomResult.Failure(error))
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                val document = snapshots.documents[0] // Get the first document due to limit(1)
                try {
                    val userDTO = document.toObject(UserDTO::class.java)
                    if (userDTO != null) {
                        // If UserDTO has @DocumentId annotation on a field for uid,
                        // toObject() might already populate it.
                        // If not, and UserDTO has a 'uid' field, explicitly set it.
                        // Assuming UserDTO is a data class or has a copy method and a 'uid' property.
                        val finalUserDTO = if (userDTO.uid.isNullOrEmpty() && document.id.isNotEmpty()) userDTO.copy(uid = document.id) else userDTO
                        trySend(CustomResult.Success(finalUserDTO))
                    } else {
                        // This case (userDTO is null after toObject on an existing document) is less common
                        // but good to handle. It means parsing failed for some reason.
                        trySend(CustomResult.Failure(Exception("Failed to parse user data for document: ${document.id}")))
                    }
                } catch (parseException: Exception) {
                    // This catches errors during document.toObject()
                    trySend(CustomResult.Failure(parseException))
                }
            } else {
                // Snapshots is null or empty, meaning no user found with that exact name.
                trySend(CustomResult.Failure(NoSuchElementException("User not found with name: $nameQuery")))
            }
        }
        // This is crucial: unregister the listener when the Flow is no longer collected.
        awaitClose {
            listenerRegistration.remove()
        }
    }
    override suspend fun searchUsersByName(nameQuery: String, maxResults: Long): Flow<CustomResult<List<UserDTO>, Exception>> = callbackFlow  {
        resultTry {
            // Firestore에서 효율적인 검색을 구현하려면 Algolia 같은 외부 검색 서비스를 연동하는 것이 좋습니다.
            // "name" 필드에 대한 색인이 필요합니다.
            val querySnapshot = usersCollection
                .orderBy("name")
                .startAt(nameQuery)
                .endAt(nameQuery + "\uf8ff")
                .limit(maxResults)
                .get()
                .await()
            
            querySnapshot.map{snapshot -> snapshot.toObject(UserDTO::class.java)}
        }
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

    /**
     * Updates specific fields for a user in Firestore.
     *
     * @param uid The ID of the user to update.
     * @param updates A map of field names to their new values. `FieldValue.serverTimestamp()` can be used for timestamps.
     * @return CustomResult indicating success or failure.
     */
    override suspend fun updateUserFields(uid: String, updates: Map<String, Any?>): CustomResult<Unit, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            if (uid.isEmpty()) {
                throw IllegalArgumentException("User ID cannot be empty.")
            }
            usersCollection.document(uid).update(updates).await()
            Unit // Indicate success
        }
    }

    /**
     * 이메일로 단일 사용자 조회 (단발성)
     */
    override suspend fun getUserByEmail(email: String): CustomResult<UserDTO, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val querySnapshot = usersCollection
                .whereEqualTo(FirestoreConstants.Users.EMAIL, email)
                .limit(1)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents.first()
                val dto = doc.toObject(UserDTO::class.java)
                // dto.uid may be null or empty; use document id in that case to avoid NPE
                dto?.let { nonNullDto ->
                    val finalUid = if (nonNullDto.uid.isNullOrEmpty()) doc.id else nonNullDto.uid
                    nonNullDto.copy(uid = finalUid)
                } ?: throw Exception("Failed to parse user data")
            } else {
                throw NoSuchElementException("User not found with email: $email")
            }
        }
    }

    override fun getUserByExactNameStream(name: String): Flow<CustomResult<UserDTO, Exception>> = callbackFlow {
        trySend(CustomResult.Loading) // Send initial loading state

        val query = usersCollection
            .whereEqualTo(FirestoreConstants.Users.NAME, name)
            .limit(1)

        val listenerRegistration = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                trySend(CustomResult.Failure(error))
                close(error) // Close the flow on error
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                // Even with limit(1), snapshots.documents is a list. Get the first.
                val document = snapshots.documents[0]
                val userDto = document.toObject(UserDTO::class.java)
                if (userDto != null) {
                    trySend(CustomResult.Success(userDto))
                } else {
                    // Document exists but couldn't be parsed
                    trySend(CustomResult.Failure(Exception("Failed to parse user data for name: $name")))
                }
            } else {
                // No document found with that exact name
                trySend(CustomResult.Failure(Exception("User not found with name: $name")))
            }
        }
        awaitClose { listenerRegistration.remove() }
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

    override suspend fun fetchUserByIdServer(userId: String): CustomResult<UserDTO, Exception> = withContext(Dispatchers.IO) {
        resultTry {
            val document = usersCollection.document(userId).get(Source.SERVER).await()
            document.toObject(UserDTO::class.java) ?: throw Exception("User data could not be parsed.")
        }
    }

}
