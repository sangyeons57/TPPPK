package com.example.data.repository

import android.net.Uri
import com.example.core_common.constants.FirestoreConstants
import com.example.data.datasource.remote.user.UserRemoteDataSource
import com.example.data.model.mapper.UserMapper
import com.example.domain.model.AccountStatus
import com.example.domain.model.User
import com.example.domain.model.UserStatus
import com.example.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * UserRepository 인터페이스의 실제 구현체
 * Firestore 'users' 컬렉션 관련 작업을 UserRemoteDataSource 또는 직접 Firestore 호출을 통해 수행합니다.
 */
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource, // UserRemoteDataSource는 User DTO 관련 작업에 주로 사용
    private val userMapper: UserMapper,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : UserRepository {

    private val usersCollection = firestore.collection(FirestoreConstants.Collections.USERS)

    override suspend fun getUser(userId: String): Result<User> {
        return try {
            val documentSnapshot = usersCollection.document(userId).get().await()
            val user = documentSnapshot.toObject(User::class.java) // KTX 권장 방식으로 변경
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found or parsing failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // getCurrentUser()는 UserRemoteDataSource를 통해 UserDto를 받고 매핑하는 기존 방식 유지 가능
    // 또는 아래 스트림 방식을 단일 조회로 변경하여 사용 가능
    override fun getCurrentUser(): Flow<Result<User?>> {
        return userRemoteDataSource.getCurrentUserProfile().map { resultDto ->
            resultDto.map { userDto ->
                userDto?.let { userMapper.mapToDomain(it) } // UserDto -> User 매핑
            }
        }
    }

    /**
     * 현재 로그인된 사용자의 프로필 정보를 실시간 스트림으로 가져옵니다.
     * Firestore의 스냅샷 리스너를 사용하여 변경 사항을 감지하고 User 객체로 변환하여 Flow로 전달합니다.
     */
    override fun getCurrentUserProfileStream(): Flow<User> = callbackFlow {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            trySend(User.EMPTY) // 또는 에러 처리, User.EMPTY는 예시이며 실제 모델에 맞게 정의 필요
            close(IllegalStateException("User not logged in"))
            return@callbackFlow
        }

        val docRef = usersCollection.document(userId)
        val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java) // KTX 권장 방식으로 변경
                if (user != null) {
                    trySend(user).isSuccess
                } else {
                    trySend(User.EMPTY).isSuccess
                }
            } else {
                trySend(User.EMPTY).isSuccess
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    /**
     * 특정 ID를 가진 사용자의 프로필 정보를 실시간 스트림으로 가져옵니다.
     * Firestore의 스냅샷 리스너를 사용하여 변경 사항을 감지하고 User 객체로 변환하여 Flow로 전달합니다.
     * @param userId 조회할 사용자의 ID
     */
    override fun getUserProfileStream(userId: String): Flow<User> = callbackFlow {
        val docRef = usersCollection.document(userId)
        val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java) // KTX 권장 방식으로 변경
                if (user != null) {
                    trySend(user).isSuccess
                } else {
                    trySend(User.EMPTY).isSuccess
                }
            } else {
                trySend(User.EMPTY).isSuccess
            }
        }
        awaitClose { listenerRegistration.remove() }
    }


    override suspend fun getCurrentStatus(): Result<UserStatus> {
        val currentUserId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return try {
            val document = usersCollection.document(currentUserId).get().await()
            val statusString = document.getString(FirestoreConstants.UserFields.STATUS) ?: UserStatus.OFFLINE.name
            
            Result.success(
                try {
                    UserStatus.valueOf(statusString.uppercase())
                } catch (e: IllegalArgumentException) {
                    UserStatus.OFFLINE
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkNicknameAvailability(nickname: String): Result<Boolean> {
        return userRemoteDataSource.checkNicknameAvailability(nickname)
    }

    override suspend fun createUserProfile(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfileImage(imageUri: Uri): Result<String?> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return userRemoteDataSource.updateProfileImage(userId, imageUri)
    }

    override suspend fun removeProfileImage(): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return userRemoteDataSource.removeProfileImage(userId)
    }

    override suspend fun updateNickname(newNickname: String): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        return try {
            usersCollection.document(userId).update(FirestoreConstants.UserFields.NAME, newNickname).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserMemo(newMemo: String): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        return try {
            usersCollection.document(userId).update(FirestoreConstants.UserFields.STATUS_MESSAGE, newMemo).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserStatus(userId: String): Result<UserStatus> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val statusString = document.getString(FirestoreConstants.UserFields.STATUS) ?: UserStatus.OFFLINE.name
            Result.success(UserStatus.valueOf(statusString.uppercase()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserStatus(status: UserStatus): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        return try {
            usersCollection.document(userId).update(FirestoreConstants.UserFields.STATUS, status.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateAccountStatus(accountStatus: AccountStatus): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        return try {
            usersCollection.document(userId).update(FirestoreConstants.UserFields.ACCOUNT_STATUS, accountStatus.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateFcmToken(token: String): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        return try {
            usersCollection.document(userId).update(FirestoreConstants.UserFields.FCM_TOKEN, token).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getParticipatingProjects(userId: String): Result<List<String>> {
        return userRemoteDataSource.getParticipatingProjects(userId)
    }
    
    override suspend fun updateParticipatingProjects(projectIds: List<String>): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        return try {
            usersCollection.document(userId).update(FirestoreConstants.UserFields.PARTICIPATING_PROJECT_IDS, projectIds).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getActiveDmChannels(userId: String): Result<List<String>> {
        return userRemoteDataSource.getActiveDmChannels(userId)
    }
    
    override suspend fun updateActiveDmChannels(dmIds: List<String>): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        return try {
            usersCollection.document(userId).update(FirestoreConstants.UserFields.ACTIVE_DM_IDS, dmIds).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun ensureUserProfileExists(firebaseUser: FirebaseUser): Result<User> {
        return userRemoteDataSource.ensureUserProfileExists(firebaseUser).map { userDto ->
            userMapper.mapToDomain(userDto)
        }
    }

    override suspend fun getCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated or UID not available")
    }
}