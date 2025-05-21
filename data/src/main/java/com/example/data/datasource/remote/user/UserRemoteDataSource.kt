package com.example.data.datasource.remote.user

import android.net.Uri
import com.example.data.model.remote.user.UserDto
import com.example.domain.model.AccountStatus
import com.example.domain.model.UserProfileData // Import UserProfileData
import com.example.domain.model.UserStatus
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlin.Result // For existing methods
import com.example.domain.model.Result as DomainResult // For new methods

/**
 * Firestore 'users' 컬렉션과의 상호작용을 추상화하는 데이터 소스 인터페이스
 */
interface UserRemoteDataSource {

    // --- New methods required by the subtask ---
    suspend fun getMyProfile(): DomainResult<com.example.domain.model.User> // Changed to User
    suspend fun getUserProfileImageUrl(userId: String): DomainResult<String?>
    suspend fun updateUserProfile(name: String, profileImageUrl: String?): DomainResult<Unit>
    suspend fun uploadProfileImage(imageUri: Uri): DomainResult<String> // Returns download URL

    // --- Existing methods ---
    /**
     * 특정 사용자의 프로필 정보를 가져옵니다.
     *
     * @param userId 조회할 사용자의 ID
     * @return 성공 시 UserDto가 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun getUserStream(userId: String): Flow<Result<UserDto?>>

    /**
     * 현재 로그인한 사용자의 프로필 정보를 Flow로 가져옵니다.
     *
     * @return UserDto를 포함한 Flow (Firestore 문서 변경 시 실시간 업데이트)
     */
    suspend fun getCurrentUserStream(): Flow<Result<UserDto?>>

    /**
     * 닉네임 중복 여부를 확인합니다.
     *
     * @param nickname 확인할 닉네임
     * @return 성공 시 사용 가능 여부(Boolean)가 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun checkNicknameAvailability(nickname: String): Result<Boolean>

    /**
     * 이름(닉네임)으로 사용자를 검색합니다.
     *
     * @param name 검색할 이름
     * @return 성공 시 UserDto 목록이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun searchUsersByName(name: String): Result<List<UserDto>>

    /**
     * 사용자 프로필을 생성합니다.
     *
     * @param userDto 생성할 사용자 정보
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun createUserProfile(userDto: UserDto): Result<Unit>
    
    /**
     * 사용자 프로필을 업데이트합니다.
     *
     * @param userDto 업데이트할 사용자 정보
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun updateUserProfile(userDto: UserDto): Result<Unit>

    /**
     * 사용자 프로필 이미지를 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param imageUri 업로드할 이미지 URI
     * @return 성공 시 이미지 URL(String)이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun updateProfileImage(userId: String, imageUri: Uri): Result<String?>

    /**
     * 사용자 프로필 이미지를 제거합니다.
     *
     * @param userId 사용자 ID
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun removeProfileImage(userId: String): Result<Unit>

    /**
     * 사용자 닉네임을 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param newNickname 새 닉네임
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun updateNickname(userId: String, newNickname: String): Result<Unit>

    /**
     * 사용자 메모(상태 메시지)를 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param newMemo 새 메모
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun updateUserMemo(userId: String, newMemo: String): Result<Unit>

    /**
     * 사용자 상태를 가져옵니다.
     *
     * @param userId 사용자 ID
     * @return 성공 시 상태 문자열이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun getUserStatus(userId: String): Result<String>

    /**
     * 사용자 상태를 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param status 새 사용자 상태
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Unit>
    
    /**
     * 사용자 계정 상태를 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param accountStatus 새 계정 상태
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun updateAccountStatus(userId: String, accountStatus: AccountStatus): Result<Unit>
    
    /**
     * FCM 토큰을 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param token 새 FCM 토큰
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun updateFcmToken(userId: String, token: String): Result<Unit>
    
    /**
     * 참여 중인 프로젝트 목록을 가져옵니다.
     *
     * @param userId 사용자 ID
     * @return 성공 시 프로젝트 ID 목록이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun getParticipatingProjects(userId: String): Result<List<String>>
    
    /**
     * 참여 프로젝트 목록을 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param projectIds 프로젝트 ID 목록
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun updateParticipatingProjects(userId: String, projectIds: List<String>): Result<Unit>
    
    /**
     * 활성 DM 채널 목록을 가져옵니다.
     *
     * @param userId 사용자 ID
     * @return 성공 시 DM 채널 ID 목록이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun getActiveDmChannels(userId: String): Result<List<String>>
    
    /**
     * 활성 DM 채널 목록을 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param dmIds DM 채널 ID 목록
     * @return 성공 시 Unit이 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun updateActiveDmChannels(userId: String, dmIds: List<String>): Result<Unit>

    /**
     * Firebase 인증 사용자 정보를 기반으로 Firestore 사용자 프로필이 존재하는지 확인하고,
     * 없으면 생성합니다.
     *
     * @param firebaseUser Firebase 인증 사용자 객체
     * @return 성공 시 UserDto가 포함된 Result, 실패 시 에러가 포함된 Result
     */
    suspend fun ensureUserProfileExists(firebaseUser: FirebaseUser): Result<UserDto>
} 