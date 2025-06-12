package com.example.data.repository

import android.net.Uri
import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.resultTry
import com.example.core_common.util.DateTimeUtil
import com.example.core_common.util.MediaUtil
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.model.remote.toDto
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.base.User
import com.example.domain.repository.MediaRepository
import com.example.domain.repository.UserRepository
import com.google.type.DateTime
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onErrorResume
import kotlinx.coroutines.flow.flowOf
import java.io.InputStream
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
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
                    val userDto = result.data // UserDTO?
                    CustomResult.Success(userDto.toDomain()) // Returns CustomResult<User, Nothing>
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(result.error) // Returns CustomResult<Nothing, E>
                }
                else -> { // Fallback for any unexpected result states
                    CustomResult.Failure(Exception("Unknown error observing user"))
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
            val userDto = user.toDto()
            userRemoteDataSource.createUser(userDto)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }

    /**
     * 사용자 프로필 정보와 프로필 이미지를 통합하여 업데이트합니다.
     * 
     * @param userId 사용자 ID
     * @param user 새 사용자 정보 (profileImageUrl이 null이면 삭제, 빈 문자열이면 유지, 새로운 경로면 업데이트)
     * @param localImageUri 업로드할 로컬 이미지 URI (선택사항)
     * @return 성공 시 업데이트된 User, 실패 시 Exception
     */
    override suspend fun updateUserProfile(
        userId: String,
        user: User,
        localImageUri: Uri?
    ): CustomResult<User, Exception> = resultTry {
        // 1. 현재 사용자 정보 가져오기
        val currentUserResult = userRemoteDataSource.observeUser(userId).first()
        val currentUser = when (currentUserResult) {
            is CustomResult.Success -> currentUserResult.data
            is CustomResult.Failure -> throw currentUserResult.error
            else -> throw Exception("unknown error")
        }

        // 2. 변경 사항 확인
        val isSameName = currentUser.name == user.name
        val isSameMemo = currentUser.memo == user.memo
        val isSameImage = when {
            localImageUri != null -> false // 새 이미지가 있으면 변경 있음
            user.profileImageUrl == null -> currentUser.profileImageUrl == null // 둘 다 null이면 같음
            user.profileImageUrl!!.isEmpty() -> true // 빈 문자열은 유지 요청
            else -> user.profileImageUrl == currentUser.profileImageUrl // URL 비교
        }
        
        // 3. 변경 사항이 없는 경우 조기 리턴
        if (isSameName && isSameMemo && isSameImage && localImageUri == null) {
            Log.d("UserRepository", "프로필 업데이트: 변경 사항 없음")
            return@resultTry currentUser.toDomain()
        }
        
        // 4. 닉네임 변경 시 중복 확인
        if (!isSameName) {
            Log.d("UserRepository", "닉네임 변경 확인: ${user.name}")
            val nicknameResult = userRemoteDataSource.checkNicknameAvailability(user.name)
            when (nicknameResult) {
                is CustomResult.Success -> {
                    if (!nicknameResult.data) {
                        throw IllegalArgumentException("Nickname already in use")
                    }
                }
                is CustomResult.Failure -> throw Exception(nicknameResult.toString())
                else -> {
                    throw Exception("Unknown error")
                }
            }
        }
        
        // 5. 프로필 이미지 처리
        val imageUrlResult = handleProfileImageUpdate(
            userId = userId,
            currentImageUrl = currentUser.profileImageUrl,
            newImageUrl = user.profileImageUrl,
            localImageUri = localImageUri
        )
        
        // 6. 이미지 처리 실패 시 중단
        if (imageUrlResult is CustomResult.Failure) {
            Log.e("UserRepository", "이미지 처리 실패: ${imageUrlResult.error.message}")
            throw imageUrlResult.error
        }
        
        // 7. 사용자 정보 업데이트
        val finalImageUrl = (imageUrlResult as? CustomResult.Success<String?>)?.data
        Log.d("UserRepository", "최종 이미지 URL: $finalImageUrl")
        
        val updatedUser = currentUser.copy(
            name = user.name,
            memo = user.memo,
            profileImageUrl = finalImageUrl,
            updatedAt = DateTimeUtil.nowFirebaseTimestamp()
        ).toDomain()
        
        userRemoteDataSource.updateUserProfile(userId, updatedUser.toDto())


        updatedUser
    }
    
    /**
     * 프로필 이미지 업데이트를 처리합니다.
     * 
     * @param userId 사용자 ID
     * @param currentImageUrl 현재 저장된 이미지 URL (null이면 기존 이미지 없음)
     * @param newImageUrl 새로운 이미지 URL (null이면 삭제, 빈 문자열이면 유지, 새로운 URL이면 교체)
     * @param localImageUri 업로드할 로컬 이미지 URI (선택사항)
     * @return 성공 시 새로운 이미지 URL, 실패 시 Exception
     */
    private suspend fun handleProfileImageUpdate(
        userId: String,
        currentImageUrl: String?,
        newImageUrl: String?,
        localImageUri: Uri?
    ): CustomResult<String, Exception> = resultTry {
        Log.d("UserRepository", "프로필 이미지 업데이트 처리: userId=$userId, currentUrl=$currentImageUrl, newUrl=$newImageUrl, localUri=$localImageUri")

        when {
            // 1. 로컬 이미지가 있는 경우 업로드
            localImageUri != null -> {
                // 기존 이미지 삭제 (있는 경우)
                deleteOldProfileImage(userId, currentImageUrl)

                // 새 이미지 업로드
                val fileExtension = MediaUtil.getFileExtension(localImageUri.toString()) ?: "jpg"
                val fileName = "${UUID.randomUUID()}.$fileExtension"
                val storagePath = MediaUtil.getUserProfilePath(userId, fileName)

                Log.d("UserRepository", "새 이미지 업로드 시도: path=$storagePath")
                val uploadResult = mediaRepository.uploadFile(
                    uri = localImageUri,
                    storagePath = storagePath
                )

                when (uploadResult) {
                    is CustomResult.Success -> {
                        Log.d("UserRepository", "이미지 업로드 성공: ${uploadResult.data}")
                        CustomResult.Success(uploadResult.data)
                    }

                    is CustomResult.Failure -> {
                        Log.e("UserRepository", "이미지 업로드 실패: ${uploadResult.error.message}")
                        CustomResult.Failure(uploadResult.error)
                    }

                    else -> CustomResult.Failure(Exception("Unknown error during upload"))
                }
            }

            // 2. 이미지 삭제 요청 (newImageUrl이 null인 경우)
            newImageUrl == null -> {
                Log.d("UserRepository", "프로필 이미지 삭제 요청")
                // 기존 이미지 삭제 (있는 경우)
                deleteOldProfileImage(userId, currentImageUrl)
                CustomResult.Success(null)
            }

            // 3. 이미지 유지 (newImageUrl이 빈 문자열인 경우)
            newImageUrl.isEmpty() -> {
                Log.d("UserRepository", "프로필 이미지 유지: $currentImageUrl")
                CustomResult.Success(currentImageUrl)
            }

            // 4. 외부 URL을 사용하는 경우 (기존 이미지 삭제 후 새 URL 사용)
            newImageUrl != currentImageUrl -> {
                Log.d("UserRepository", "외부 URL 사용: $newImageUrl")
                // 기존 이미지 삭제 (있는 경우)
                deleteOldProfileImage(userId, currentImageUrl)
                CustomResult.Success(newImageUrl)
            }

            // 5. 변경 사항 없음
            else -> {
                Log.d("UserRepository", "프로필 이미지 변경 없음")
                CustomResult.Success(currentImageUrl)
            }
        }.toString()
    }
    
    /**
     * 이전 프로필 이미지를 삭제합니다.
     * 
     * @param userId 사용자 ID
     * @param imageUrl 삭제할 이미지 URL (null이면 무시)
     */
    private suspend fun deleteOldProfileImage(userId: String, imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) return
        
        try {
            // Firebase Storage 경로에서 파일 삭제
            mediaRepository.deleteFile(imageUrl)
        } catch (e: Exception) {
            // 삭제 실패 시 로깅만 하고 계속 진행
            Log.e("UserRepository", "Failed to delete old profile image: ${e.message}")
        }
    }
    
    /**
     * updateUserProfileImage 메서드는 updateUserProfile 메서드로 통합되었습니다.
     * 프로필 이미지 업데이트는 updateUserProfile 메서드의 localImageUri 매개변수를 통해 처리됩니다.
     */

    /**
     * 사용자 프로필 이미지를 삭제합니다.
     * 
     * @param userId 사용자 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    override suspend fun deleteUserProfileImage(userId: String): CustomResult<Unit, Exception> {
        TODO("잠시 비활성화")
        /**
        return resultTry {
            // 현재 사용자 정보 조회
            val userResult = userRemoteDataSource.getMyUserInfo()
            when (userResult) {
                is CustomResult.Success -> {
                    val currentUser = userResult.data
                    // 프로필 이미지 URL이 있으면 삭제 처리
                    currentUser.profileImageUrl?.let { imageUrl ->
                        val deleteResult = mediaRepository.deleteFile(imageUrl)
                        if (deleteResult is CustomResult.Failure) {
                            return deleteResult
                        }
                    }
                    // Firestore에서 URL 제거
                    userRemoteDataSource.updateUserProfileImage(userId, null)
                }
                is CustomResult.Failure -> throw userResult.error
                else -> throw Exception("Unknown error")
            }
        }
        **/
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
    override suspend fun searchUsersByNameStream(name: String, limit: Int): Flow<CustomResult<List<User>, Exception>> {
        return userRemoteDataSource.searchUsersByName(name, limit.toLong()).map{ userResult ->
            when (userResult) {
                is CustomResult.Success -> {
                    val users = userResult.data.take(limit).map { it.toDomain() }
                    CustomResult.Success(users)
                }
                is CustomResult.Failure -> CustomResult.Failure(userResult.error)
                else -> CustomResult.Failure(Exception("Unknown error in searchUsersByName"))
            }
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
        TODO("나중에 FCM사용할때 다시 구현")
        /**
        return try {
            // TODO: Implement actual FCM token removal in UserRemoteDataSource
            userRemoteDataSource.updateFcmToken(token)
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
         */
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

    /**
     * 사용자의 프로필 이미지 URL만 업데이트합니다.
     * Firestore의 사용자 문서에 있는 profileImageUrl 필드를 직접 수정합니다.
     *
     * @param userId 업데이트할 사용자의 ID.
     * @param imageUrl 새 프로필 이미지의 다운로드 URL. null일 경우 필드를 제거하거나 기본값으로 설정합니다.
     * @return 작업 성공 시 [CustomResult.Success] (Unit), 실패 시 [CustomResult.Failure] (Exception).
     */
    override suspend fun updateUserProfileImageUrl(userId: String, imageUrl: String?): CustomResult<Unit, Exception> {
        return userRemoteDataSource.updateUserProfileImageUrl(userId, imageUrl)
    }

    /**
     * 이메일로 사용자 단건 조회 (단발성)
     */
    override suspend fun getUserByEmail(email: String): CustomResult<User, Exception> {
        return when (val res = userRemoteDataSource.getUserByEmail(email)) {
            is CustomResult.Success -> CustomResult.Success(res.data.toDomain())
            is CustomResult.Failure -> CustomResult.Failure(res.error)
            else -> CustomResult.Failure(Exception("Unknown error in getUserByEmail"))
        }
    }

    /**
     * 서버에서만 사용자 정보를 가져와 캐시 정합성을 확인합니다.
     */
    override suspend fun fetchUserRemote(userId: String): CustomResult<User, Exception> {
        return when (val res = userRemoteDataSource.fetchUserByIdServer(userId)) {
            is CustomResult.Success -> CustomResult.Success(res.data.toDomain())
            is CustomResult.Failure -> CustomResult.Failure(res.error)
            else -> CustomResult.Failure(Exception("Unknown error fetching remote user"))
        }
    }

    /**
     * 현재 로그인된 사용자의 메모(상태 메시지)를 업데이트합니다.
     *
     * @param newMemo 새로운 메모 문자열.
     * @return 성공 시 [CustomResult.Success] (Unit), 실패 시 [CustomResult.Failure] (Exception).
     */
    override suspend fun updateCurrentUserMemo(newMemo: String): CustomResult<Unit, Exception> {
        return userRemoteDataSource.updateUserMemo(newMemo)
    }

    /**
     * 정확한 이름으로 단일 사용자를 가져옵니다. Flow를 반환합니다.
     *
     * @param name 정확히 일치하는 사용자 이름
     * @return 사용자 정보를 담은 Flow 또는 null을 담은 Flow, 실패 시 Exception
     */
    override fun getUserByExactNameStream(name: String): Flow<CustomResult<User, Exception>> {
        return userRemoteDataSource.getUserByExactNameStream(name).map { result ->
            when (result) {
                is CustomResult.Success -> {
                    val userDto = result.data // UserDTO (non-nullable)
                    CustomResult.Success(userDto.toDomain()) // Returns CustomResult<User, Nothing>
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(result.error) // Returns CustomResult<Nothing, E>
                }
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                // Progress state might not be applicable here if it's a simple fetch,
                // but including for completeness if the datasource might emit it.
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }

    /**
     * Searches for a single user by their exact name and returns a Flow that emits the User.
     * The Flow will emit updates if the user's data changes in Firestore.
     *
     * @param name The exact name of the user to search for.
     * @return A Flow emitting [CustomResult.Success] with [User] if found, or [CustomResult.Failure] otherwise.
     */
    override suspend fun searchUserByNameStream(name: String): Flow<CustomResult<User, Exception>> {
        return userRemoteDataSource.searchUserByName(name).map { result -> // Calling the new datasource method
            when (result) {
                is CustomResult.Success -> {
                    val userDto = result.data // UserDTO
                    CustomResult.Success(userDto.toDomain()) // Convert DTO to Domain User
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(result.error) // Pass through the error
                }
                is CustomResult.Loading -> CustomResult.Loading // Pass through Loading state
                is CustomResult.Initial -> CustomResult.Initial // Pass through Initial state
                is CustomResult.Progress -> CustomResult.Progress(result.progress) // Pass through Progress state
            }
        }
    }

    override fun getProjectWrappersStream(userId: String): Flow<CustomResult<List<ProjectsWrapper>, Exception>> {
        return userRemoteDataSource.getProjectWrappersStream(userId).map { result ->
            when (result) {
                is CustomResult.Success -> {
                    val domainWrappers = result.data.map { dto -> dto.toDomain() }
                    CustomResult.Success(domainWrappers)
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(result.error)
                }
                is CustomResult.Loading -> {
                    CustomResult.Loading
                }
                is CustomResult.Initial -> {
                    CustomResult.Initial
                }
                is CustomResult.Progress -> {
                    CustomResult.Progress(result.progress)
                }
            }
        }
    }

    /**
     * Processes user withdrawal by anonymizing their data in Firestore
     * and setting their account status to WITHDRAWN.
     *
     * @param uid The ID of the user to process.
     * @return CustomResult indicating success or failure.
     */
    override suspend fun processUserWithdrawal(uid: String): CustomResult<Unit, Exception> = resultTry {
        Log.d("UserRepositoryImpl", "Processing withdrawal for user: $uid")
        val updates = mapOf(
            FirestoreConstants.Users.NAME to "UNKNOWN",
            // 이메일은 원본을 유지하여 재활성화 시 매핑 문제를 방지합니다.
            // FirestoreConstants.Users.EMAIL 항목은 더 이상 익명화하지 않습니다.
            FirestoreConstants.Users.PROFILE_IMAGE_URL to null,
            FirestoreConstants.Users.MEMO to null,
            FirestoreConstants.Users.FCM_TOKEN to null,
            FirestoreConstants.Users.ACCOUNT_STATUS to UserAccountStatus.WITHDRAWN,
            FirestoreConstants.Users.UPDATED_AT to FieldValue.serverTimestamp() // Handled by UserRemoteDataSource
        )
        val result = userRemoteDataSource.updateUserFields(uid, updates)
        if (result is CustomResult.Failure) {
            throw result.error
        }
        Log.d("UserRepositoryImpl", "User $uid withdrawal data processed successfully.")
        // CustomResult.Success(Unit) is implicitly returned by resultTry on success
    }

}
