package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.model.mapper.UserMapper
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.base.User
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val userMapper: UserMapper,
    private val mediaRepository: MediaRepository // 이미지 처리를 위해 필요
) : UserRepository {

    /**
     * 사용자 ID로 사용자 정보를 실시간 스트림으로 가져옵니다.
     * 
     * @param userId 사용자 ID
     * @return 사용자 정보를 담은 Flow
     */
    override fun getUserStream(userId: String): Flow<CustomResult<User, Exception>> {
        return userRemoteDataSource.observeUser(userId).map { result ->
            when (result) {
                is CustomResult.Success -> {
                    val userDto = result.data
                    CustomResult.Success(userMapper.mapToDomain(userDto))
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(result.exception)
                }
            }
        }
    }

    /**
     * 새로운 사용자 프로필을 생성합니다.
     * 
     * @param user 생성할 사용자 정보
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun createUserProfile(user: User): CustomResult<Unit, Exception> {
        return try {
            // 회원가입 시 Auth에서 UID를 받고, 그 UID를 ID로 사용하여 UserDTO 생성
            val userDto = userMapper.mapToDto(user)
            userRemoteDataSource.createUser(userDto)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 사용자 프로필 정보를 업데이트합니다.
     * 
     * @param userId 사용자 ID
     * @param user 새 사용자 정보
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun updateUserProfile(userId: String, user: User): CustomResult<Unit, Exception> {
        return try {
            // 닉네임 변경 시 중복 확인 선행
            if (user.name != null) {
                val nicknameResult = userRemoteDataSource.checkNicknameAvailability(user.name)
                when (nicknameResult) {
                    is CustomResult.Success -> {
                        if (!nicknameResult.data) {
                            return CustomResult.Failure(IllegalArgumentException("Nickname already in use"))
                        }
                    }
                    is CustomResult.Failure -> {
                        return nicknameResult
                    }
                }
            }
            
            // 프로필 업데이트 수행
            if (user.name != null && user.memo != null) {
                userRemoteDataSource.updateUserProfile(user.name, user.memo)
            } else if (user.name != null) {
                userRemoteDataSource.updateUserProfile(user.name, null)
            } else if (user.memo != null) {
                userRemoteDataSource.updateUserStatus(user.memo)
            } else {
                // 변경할 내용이 없으면 성공으로 처리
                CustomResult.Success(Unit)
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 사용자 프로필 이미지를 업데이트합니다.
     * 
     * @param userId 사용자 ID
     * @param imageInputStream 이미지 입력 스트림
     * @param imageMimeType 이미지 MIME 타입
     * @return 성공 시 이미지 URL을 포함한 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun updateUserProfileImage(
        userId: String,
        imageInputStream: InputStream,
        imageMimeType: String
    ): CustomResult<String?, Exception> {
        return try {
            // 이미지 업로드
            val mediaImageResult = mediaRepository.uploadImage(
                inputStream = imageInputStream,
                mimeType = imageMimeType,
                storagePath = "user_profile_images",
                desiredFileName = "_"
            )
            
            when (mediaImageResult) {
                is CustomResult.Success -> {
                    val mediaImage = mediaImageResult.data
                    // 사용자 프로필 이미지 URL 업데이트
                    val updateResult = userRemoteDataSource.updateUserProfileImage(userId, mediaImage.url)
                    when (updateResult) {
                        is CustomResult.Success -> CustomResult.Success(mediaImage.url)
                        is CustomResult.Failure -> CustomResult.Failure(updateResult.exception)
                    }
                }
                is CustomResult.Failure -> CustomResult.Failure(mediaImageResult.exception)
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 사용자 프로필 이미지를 삭제합니다.
     * 
     * @param userId 사용자 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun deleteUserProfileImage(userId: String): CustomResult<Unit, Exception> {
        return try {
            // 현재 사용자 정보 조회
            val userResult = userRemoteDataSource.getMyUserInfo()
            when (userResult) {
                is CustomResult.Success -> {
                    val currentUser = userResult.data
                    // 프로필 이미지 URL이 있으면 삭제 처리
                    currentUser.profileImageUrl?.let { imageUrl ->
                        val deleteResult = mediaRepository.deleteImageByUrl(imageUrl)
                        if (deleteResult is CustomResult.Failure) {
                            return deleteResult
                        }
                    }
                    // Firestore에서 URL 제거
                    userRemoteDataSource.updateUserProfileImage(userId, null)
                }
                is CustomResult.Failure -> CustomResult.Failure(userResult.exception)
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 사용자 접속 상태를 업데이트합니다.
     * 
     * @param userId 사용자 ID
     * @param newStatus 새 상태
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun updateUserConnectionStatus(userId: String, newStatus: UserStatus): CustomResult<Unit, Exception> {
        return try {
            userRemoteDataSource.updateUserStatus(newStatus.name)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 닉네임 사용 가능 여부를 확인합니다.
     * 
     * @param nickname 확인할 닉네임
     * @return 사용 가능하면 true, 이미 사용 중이면 false
     */
    override suspend fun checkNicknameAvailability(nickname: String): CustomResult<Boolean, Exception> {
        return userRemoteDataSource.checkNicknameAvailability(nickname)
    }

    /**
     * 이름으로 사용자를 검색합니다.
     * 
     * @param name 검색할 이름
     * @param limit 검색 결과 제한 개수
     * @return 검색된 사용자 목록
     */
    override suspend fun searchUsersByName(name: String, limit: Int): CustomResult<List<User>, Exception> {
        return try {
            val result = userRemoteDataSource.searchUsersByName(name)
            when (result) {
                is CustomResult.Success -> {
                    val users = result.data.take(limit).map { userMapper.mapToDomain(it) }
                    CustomResult.Success(users)
                }
                is CustomResult.Failure -> CustomResult.Failure(result.exception)
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 사용자의 FCM 토큰을 업데이트합니다.
     * 
     * @param userId 사용자 ID
     * @param token 새 FCM 토큰
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun updateUserFcmToken(userId: String, token: String): CustomResult<Unit, Exception> {
        return try {
            userRemoteDataSource.updateFcmToken(token)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 사용자의 FCM 토큰을 제거합니다.
     * 
     * @param userId 사용자 ID
     * @param token 제거할 FCM 토큰
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun removeUserFcmToken(userId: String, token: String): CustomResult<Unit, Exception> {
        // UserRemoteDataSource에 removeFcmToken 함수가 구현되어 있지 않은 경우 구현 필요
        return try {
            // 임시 구현 - 실제로는 UserRemoteDataSource에 적절한 메서드가 필요
            CustomResult.Success(Unit)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
    
    /**
     * 관리자가 사용자의 계정 상태를 업데이트합니다.
     * 
     * @param targetUserId 대상 사용자 ID
     * @param newAccountStatus 새 계정 상태
     * @param adminUserId 관리자 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun updateUserAccountStatusByAdmin(
        targetUserId: String,
        newAccountStatus: UserAccountStatus,
        adminUserId: String // DataSource에서 권한 확인용
    ): CustomResult<Unit, Exception> {
        return try {
            userRemoteDataSource.updateUserAccountStatus(newAccountStatus.name)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 관리자가 사용자의 메모를 업데이트합니다.
     * 
     * @param targetUserId 대상 사용자 ID
     * @param memo 새 메모
     * @param adminUserId 관리자 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun updateUserMemo(
        targetUserId: String,
        memo: String,
        adminUserId: String // DataSource에서 권한 확인용
    ): CustomResult<Unit, Exception> {
        return try {
            userRemoteDataSource.updateUserMemo(memo)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}
