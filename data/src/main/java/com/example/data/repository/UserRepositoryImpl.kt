package com.example.data.repository

import android.net.Uri
import com.example.domain.model.User
import com.example.domain.model.UserStatus
import com.example.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.Result
import com.example.data.util.FirestoreConstants as FC
import androidx.core.net.toUri
import com.google.firebase.auth.UserProfileChangeRequest

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : UserRepository {

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    /** 사용자 프로필 정보 가져오기 (User 모델 반환) */
    override suspend fun getUser(): Result<User> { // ★ 반환 타입 User로 변경
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        val userDocRef = firestore.collection(FC.Collections.USERS).document(userId)

        return try {
            val snapshot = userDocRef.get().await()
            if (snapshot.exists()) {
                // Firestore 문서 -> User Domain 모델 변환
                val user = snapshot.toObject(User::class.java)
                    ?: return Result.failure(IllegalStateException("Failed to parse user data"))

                // ★ User 객체를 직접 성공 결과로 반환
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("User profile document not found in Firestore."))
            }
        } catch (e: Exception) {
            println("Error getting user profile: ${e.message}")
            Result.failure(e)
        }
    }

    // --- updateProfileImage, removeProfileImage, updateUserName ---
    // 이 함수들은 Firestore의 User 필드를 직접 업데이트하므로 큰 변경 필요 없음
    // (상수 이름 확인 등)

    override suspend fun updateProfileImage(imageUri: Uri): Result<String?> {
        // ... (이전과 유사한 구현, FC.Users.Fields.PROFILE_IMAGE_URL 사용) ...
        // Firestore 업데이트
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        val userDocRef = firestore.collection(FC.Collections.USERS).document(userId)
        val fileExtension = getFileExtension(imageUri)
        val storageRef = storage.reference.child("${FC.StoragePaths.PROFILE_IMAGES}/$userId${fileExtension}")

        return try {
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            userDocRef.update(FC.Users.Fields.PROFILE_IMAGE_URL, downloadUrl).await() // User 필드 업데이트
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setPhotoUri(downloadUrl.toUri())
                .build()
            auth.currentUser?.updateProfile(profileUpdates)?.await()
            Result.success(downloadUrl)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun getFileExtension(uri: Uri): String {
        // TODO: ContentResolver 등을 사용하여 실제 파일 확장자 확인 로직 구현
        return ".jpg"
    }

    override suspend fun removeProfileImage(): Result<Unit> {
        // ... (이전과 유사한 구현, FC.Users.Fields.PROFILE_IMAGE_URL 사용) ...
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        val userDocRef = firestore.collection(FC.Collections.USERS).document(userId)
        return try {
            val currentImageUrl = userDocRef.get().await().getString(FC.Users.Fields.PROFILE_IMAGE_URL)
            if (currentImageUrl != null) {
                try {
                    Firebase.storage.getReferenceFromUrl(currentImageUrl).delete().await()
                } catch (storageError: Exception) {
                    println("Warning: Failed to remove image from Storage: ${storageError.message}")
                }
            }
            userDocRef.update(FC.Users.Fields.PROFILE_IMAGE_URL, null).await() // User 필드 업데이트
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setPhotoUri(null)
                .build()
            auth.currentUser?.updateProfile(profileUpdates)?.await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateUserName(newName: String): Result<Unit> {
        // ... (이전과 유사한 구현, FC.Users.Fields.NAME 사용) ...
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        val userDocRef = firestore.collection(FC.Collections.USERS).document(userId)
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) { return Result.failure(IllegalArgumentException("Username cannot be blank.")) }
        return try {
            userDocRef.update(FC.Users.Fields.NAME, trimmedName).await() // User 필드 업데이트
            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(trimmedName)
                                .build()
            auth.currentUser?.updateProfile(profileUpdates)?.await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- updateStatusMessage ---
    // 이 메서드는 User 모델에 statusMessage 필드가 없으므로,
    // User 모델에 해당 필드를 추가하거나, 이 메서드의 역할을 재정의하거나 삭제해야 합니다.
    // 만약 User 모델의 status 필드를 이 메시지로 사용한다면 로직 변경 필요.
    // 여기서는 일단 User 모델에 statusMessage가 없다고 가정하고 주석 처리 또는 에러 반환.
    override suspend fun updateStatusMessage(newStatus: String): Result<Unit> {
        // return Result.failure(NotImplementedError("User model does not have statusMessage field."))
        // 또는 임시 구현 유지 (단, Firestore 상수 확인 필요)
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        val userDocRef = firestore.collection(FC.Collections.USERS).document(userId)
        val statusToSave = newStatus.trim().takeIf { it.isNotEmpty() }
        return try {
            // Firestore에 statusMessage 필드가 있다고 가정하고 업데이트 (상수 FC.Users.Fields.STATUS_MESSAGE 필요)
            userDocRef.set(mapOf(FC.Users.Fields.STATUS_MESSAGE to statusToSave), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- getCurrentStatus, updateUserStatus ---
    // 이 함수들은 User 모델의 status 필드를 사용하므로 변경 필요 없음
    // (상수 이름 확인 등)

    override suspend fun getCurrentStatus(): Result<UserStatus> {
        // ... (이전과 동일한 구현, FC.Users.Fields.STATUS 사용) ...
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        val userDocRef = firestore.collection(FC.Collections.USERS).document(userId)
        return try {
            val snapshot = userDocRef.get().await()
            if (snapshot.exists()) {
                val statusString = snapshot.getString(FC.Users.Fields.STATUS) // User 필드 사용
                val userStatus = UserStatus.entries.find { it.name.equals(statusString, ignoreCase = true) } ?: UserStatus.OFFLINE
                Result.success(userStatus)
            } else { Result.failure(IllegalStateException("User profile document not found.")) }
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateUserStatus(status: UserStatus): Result<Unit> {
        // ... (이전과 동일한 구현, FC.Users.Fields.STATUS 사용) ...
        val userId = getCurrentUserId() ?: return Result.failure(IllegalStateException("User not logged in"))
        val userDocRef = firestore.collection(FC.Collections.USERS).document(userId)
        val statusString = status.name
        return try {
            userDocRef.update(FC.Users.Fields.STATUS, statusString).await() // User 필드 업데이트
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- ensureUserProfileExists ---
    // 이 함수는 이미 User 모델을 반환하므로 변경 필요 없음
    override suspend fun ensureUserProfileExists(firebaseUser: FirebaseUser): Result<User> {
        // ... (이전과 동일한 구현) ...
        val userDocRef = firestore.collection(FC.Collections.USERS).document(firebaseUser.uid)
        return try {
            val snapshot = userDocRef.get().await()
            if (snapshot.exists()) {
                val existingUser = snapshot.toObject(User::class.java) ?: throw IllegalStateException("User document exists but cannot be parsed.")
                Result.success(existingUser)
            } else {
                val newUser = User(
                    userId = firebaseUser.uid,
                    name = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: firebaseUser.email?.substringBefore('@')?.takeIf { it.isNotBlank() } ?: "사용자",
                    email = firebaseUser.email ?: "",
                    profileImageUrl = firebaseUser.photoUrl?.toString(),
                    status = null
                )
                userDocRef.set(newUser).await()
                Result.success(newUser)
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}