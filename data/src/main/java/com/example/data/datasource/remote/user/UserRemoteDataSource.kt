package com.example.data.datasource.remote.user

import android.net.Uri
import com.example.data.model.remote.user.UserDto
import com.example.domain.model.AccountStatus
// import com.example.domain.model.UserProfileData // UserProfileData는 User 모델에 통합되었거나 사용되지 않는 것으로 가정
import com.example.domain.model.UserStatus
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlin.Result // kotlin.Result를 명시적으로 사용
// import com.example.domain.model.Result as DomainResult // 이 줄을 삭제합니다.

/**
 * Firestore 'users' 컬렉션과의 상호작용을 추상화하는 데이터 소스 인터페이스
 */
interface UserRemoteDataSource {

    // --- New methods required by the subtask --- (모두 kotlin.Result 사용)
    suspend fun getMyProfile(): Result<com.example.domain.model.User>
    suspend fun getUserProfileImageUrl(userId: String): Result<String?>
    suspend fun updateUserProfile(name: String, profileImageUrl: String?): Result<Unit>
    suspend fun uploadProfileImage(imageUri: Uri): Result<String> // Returns download URL

    // --- Existing methods --- (모두 kotlin.Result 사용)
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