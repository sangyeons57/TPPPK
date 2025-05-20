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
        getUserStream(currentUserId)
    }

    override suspend fun checkNicknameAvailability(nickname: String): Result<Boolean> = runCatching {
        val querySnapshot = userCollection
            .whereEqualTo(UserFields.NAME, nickname) // DTO의 @PropertyName("name")과 일치해야 함
            .limit(1)
            .get()
            .await()
        Log.d("checkNicknameAvailability", "Query result: $querySnapshot ${querySnapshot.isEmpty}")
        return Result.success(querySnapshot.isEmpty)
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
        
        userCollection.document(userId).update(mapOf(
            "profileImageUrl" to downloadUrl,
            "updatedAt" to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
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
        
        userCollection.document(userId).update(mapOf(
            "profileImageUrl" to FieldValue.delete(),
            "updatedAt" to DateTimeUtil.nowFirebaseTimestamp()
        )).await() // 필드 삭제
        Result.success(Unit)
    }

    override suspend fun updateNickname(userId: String, newNickname: String): Result<Unit> = runCatching {
        val isAvailable = checkNicknameAvailability(newNickname).getOrThrow()
        if (!isAvailable) {
            throw IllegalArgumentException("이미 사용 중인 닉네임입니다: $newNickname")
        }
        userCollection.document(userId).update(mapOf(
            "name" to newNickname, // DTO의 @PropertyName("name")
            "updatedAt" to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
        Result.success(Unit)
    }

    override suspend fun updateUserMemo(userId: String, newMemo: String): Result<Unit> = runCatching {
        userCollection.document(userId).update(mapOf(
            "memo" to newMemo,
            "updatedAt" to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
        Result.success(Unit)
    }

    override suspend fun getUserStatus(userId: String): Result<String> = runCatching {
        val document = userCollection.document(userId).get().await()
        document.getString("status") ?: UserStatus.OFFLINE.name
    }

    /**
     * 이름(닉네임)으로 사용자를 검색합니다.
     * 
     * @param name 검색할 이름
     * @return 성공 시 UserDto 목록이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    override suspend fun searchUsersByName(name: String): Result<List<UserDto>> = runCatching {
        val trimmedName = name.trim()
        
        // 이름이 검색어를 포함하는 사용자 문서 조회
        val querySnapshot = userCollection
            .whereGreaterThanOrEqualTo(UserFields.NAME, trimmedName)
            .whereLessThan(UserFields.NAME, trimmedName + "\uf8ff") // 접두사 검색을 위한 기법
            .limit(10) // 결과 수 제한
            .get()
            .await()
            
        querySnapshot.documents.mapNotNull { document ->
            val userDto = document.toObject(UserDto::class.java)
            // document.id가 UserDto 모델의 id 필드에 자동 매핑이 안될 수 있으므로
            // 수동으로 설정 (필요한 경우)
            if (userDto != null && userDto.id.isEmpty()) {
                userDto.copy(id = document.id)
            } else {
                userDto
            }
        }
    }

    override suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Unit> = runCatching {
        userCollection.document(userId).update(mapOf(
            "status" to status.name,
            "updatedAt" to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
        Result.success(Unit)
    }

    override suspend fun updateAccountStatus(userId: String, accountStatus: AccountStatus): Result<Unit> = runCatching {
        userCollection.document(userId).update(mapOf(
            "accountStatus" to accountStatus.name,
            "updatedAt" to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
        Result.success(Unit)
    }

    override suspend fun updateFcmToken(userId: String, token: String): Result<Unit> = runCatching {
        userCollection.document(userId).update(mapOf(
            "fcmToken" to token,
            "updatedAt" to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
        Result.success(Unit)
    }

    override suspend fun getParticipatingProjects(userId: String): Result<List<String>> = runCatching {
        val document = userCollection.document(userId).get().await()
        document.toObject(UserDto::class.java)?.participatingProjectIds ?: emptyList()
    }

    override suspend fun updateParticipatingProjects(userId: String, projectIds: List<String>): Result<Unit> = runCatching {
        userCollection.document(userId).update(mapOf(
            "participatingProjectIds" to projectIds,
            "updatedAt" to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
        Result.success(Unit)
    }

    override suspend fun getActiveDmChannels(userId: String): Result<List<String>> = runCatching {
        val document = userCollection.document(userId).get().await()
        document.toObject(UserDto::class.java)?.activeDmIds ?: emptyList()
    }

    override suspend fun updateActiveDmChannels(userId: String, dmIds: List<String>): Result<Unit> = runCatching {
        userCollection.document(userId).update(mapOf(
            "activeDmIds" to dmIds,
            "updatedAt" to DateTimeUtil.nowFirebaseTimestamp()
        )).await()
        Result.success(Unit)
    }

    override suspend fun ensureUserProfileExists(firebaseUser: FirebaseUser): Result<UserDto> = runCatching {
        val userId = firebaseUser.uid
        val document = userCollection.document(userId).get().await()
        
        if (document.exists()) {
            document.toObject(UserDto::class.java)
                ?: throw NoSuchElementException("User document $userId exists but could not be deserialized.")
        } else {
            // 사용자 프로필이 존재하지 않으면 새로 생성합니다.
            val now = DateTimeUtil.nowFirebaseTimestamp() // 일관성을 위해 한 번 호출
            val newUserDto = UserDto(
                id = userId,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@') ?: "User_${userId.take(6)}",
                profileImageUrl = firebaseUser.photoUrl?.toString(),
                createdAt = now, 
                updatedAt = now, // updatedAt 추가
                status = UserStatus.OFFLINE,
                accountStatus = AccountStatus.ACTIVE,
                isEmailVerified = firebaseUser.isEmailVerified
                // participatingProjectIds and activeDmIds default to emptyList in UserDto
            )
            createUserProfile(newUserDto).getOrThrow() // 이 호출은 UserMapper를 통해 UserDto를 사용합니다.
            newUserDto
        }
    }
} 