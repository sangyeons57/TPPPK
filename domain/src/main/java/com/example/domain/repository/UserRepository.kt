package com.example.domain._repository

import com.example.domain.model.User
import com.example.domain.model.UserStatus
import com.example.domain.model.AccountStatus
import java.io.InputStream
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 사용자 정보 조회, 업데이트, 계정 관리 등과 관련된 데이터 처리를 위한 인터페이스입니다.
 */
interface UserRepository {
    suspend fun getCurrentUserProfile(currentUserId: String): Result<User>
    fun getCurrentUserProfileStream(currentUserId: String): Flow<Result<User>>
    suspend fun getUserProfile(userId: String): Result<User>
    suspend fun createUserProfile(user: User): Result<Unit>
    suspend fun updateUserProfile(userId: String, newNickname: String?, newStatusMessage: String?): Result<Unit>
    suspend fun updateUserProfileImage(
        userId: String,
        imageInputStream: InputStream,
        imageMimeType: String
    ): Result<String?>
    suspend fun deleteUserProfileImage(userId: String): Result<Unit>
    suspend fun updateUserConnectionStatus(userId: String, newStatus: UserStatus): Result<Unit>
    suspend fun checkNicknameAvailability(nickname: String): Result<Boolean>
    suspend fun searchUsersByName(name: String, limit: Int = 10): Result<List<User>>
    suspend fun updateUserFcmToken(userId: String, token: String): Result<Unit>
    suspend fun removeUserFcmToken(userId: String, token: String): Result<Unit>
    suspend fun updateUserAccountStatusByAdmin(
        targetUserId: String,
        newAccountStatus: AccountStatus,
        adminUserId: String
    ): Result<Unit>
    suspend fun updateUserMemoByAdmin(
        targetUserId: String,
        memo: String,
        adminUserId: String
    ): Result<Unit>
}
