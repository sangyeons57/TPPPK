package com.example.data.repository

import android.net.Uri
import com.example.data.datasource.remote.user.UserRemoteDataSource
import com.example.data.model.mapper.UserMapper
import com.example.domain.model.AccountStatus
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * UserRepository 인터페이스의 실제 구현체
 * Firestore 'users' 컬렉션 관련 작업을 UserRemoteDataSource를 통해 수행합니다.
 */
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val userMapper: UserMapper
) : UserRepository {

    private fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    override suspend fun getUserProfile(userId: String): Result<User> {
        return userRemoteDataSource.getUserProfile(userId).map { userDto ->
            userMapper.mapToDomain(userDto)
        }
    }

    override fun getCurrentUserProfile(): Flow<Result<User?>> {
        return userRemoteDataSource.getCurrentUserProfile().map { result ->
            result.map { userDto ->
                userDto?.let { userMapper.mapToDomain(it) }
            }
        }
    }

    override fun getCurrentStatus(): Result<UserStatus> {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return try {
            // 저장된 상태가 없거나 오류 발생 시 기본값 OFFLINE 반환
            val currentUser = FirebaseAuth.getInstance().currentUser
            
            if (currentUser != null) {
                // Firestore에서 사용자 상태 필드 가져오기
                val db = FirebaseFirestore.getInstance()
                val userDocRef = db.collection(FC.Collections.USERS).document(currentUserId)
                
                // 동기식으로 처리 (Result로 래핑)
                val document = userDocRef.get().result
                val statusString = document?.getString("status") ?: UserStatus.OFFLINE.name
                
                Result.success(
                    try {
                        UserStatus.valueOf(statusString)
                    } catch (e: IllegalArgumentException) {
                        UserStatus.OFFLINE
                    }
                )
            } else {
                Result.success(UserStatus.OFFLINE)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkNicknameAvailability(nickname: String): Result<Boolean> {
        return userRemoteDataSource.checkNicknameAvailability(nickname)
    }

    override suspend fun createUserProfile(user: User): Result<Unit> {
        val userDto = userMapper.mapToDto(user)
        return userRemoteDataSource.createUserProfile(userDto)
    }

    override suspend fun updateUserProfile(user: User): Result<Unit> {
        val userDto = userMapper.mapToDto(user)
        return userRemoteDataSource.updateUserProfile(userDto)
    }

    override suspend fun updateProfileImage(imageUri: Uri): Result<String?> {
        // 현재 로그인된 사용자에게만 적용
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return userRemoteDataSource.updateProfileImage(userId, imageUri)
    }

    override suspend fun removeProfileImage(): Result<Unit> {
        // 현재 로그인된 사용자에게만 적용
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return userRemoteDataSource.removeProfileImage(userId)
    }

    override suspend fun updateNickname(newNickname: String): Result<Unit> {
        // 현재 로그인된 사용자에게만 적용
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return userRemoteDataSource.updateNickname(userId, newNickname)
    }

    override suspend fun updateUserMemo(newMemo: String): Result<Unit> {
        // 현재 로그인된 사용자에게만 적용
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return userRemoteDataSource.updateUserMemo(userId, newMemo)
    }

    override suspend fun getUserStatus(userId: String): Result<UserStatus> {
        return userRemoteDataSource.getUserStatus(userId).map { statusString ->
            try {
                UserStatus.valueOf(statusString)
            } catch (e: IllegalArgumentException) {
                UserStatus.OFFLINE
            }
        }
    }

    override suspend fun updateUserStatus(status: UserStatus): Result<Unit> {
        // 현재 로그인된 사용자에게만 적용
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return userRemoteDataSource.updateUserStatus(userId, status)
    }

    override suspend fun updateAccountStatus(accountStatus: AccountStatus): Result<Unit> {
        // 현재 로그인된 사용자에게만 적용
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return userRemoteDataSource.updateAccountStatus(userId, accountStatus)
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> {
        // 현재 로그인된 사용자에게만 적용
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return userRemoteDataSource.updateFcmToken(userId, token)
    }

    override suspend fun getParticipatingProjects(userId: String): Result<List<String>> {
        return userRemoteDataSource.getParticipatingProjects(userId)
    }

    override suspend fun updateParticipatingProjects(projectIds: List<String>): Result<Unit> {
        // 현재 로그인된 사용자에게만 적용
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return userRemoteDataSource.updateParticipatingProjects(userId, projectIds)
    }

    override suspend fun getActiveDmChannels(userId: String): Result<List<String>> {
        return userRemoteDataSource.getActiveDmChannels(userId)
    }

    override suspend fun updateActiveDmChannels(dmIds: List<String>): Result<Unit> {
        // 현재 로그인된 사용자에게만 적용
        val userId = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(IllegalStateException("사용자가 로그인되어 있지 않습니다."))
            
        return userRemoteDataSource.updateActiveDmChannels(userId, dmIds)
    }

    override suspend fun ensureUserProfileExists(firebaseUser: FirebaseUser): Result<User> {
        return userRemoteDataSource.ensureUserProfileExists(firebaseUser).map { userDto ->
            userMapper.mapToDomain(userDto)
        }
    }
}