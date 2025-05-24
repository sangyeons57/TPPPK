// 경로: domain/repository/UserRepository.kt
package com.example.domain.repository

// kotlin.Result를 사용하도록 변경하고, com.example.domain.model.Result import는 제거합니다.
import android.net.Uri
import com.example.domain.model.AccountStatus
// import com.example.domain.model.Result // 이 줄을 삭제합니다.
import com.example.domain.model.User
import com.example.domain.model.UserStatus
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlin.Result // kotlin.Result를 명시적으로 import 합니다.

/**
 * 사용자 프로필 데이터 관리를 위한 인터페이스
 * Firestore 'users' 컬렉션과의 상호작용을 정의합니다.
 */
interface UserRepository {

    // --- Methods required by the new UseCases ---
    suspend fun getMyProfile(): Result<User> // kotlin.Result 사용
    suspend fun getUserProfileImageUrl(userId: String): Result<String?> // kotlin.Result 사용
    suspend fun updateUserProfile(name: String, profileImageUrl: String?): Result<Unit> // kotlin.Result 사용
    suspend fun uploadProfileImage(imageUri: Uri): Result<String> // kotlin.Result 사용

    // --- Existing methods (모두 kotlin.Result를 사용하도록 통일) ---
    /**
     * 현재 로그인한 사용자 프로필 정보를 실시간 스트림으로 가져오기
     * @return 현재 사용자 정보를 실시간으로 제공하는 Flow
     */
    fun getCurrentUserStream(): Flow<Result<User>>

    /** 
     * 특정 사용자의 프로필 정보 스트림 (실시간 업데이트) 
     * @param userId 조회할 사용자의 ID
     * @return 사용자 정보를 실시간으로 제공하는 Flow
     */
    fun getUserStream(userId: String): Flow<Result<User>>

    /** 현재 로그인한 사용자의 상태를 가져옵니다. */
    suspend fun getCurrentStatus(): Result<UserStatus>

    /** 닉네임 중복 확인 */
    suspend fun checkNicknameAvailability(nickname: String): Result<Boolean>
    
    /**
     * 이름(닉네임)으로 사용자를 검색합니다.
     * @param name 검색할 이름
     * @return 검색 결과에 해당하는 사용자 목록 또는 에러를 포함하는 Result
     */
    suspend fun searchUsersByName(name: String): Result<List<User>>

    /** 사용자 프로필 생성 */
    suspend fun createUserProfile(user: User): Result<Unit>
    
    /** 사용자 프로필 업데이트 (기존 메서드, 시그니처 다름) */
    suspend fun updateUserProfile(user: User): Result<Unit>

    /** 사용자 프로필 이미지 제거 */
    suspend fun removeProfileImage(): Result<Unit>

    /** 사용자 닉네임 업데이트 */
    suspend fun updateNickname(newNickname: String): Result<Unit>

    /** 사용자 메모(상태 메시지) 업데이트 */
    suspend fun updateUserMemo(newMemo: String): Result<Unit>

    /** 현재 사용자 상태 가져오기 */
    suspend fun getUserStatus(userId: String): Result<UserStatus>

    /** 사용자 상태 업데이트 */
    suspend fun updateUserStatus(status: UserStatus): Result<Unit>
    
    /** 사용자 계정 상태 업데이트 */
    suspend fun updateAccountStatus(accountStatus: AccountStatus): Result<Unit>
    
    /** FCM 토큰 업데이트 */
    suspend fun updateFcmToken(token: String): Result<Unit>
    
    /** 참여 중인 프로젝트 목록 가져오기 */
    suspend fun getParticipatingProjects(userId: String): Result<List<String>>
    
    /** 참여 프로젝트 목록 업데이트 */
    suspend fun updateParticipatingProjects(projectIds: List<String>): Result<Unit>
    
    /** 활성 DM 채널 목록 가져오기 */
    suspend fun getActiveDmChannels(userId: String): Result<List<String>>
    
    /** 활성 DM 채널 목록 업데이트 */
    suspend fun updateActiveDmChannels(dmIds: List<String>): Result<Unit>

    /**
     * 현재 인증된 사용자 정보를 기반으로 사용자 문서가 존재하는지 확인하고,
     * 없으면 필요한 정보를 사용하여 문서를 생성합니다.
     * @param userId 현재 로그인된 사용자 ID
     * @param email 사용자 이메일 주소
     * @return 성공 시 생성된 User 객체, 실패 시 에러 포함 Result
     */
    suspend fun ensureUserProfileExists(firebaseUser: FirebaseUser): Result<User>

    /**
     * 현재 로그인된 사용자의 고유 ID를 반환합니다.
     * 사용자가 로그인되어 있지 않거나 ID를 가져올 수 없는 경우 예외를 발생시킵니다.
     *
     * @return 현재 사용자의 ID (String).
     * @throws IllegalStateException 사용자를 찾을 수 없거나 인증되지 않은 경우.
     */
    suspend fun getCurrentUserId(): String

    /**
     * 사용자의 민감한 정보를 제거하고 계정을 '탈퇴' 상태로 표시합니다.
     * Firestore/RTDB의 사용자 문서를 완전히 삭제하지 않고, 개인 식별 정보(PII)를 제거하거나 null 처리하며,
     * 계정 상태를 'WITHDRAWN' 또는 유사한 값으로 업데이트합니다.
     * 사용자 이름, 채팅 기록 등은 유지될 수 있습니다.
     * @return 성공 시 Unit, 실패 시 에러를 포함하는 Result
     */
    suspend fun clearSensitiveUserDataAndMarkAsWithdrawn(): Result<Unit>
}