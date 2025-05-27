package com.example.data._repository

import com.example.core_common.result.resultTry
import com.example.data.datasource._remote.UserRemoteDataSource
import com.example.data.datasource._remote.AuthRemoteDataSource // 현재 사용자 ID 등 필요시
import com.example.domain._repository.MediaRepository // 이미지 업로드용
import com.example.data.model._remote.UserDTO
import com.example.data.model.mapper.toDomain
import com.example.data.model.mapper.toDto
import com.example.domain.model.User
import com.example.domain.model.UserStatus
import com.example.domain.model.AccountStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import javax.inject.Inject
import kotlin.Result

class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val authRemoteDataSource: AuthRemoteDataSource, // 현재 사용자 UID 등 필요시
    private val mediaRepository: MediaRepository // 프로필 이미지 업로드
    // private val userMapper: UserMapper // 개별 매퍼 사용시
) : UserRepository {

    override suspend fun getCurrentUserProfile(currentUserId: String): Result<User> = resultTry {
        userRemoteDataSource.getUser(currentUserId).getOrThrow().toDomain()
    }

    override fun getCurrentUserProfileStream(currentUserId: String): Flow<Result<User>> {
        // UserRemoteDataSource에 getUserStream(userId) 함수 필요
        return userRemoteDataSource.getUserStream(currentUserId).map { result ->
            result.mapCatching { it.toDomain() }
        }
    }

    override suspend fun getUserProfile(userId: String): Result<User> = resultTry {
        userRemoteDataSource.getUser(userId).getOrThrow().toDomain()
    }

    override suspend fun createUserProfile(user: User): Result<Unit> = resultTry {
        // 회원가입 시 Auth에서 UID를 받고, 그 UID를 ID로 사용하여 UserDTO 생성
        val userDto = user.toDto() // User 도메인 객체를 UserDTO로 변환
        userRemoteDataSource.createUser(userDto).getOrThrow()
    }

    override suspend fun updateUserProfile(userId: String, newNickname: String?, newStatusMessage: String?): Result<Unit> = resultTry {
        // UserRemoteDataSource에 updateUserProfile(userId, nickname, statusMessage) 함수 필요
        // 또는 개별 업데이트 함수 (updateNickname, updateStatusMessage)
        // Firestore Map을 사용한 부분 업데이트 권장
        val updates = mutableMapOf<String, Any?>()
        newNickname?.let { updates[\
nickname\] = it }
        // statusMessage는 null로 설정하여 필드 제거 가능성을 고려해야 할 수 있음
        updates[\statusMessage\] = newStatusMessage // null이면 필드 제거 또는 유지 (DataSource 정책에 따름)
        updates[\updatedAt\] = Timestamp.now()
        
        if (newNickname != null) { // 닉네임 변경 시 중복 확인 선행 가능 (UseCase에서)
             val isAvailable = userRemoteDataSource.checkNicknameAvailability(newNickname).getOrThrow()
             if (!isAvailable) throw IllegalArgumentException(\Nickname
already
in
use.\)
        }
        userRemoteDataSource.updateUser(userId, updates).getOrThrow()
    }

    override suspend fun updateUserProfileImage(
        userId: String,
        imageInputStream: InputStream,
        imageMimeType: String
    ): Result<String?> = resultTry {
        val mediaImage = mediaRepository.uploadImage(
            inputStream = imageInputStream,
            mimeType = imageMimeType,
            storagePath = \user_profile_images\,
            desiredFileName = \\_\\
        ).getOrThrow()
        
        userRemoteDataSource.updateUserProfileImageUrl(userId, mediaImage.url).getOrThrow()
        mediaImage.url
    }

    override suspend fun deleteUserProfileImage(userId: String): Result<Unit> = resultTry {
        // 1. 현재 이미지 URL 가져오기 (삭제를 위해) - UserRemoteDataSource.getUser 필요
        // 2. MediaRepository.deleteImageByUrl 호출 (선택적, Storage에서 실제 파일 삭제)
        // 3. UserRemoteDataSource.updateUserProfileImageUrl(userId, null) 호출하여 Firestore에서 URL 제거
        val currentUser = userRemoteDataSource.getUser(userId).getOrThrow()
        currentUser.profileImageUrl?.let {
            // mediaRepository.deleteImageByUrl(it) // 실제 파일 삭제 (오류 무시 가능)
        }
        userRemoteDataSource.updateUserProfileImageUrl(userId, null).getOrThrow()
    }

    override suspend fun updateUserConnectionStatus(userId: String, newStatus: UserStatus): Result<Unit> = resultTry {
        // UserRemoteDataSource에 updateUserStatus(userId, statusName) 함수가 이미 있음
        userRemoteDataSource.updateUserStatus(userId, newStatus.name).getOrThrow()
    }

    override suspend fun checkNicknameAvailability(nickname: String): Result<Boolean> = resultTry {
        // UserRemoteDataSource에 checkNicknameAvailability(nickname) 함수가 이미 있음
        userRemoteDataSource.checkNicknameAvailability(nickname).getOrThrow()
    }

    override suspend fun searchUsersByName(name: String, limit: Int): Result<List<User>> = resultTry {
        // UserRemoteDataSource에 searchUsersByName(name, limit) 함수가 이미 있음
        userRemoteDataSource.searchUsersByName(name, limit).getOrThrow().map { it.toDomain() }
    }

    override suspend fun updateUserFcmToken(userId: String, token: String): Result<Unit> = resultTry {
        // UserRemoteDataSource에 addOrUpdateFcmToken(userId, token) 함수 필요
        userRemoteDataSource.addOrUpdateFcmToken(userId, token).getOrThrow()
    }

    override suspend fun removeUserFcmToken(userId: String, token: String): Result<Unit> = resultTry {
        // UserRemoteDataSource에 removeFcmToken(userId, token) 함수 필요
        userRemoteDataSource.removeFcmToken(userId, token).getOrThrow()
    }
    
    override suspend fun updateUserAccountStatusByAdmin(
        targetUserId: String,
        newAccountStatus: AccountStatus,
        adminUserId: String // DataSource에서 권한 확인용
    ): Result<Unit> = resultTry {
        // UserRemoteDataSource에 updateUserAccountStatus(targetUserId, statusName, adminUserId) 함수가 이미 있음
        userRemoteDataSource.updateUserAccountStatus(targetUserId, newAccountStatus.name, adminUserId).getOrThrow()
    }

    override suspend fun updateUserMemoByAdmin(
        targetUserId: String,
        memo: String,
        adminUserId: String // DataSource에서 권한 확인용
    ): Result<Unit> = resultTry {
        // UserRemoteDataSource에 updateUserMemo(targetUserId, memo, adminUserId) 함수가 이미 있음
        userRemoteDataSource.updateUserMemo(targetUserId, memo, adminUserId).getOrThrow()
    }
}
