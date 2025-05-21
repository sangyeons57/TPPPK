package com.example.data.repository

import android.net.Uri
import android.util.Log
import com.example.core_common.constants.FirestoreConstants
import com.example.data.datasource.remote.user.UserRemoteDataSource
import com.example.core_common.dispatcher.DispatcherProvider
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.Result

/**
 * UserRepository 인터페이스의 실제 구현체
 * Firestore 'users' 컬렉션 관련 작업을 UserRemoteDataSource 또는 직접 Firestore 호출을 통해 수행합니다.
 */
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val userMapper: UserMapper,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val dispatcherProvider: DispatcherProvider
) : UserRepository {

    private val usersCollection = firestore.collection(FirestoreConstants.Collections.USERS)

    /**
     * 특정 ID를 가진 사용자의 프로필 정보를 실시간 스트림으로 가져옵니다.
     * UserRemoteDataSource를 통해 Firestore의 스냅샷 리스너를 사용하여 변경 사항을 감지하고
     * UserDto를 User 객체로 변환하여 Flow로 전달합니다.
     * @param userId 조회할 사용자의 ID
     * @return Flow<User> 사용자 정보 Flow
     */
    override fun getUserStream(userId: String): Flow<Result<User>> {
        return flow {
            userRemoteDataSource.getUserStream(userId).collect { resultDto ->
                emit(resultDto.map { userDto ->
                    userDto?.let { userMapper.mapToDomain(it) } ?: User.EMPTY
                })
            }
        }
    }

    /**
     * 현재 로그인한 사용자의 프로필 정보를 실시간 스트림으로 가져옵니다.
     * @return Flow<User> 현재 사용자 정보 Flow
     */
    override fun getCurrentUserStream(): Flow<Result<User>> {
        return flow {
            try {
                val currentUserId = getCurrentUserId()
                userRemoteDataSource.getUserStream(currentUserId).collect { resultDto ->
                    emit(resultDto.map { userDto ->
                        userDto?.let { userMapper.mapToDomain(it) } ?: User.EMPTY
                    })
                }
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }
    }

    override suspend fun getCurrentStatus(): Result<UserStatus> {
        val currentUserId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        
        // DataSource를 통해 사용자 상태 가져오기
        return userRemoteDataSource.getUserStatus(currentUserId).map { statusString ->
            try {
                UserStatus.valueOf(statusString.uppercase())
            } catch (e: IllegalArgumentException) {
                UserStatus.OFFLINE
            }
        }
    }

    override suspend fun checkNicknameAvailability(nickname: String): Result<Boolean> {
        Log.d("UserRepositoryImpl", "checkNicknameAvailability called with nickname: $nickname")
        return userRemoteDataSource.checkNicknameAvailability(nickname)
    }

    /**
     * 이름(닉네임)으로 사용자를 검색합니다.
     * @param name 검색할 이름
     * @return 검색 결과에 해당하는 사용자 목록 또는 에러를 포함하는 Result
     */
    override suspend fun searchUsersByName(name: String): Result<List<User>> {
        // UserRemoteDataSource를 통해 데이터 접근하고 UserMapper로 변환
        return try {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                return Result.success(emptyList())
            }
            
            // DataSource를 통해 데이터를 가져오고, 결과를 매핑
            val userDtosResult = userRemoteDataSource.searchUsersByName(trimmedName)
            
            userDtosResult.map { userDtos ->
                userDtos.map { userDto ->
                    userMapper.mapToDomain(userDto)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createUserProfile(user: User): Result<Unit> {
        // User 도메인 모델을 UserDto로 변환 후 DataSource를 통해 저장
        return try {
            val userDto = userMapper.mapToDto(user)
            userRemoteDataSource.createUserProfile(userDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateUserProfile(user: User): Result<Unit> {
        // User 도메인 모델을 UserDto로 변환 후 DataSource를 통해 업데이트
        return try {
            val userDto = userMapper.mapToDto(user)
            userRemoteDataSource.updateUserProfile(userDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeProfileImage(): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return userRemoteDataSource.removeProfileImage(userId)
    }

    override suspend fun updateNickname(newNickname: String): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        // DataSource를 통해 닉네임 업데이트
        return userRemoteDataSource.updateNickname(userId, newNickname)
    }

    override suspend fun updateUserMemo(newMemo: String): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        // DataSource를 통해 메모 업데이트
        return userRemoteDataSource.updateUserMemo(userId, newMemo)
    }

    override suspend fun getUserStatus(userId: String): Result<UserStatus> {
        // DataSource를 통해 사용자 상태 가져오기
        return userRemoteDataSource.getUserStatus(userId).map { statusString ->
            try {
                UserStatus.valueOf(statusString.uppercase())
            } catch (e: IllegalArgumentException) {
                UserStatus.OFFLINE
            }
        }
    }

    override suspend fun updateUserStatus(status: UserStatus): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        // DataSource를 통해 사용자 상태 업데이트
        return userRemoteDataSource.updateUserStatus(userId, status)
    }
    
    override suspend fun updateAccountStatus(accountStatus: AccountStatus): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        // DataSource를 통해 계정 상태 업데이트
        return userRemoteDataSource.updateAccountStatus(userId, accountStatus)
    }
    
    override suspend fun updateFcmToken(token: String): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        // DataSource를 통해 FCM 토큰 업데이트
        return userRemoteDataSource.updateFcmToken(userId, token)
    }
    
    override suspend fun getParticipatingProjects(userId: String): Result<List<String>> {
        return userRemoteDataSource.getParticipatingProjects(userId)
    }
    
    override suspend fun updateParticipatingProjects(projectIds: List<String>): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        // DataSource를 통해 참여 프로젝트 업데이트
        return userRemoteDataSource.updateParticipatingProjects(userId, projectIds)
    }
    
    override suspend fun getActiveDmChannels(userId: String): Result<List<String>> {
        return userRemoteDataSource.getActiveDmChannels(userId)
    }
    
    override suspend fun updateActiveDmChannels(dmIds: List<String>): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
        // DataSource를 통해 활성 DM 채널 업데이트
        return userRemoteDataSource.updateActiveDmChannels(userId, dmIds)
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

    // --- Implementation of new methods from UserRepository interface ---

    override suspend fun getMyProfile(): Result<User> = withContext(dispatcherProvider.io) {
        try {
            userRemoteDataSource.getMyProfile()
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Error in getMyProfile: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getUserProfileImageUrl(userId: String): Result<String?> = withContext(dispatcherProvider.io) {
        try {
            userRemoteDataSource.getUserProfileImageUrl(userId)
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Error in getUserProfileImageUrl: ${e.message}", e)
            Result.failure(Exception("Failed to get profile image URL from repository", e))
        }
    }

    override suspend fun updateUserProfile(name: String, profileImageUrl: String?): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            userRemoteDataSource.updateUserProfile(name, profileImageUrl)
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Error in updateUserProfile: ${e.message}", e)
            Result.failure(Exception("Failed to update profile from repository", e))
        }
    }

    override suspend fun uploadProfileImage(imageUri: Uri): Result<String> = withContext(dispatcherProvider.io) {
        try {
            userRemoteDataSource.uploadProfileImage(imageUri)
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Error in uploadProfileImage: ${e.message}", e)
            Result.failure(Exception("Failed to upload profile image from repository", e))
        }
    }
}