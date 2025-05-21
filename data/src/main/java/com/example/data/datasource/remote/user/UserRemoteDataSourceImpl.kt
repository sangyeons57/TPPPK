package com.example.data.datasource.remote.user

import android.net.Uri
import android.util.Log
import com.example.core_common.constants.FirestoreConstants.Collections
import com.example.core_common.constants.FirestoreConstants.UserFields
import com.example.data.model.remote.user.UserDto
import com.example.domain.model.AccountStatus
import com.example.domain.model.UserStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import java.util.NoSuchElementException
import java.util.UUID
import com.example.core_common.dispatcher.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result
import com.example.core_common.util.DateTimeUtil

/**
 * UserRemoteDataSource 인터페이스의 Firestore 구현체입니다.
 */
@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val dispatcherProvider: DispatcherProvider
) : UserRemoteDataSource {

    private val userCollection = firestore.collection(Collections.USERS)
    private val profileImagesRef = storage.reference.child("profile_images")

    override suspend fun getUserStream(userId: String): Flow<Result<UserDto?>> = callbackFlow {
        val listenerRegistration = userCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val userDto = snapshot.toObject(UserDto::class.java)
                    trySend(Result.success(userDto))
                } else {
                    trySend(Result.success(null))
                }
            }
        awaitClose { listenerRegistration.remove() }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override suspend fun getCurrentUserStream(): Flow<Result<UserDto?>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: run {
            trySend(Result.success(null))
            close()
            return@callbackFlow
        }
        val listenerRegistration = userCollection.document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val userDto = snapshot.toObject(UserDto::class.java)
                    trySend(Result.success(userDto))
                } else {
                    trySend(Result.success(null))
                }
            }
        awaitClose { listenerRegistration.remove() }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override suspend fun checkNicknameAvailability(nickname: String): Result<Boolean> = runCatching {
        val querySnapshot = userCollection
            .whereEqualTo(UserFields.NAME, nickname)
            .limit(1)
            .get()
            .await()
        querySnapshot.isEmpty
    }.onFailure { Log.d("checkNicknameAvailability", "Query failed: ${it.message}") }

    override suspend fun createUserProfile(userDto: UserDto): Result<Unit> = runCatching {
        userCollection.document(userDto.id).set(userDto).await()
    }

    override suspend fun updateUserProfile(userDto: UserDto): Result<Unit> = runCatching {
        userCollection.document(userDto.id).set(userDto, SetOptions.merge()).await()
    }

    override suspend fun removeProfileImage(userId: String): Result<Unit> = runCatching {
        val userDoc = userCollection.document(userId).get().await()
        val currentUserDto = userDoc.toObject(UserDto::class.java)
        val imageUrl = currentUserDto?.profileImageUrl
        
        if (imageUrl != null && imageUrl.startsWith("gs://")) {
            val imageRef = storage.getReferenceFromUrl(imageUrl)
            imageRef.delete().await()
        }
        
        userCollection.document(userId).update(mapOf(
            UserFields.PROFILE_IMAGE_URL to FieldValue.delete(),
            UserFields.UPDATED_AT to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
    }

    override suspend fun updateNickname(userId: String, newNickname: String): Result<Unit> = runCatching {
        val isAvailable = checkNicknameAvailability(newNickname).getOrThrow()
        if (!isAvailable) {
            throw IllegalArgumentException("이미 사용 중인 닉네임입니다: $newNickname")
        }
        userCollection.document(userId).update(mapOf(
            UserFields.NAME to newNickname,
            UserFields.UPDATED_AT to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
    }

    override suspend fun updateUserMemo(userId: String, newMemo: String): Result<Unit> = runCatching {
        userCollection.document(userId).update(mapOf(
            UserFields.MEMO to newMemo,
            UserFields.UPDATED_AT to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
    }

    override suspend fun getUserStatus(userId: String): Result<String> = runCatching {
        val document = userCollection.document(userId).get().await()
        document.getString(UserFields.STATUS) ?: UserStatus.OFFLINE.name
    }

    override suspend fun searchUsersByName(name: String): Result<List<UserDto>> = runCatching {
        val trimmedName = name.trim()
        val querySnapshot = userCollection
            .whereGreaterThanOrEqualTo(UserFields.NAME, trimmedName)
            .whereLessThan(UserFields.NAME, trimmedName + "\uf8ff")
            .limit(10)
            .get()
            .await()
            
        querySnapshot.documents.mapNotNull { document ->
            document.toObject(UserDto::class.java)?.copy(id = document.id)
        }
    }

    override suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Unit> = runCatching {
        userCollection.document(userId).update(mapOf(
            UserFields.STATUS to status.name,
            UserFields.UPDATED_AT to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
    }

    override suspend fun updateAccountStatus(userId: String, accountStatus: AccountStatus): Result<Unit> = runCatching {
        userCollection.document(userId).update(mapOf(
            UserFields.ACCOUNT_STATUS to accountStatus.name,
            UserFields.UPDATED_AT to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
    }

    override suspend fun updateFcmToken(userId: String, token: String): Result<Unit> = runCatching {
        userCollection.document(userId).update(mapOf(
            UserFields.FCM_TOKEN to token,
            UserFields.UPDATED_AT to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
    }

    override suspend fun getParticipatingProjects(userId: String): Result<List<String>> = runCatching {
        val document = userCollection.document(userId).get().await()
        document.toObject(UserDto::class.java)?.participatingProjectIds ?: emptyList()
    }

    override suspend fun updateParticipatingProjects(userId: String, projectIds: List<String>): Result<Unit> = runCatching {
        userCollection.document(userId).update(mapOf(
            UserFields.PARTICIPATING_PROJECT_IDS to projectIds,
            UserFields.UPDATED_AT to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
    }

    override suspend fun getActiveDmChannels(userId: String): Result<List<String>> = runCatching {
        val document = userCollection.document(userId).get().await()
        document.toObject(UserDto::class.java)?.activeDmIds ?: emptyList()
    }

    override suspend fun updateActiveDmChannels(userId: String, dmIds: List<String>): Result<Unit> = runCatching {
        userCollection.document(userId).update(mapOf(
            UserFields.ACTIVE_DM_IDS to dmIds,
            UserFields.UPDATED_AT to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
    }

    override suspend fun ensureUserProfileExists(firebaseUser: FirebaseUser): Result<UserDto> = runCatching {
        val userId = firebaseUser.uid
        val document = userCollection.document(userId).get().await()
        
        if (document.exists()) {
            document.toObject(UserDto::class.java)
                ?: throw NoSuchElementException("User document $userId exists but could not be deserialized.")
        } else {
            val now = DateTimeUtil.nowFirebaseTimestamp()
            val newUserDto = UserDto(
                id = userId,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@') ?: "User_${userId.take(6)}",
                profileImageUrl = firebaseUser.photoUrl?.toString(),
                createdAt = now, 
                updatedAt = now,
                status = UserStatus.OFFLINE,
                accountStatus = AccountStatus.ACTIVE,
                isEmailVerified = firebaseUser.isEmailVerified
            )
            createUserProfile(newUserDto).getOrThrow()
            newUserDto
        }
    }

    // --- Implementation of new methods (이제 kotlin.Result 사용) ---

    override suspend fun getMyProfile(): Result<User> = withContext(dispatcherProvider.io) {
        runCatching {
            val firebaseUser = auth.currentUser ?: throw IllegalStateException("User not logged in")
            val userId = firebaseUser.uid
            val firestoreDoc = userCollection.document(userId).get().await()
            
            val defaultUser = com.example.domain.model.User.EMPTY 

            com.example.domain.model.User(
                id = userId,
                email = firebaseUser.email ?: defaultUser.email,
                name = firestoreDoc.getString(UserFields.NAME) ?: firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@') ?: defaultUser.name,
                profileImageUrl = firestoreDoc.getString(UserFields.PROFILE_IMAGE_URL) ?: firebaseUser.photoUrl?.toString(),
                memo = firestoreDoc.getString(UserFields.MEMO) ?: defaultUser.memo,
                statusMessage = firestoreDoc.getString(UserFields.STATUS_MESSAGE) ?: defaultUser.statusMessage,
                status = firestoreDoc.getString(UserFields.STATUS)?.let {
                    try { com.example.domain.model.UserStatus.valueOf(it.uppercase()) } catch (e: IllegalArgumentException) { defaultUser.status }
                } ?: defaultUser.status,
                createdAt = firestoreDoc.getTimestamp(UserFields.CREATED_AT)?.toDate()?.toInstant() ?: defaultUser.createdAt,
                fcmToken = firestoreDoc.getString(UserFields.FCM_TOKEN) ?: defaultUser.fcmToken,
                participatingProjectIds = (firestoreDoc.get(UserFields.PARTICIPATING_PROJECT_IDS) as? List<String>) ?: defaultUser.participatingProjectIds,
                accountStatus = firestoreDoc.getString(UserFields.ACCOUNT_STATUS)?.let {
                    try { com.example.domain.model.AccountStatus.valueOf(it.uppercase()) } catch (e: IllegalArgumentException) { defaultUser.accountStatus }
                } ?: defaultUser.accountStatus,
                activeDmIds = (firestoreDoc.get(UserFields.ACTIVE_DM_IDS) as? List<String>) ?: defaultUser.activeDmIds,
                isEmailVerified = firebaseUser.isEmailVerified,
                updatedAt = firestoreDoc.getTimestamp(UserFields.UPDATED_AT)?.toDate()?.toInstant(),
                consentTimeStamp = firestoreDoc.getTimestamp(UserFields.CONSENT_TIMESTAMP)?.toDate()?.toInstant()
            )
        }.onFailure { Log.e("UserRemoteDataSource", "Error in getMyProfile: ${it.message}", it) }
    }

    override suspend fun getUserProfileImageUrl(userId: String): Result<String?> = withContext(dispatcherProvider.io) {
        runCatching {
            val document = userCollection.document(userId).get().await()
            if (document.exists()) {
                document.getString(UserFields.PROFILE_IMAGE_URL)
            } else {
                throw NoSuchElementException("User document not found for userId: $userId")
            }
        }.onFailure { Log.e("UserRemoteDataSource", "Error in getUserProfileImageUrl: ${it.message}", it) }
    }

    override suspend fun updateUserProfile(name: String, profileImageUrl: String?): Result<Unit> = withContext(dispatcherProvider.io) {
        runCatching {
            val firebaseUser = auth.currentUser ?: throw IllegalStateException("User not logged in")
            val userId = firebaseUser.uid

            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
            profileImageUrl?.let { profileUpdates.setPhotoUri(Uri.parse(it)) }
            firebaseUser.updateProfile(profileUpdates.build()).await()

            val userDocRef = userCollection.document(userId)
            val updates = mutableMapOf<String, Any?>(
                UserFields.NAME to name,
                UserFields.UPDATED_AT to FieldValue.serverTimestamp()
            )
            if (profileImageUrl != null) {
                updates[UserFields.PROFILE_IMAGE_URL] = profileImageUrl
            } else {
                 updates[UserFields.PROFILE_IMAGE_URL] = FieldValue.delete()
            }
            userDocRef.set(updates, SetOptions.merge()).await()
        }.onFailure { Log.e("UserRemoteDataSource", "Error in updateUserProfile: ${it.message}", it) }
    }

    override suspend fun uploadProfileImage(imageUri: Uri): Result<String> = withContext(dispatcherProvider.io) {
        runCatching {
            val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
            val imageFileName = "${userId}_${UUID.randomUUID()}"
            val imageRef = profileImagesRef.child(imageFileName)

            imageRef.putFile(imageUri).await()
            imageRef.downloadUrl.await().toString()
        }.onFailure { Log.e("UserRemoteDataSource", "Error in uploadProfileImage: ${it.message}", it) }
    }
}