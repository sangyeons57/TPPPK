package com.example.data.datasource.remote.user

import android.net.Uri
import com.example.core_common.constants.FirestoreConstants.Collections
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result

/**
 * UserRemoteDataSource 인터페이스의 Firestore 구현체입니다.
 */
@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRemoteDataSource {

    private val userCollection = firestore.collection(Collections.USERS) // 'users' 컬렉션 참조 예시
    
    // 프로필 이미지 저장소 참조
    private val profileImagesRef = storage.reference.child("profile_images")

    /**
     * Firestore에서 특정 사용자의 프로필 정보를 가져옵니다.
     *
     * @param userId 가져올 사용자의 ID (Firebase Auth UID).
     * @return kotlin.Result 객체. 성공 시 UserDto, 실패 시 Exception 포함.
     */
    override suspend fun getUserProfile(userId: String): Result<UserDto> = runCatching {
        val documentSnapshot = userCollection.document(userId).get().await()
        documentSnapshot.toObject(UserDto::class.java)
            ?: throw NoSuchElementException("User document with id $userId not found or could not be deserialized.")
    }

    override fun getCurrentUserProfile(): Flow<Result<UserDto?>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: run {
            trySend(Result.success(null))
            close() // Flow 정상 종료
            return@callbackFlow
        }
        
        val listenerRegistration = userCollection.document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    close(error) // Flow 에러 종료
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val userDto = snapshot.toObject(UserDto::class.java)
                    trySend(Result.success(userDto))
                } else {
                    trySend(Result.success(null)) // 문서가 없거나 삭제된 경우
                }
            }
        
        awaitClose { listenerRegistration.remove() }
    }.catch { e ->
        emit(Result.failure(e)) // Flow 스트림 자체의 예외 처리
    }

    override suspend fun checkNicknameAvailability(nickname: String): Result<Boolean> = runCatching {
        val querySnapshot = userCollection
            .whereEqualTo("name", nickname) // DTO의 @PropertyName("name")과 일치해야 함
            .limit(1)
            .get()
            .await()
        querySnapshot.isEmpty
    }

    override suspend fun createUserProfile(userDto: UserDto): Result<Unit> = runCatching {
        userCollection.document(userDto.id).set(userDto).await()
        Result.success(Unit) // 명시적 반환
    }

    /**
     * Firestore에서 특정 사용자의 프로필 정보를 업데이트합니다.
     * 문서가 존재하지 않으면 생성하고, 존재하면 제공된 필드만 병합합니다.
     *
     * @param userId 업데이트할 사용자의 ID (Firebase Auth UID).
     * @param userDto 업데이트할 사용자 정보 DTO.
     * @return kotlin.Result 객체. 성공 시 Unit, 실패 시 Exception 포함.
     */
    override suspend fun updateUserProfile(userDto: UserDto): Result<Unit> = runCatching {
        userCollection.document(userDto.id).set(userDto, SetOptions.merge()).await()
        Result.success(Unit) // 명시적 반환 및 TODO 제거
    }

    override suspend fun updateProfileImage(userId: String, imageUri: Uri): Result<String?> = runCatching {
        val userDoc = userCollection.document(userId).get().await()
        val currentUserDto = userDoc.toObject(UserDto::class.java)
        val previousImageUrl = currentUserDto?.profileImageUrl
        
        if (previousImageUrl != null && previousImageUrl.startsWith("gs://")) { // Firebase Storage URL인지 확인
            try {
                val previousImageRef = storage.getReferenceFromUrl(previousImageUrl)
                previousImageRef.delete().await()
            } catch (e: Exception) { /* 이전 이미지 삭제 실패는 무시 */ }
        }
        
        val imageFileName = "${userId}_${UUID.randomUUID()}"
        val imageRef = profileImagesRef.child(imageFileName)
        imageRef.putFile(imageUri).await()
        val downloadUrl = imageRef.downloadUrl.await().toString()
        
        userCollection.document(userId).update("profileImageUrl", downloadUrl).await()
        downloadUrl
    }

    override suspend fun removeProfileImage(userId: String): Result<Unit> = runCatching {
        val userDoc = userCollection.document(userId).get().await()
        val currentUserDto = userDoc.toObject(UserDto::class.java)
        val imageUrl = currentUserDto?.profileImageUrl
        
        if (imageUrl != null && imageUrl.startsWith("gs://")) { // Firebase Storage URL인지 확인
            val imageRef = storage.getReferenceFromUrl(imageUrl)
            imageRef.delete().await()
        }
        
        userCollection.document(userId).update("profileImageUrl", FieldValue.delete()).await() // 필드 삭제
        Result.success(Unit)
    }

    override suspend fun updateNickname(userId: String, newNickname: String): Result<Unit> = runCatching {
        val isAvailable = checkNicknameAvailability(newNickname).getOrThrow()
        if (!isAvailable) {
            throw IllegalArgumentException("이미 사용 중인 닉네임입니다: $newNickname")
        }
        userCollection.document(userId).update("name", newNickname).await() // DTO의 @PropertyName("name")
        Result.success(Unit)
    }

    override suspend fun updateUserMemo(userId: String, newMemo: String): Result<Unit> = runCatching {
        userCollection.document(userId).update("memo", newMemo).await()
        Result.success(Unit)
    }

    override suspend fun getUserStatus(userId: String): Result<String> = runCatching {
        val document = userCollection.document(userId).get().await()
        document.getString("status") ?: UserStatus.OFFLINE.name
    }

    override suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Unit> = runCatching {
        userCollection.document(userId).update("status", status.name).await()
        Result.success(Unit)
    }

    override suspend fun updateAccountStatus(userId: String, accountStatus: AccountStatus): Result<Unit> = runCatching {
        userCollection.document(userId).update("accountStatus", accountStatus.name).await()
        Result.success(Unit)
    }

    override suspend fun updateFcmToken(userId: String, token: String): Result<Unit> = runCatching {
        userCollection.document(userId).update("fcmToken", token).await()
        Result.success(Unit)
    }

    override suspend fun getParticipatingProjects(userId: String): Result<List<String>> = runCatching {
        val document = userCollection.document(userId).get().await()
        document.toObject(UserDto::class.java)?.participatingProjectIds ?: emptyList()
    }

    override suspend fun updateParticipatingProjects(userId: String, projectIds: List<String>): Result<Unit> = runCatching {
        userCollection.document(userId).update("participatingProjectIds", projectIds).await()
        Result.success(Unit)
    }

    override suspend fun getActiveDmChannels(userId: String): Result<List<String>> = runCatching {
        val document = userCollection.document(userId).get().await()
        document.toObject(UserDto::class.java)?.activeDmIds ?: emptyList()
    }

    override suspend fun updateActiveDmChannels(userId: String, dmIds: List<String>): Result<Unit> = runCatching {
        userCollection.document(userId).update("activeDmIds", dmIds).await()
        Result.success(Unit)
    }

    override suspend fun ensureUserProfileExists(firebaseUser: FirebaseUser): Result<UserDto> = runCatching {
        val userId = firebaseUser.uid
        val document = userCollection.document(userId).get().await()
        
        if (document.exists()) {
            document.toObject(UserDto::class.java) ?: throw Exception("사용자 문서 변환 실패")
        } else {
            val userDto = UserDto(
                id = userId,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: "", // 초기 이름, 닉네임 중복 확인은 별도 로직
                isEmailVerified = firebaseUser.isEmailVerified,
                // 나머지 필드는 DTO의 기본값 사용
            )
            userCollection.document(userId).set(userDto).await()
            userDto
        }
    }
} 