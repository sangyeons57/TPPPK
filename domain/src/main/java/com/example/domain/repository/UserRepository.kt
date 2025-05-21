// 경로: domain/repository/UserRepository.kt
package com.example.domain.repository

import android.net.Uri
import android.net.Uri // Ensure Uri is imported if not already
import com.example.domain.model.User
import com.example.domain.model.UserProfileData // Import UserProfileData
import com.example.domain.model.UserStatus
import com.example.domain.model.AccountStatus
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
// Replace kotlin.Result with our domain specific Result for new methods
import com.example.domain.model.Result // Import our Result
// Keep kotlin.Result for existing methods if they are not being changed in this step
// For clarity, it's better to migrate all to the same Result type, but let's stick to the subtask for now.
// The subtask implies new methods should use the specified Result.

/**
 * 사용자 프로필 데이터 관리를 위한 인터페이스
 * Firestore 'users' 컬렉션과의 상호작용을 정의합니다.
 */
interface UserRepository {

    // --- Methods required by the new UseCases ---
    suspend fun getMyProfile(): Result<User> // Changed to User
    suspend fun getUserProfileImageUrl(userId: String): Result<String?> // Result from our model
    suspend fun updateUserProfile(name: String, profileImageUrl: String?): Result<Unit> // Result from our model
    suspend fun uploadProfileImage(imageUri: Uri): Result<String> // Result from our model, returns download URL

    // --- Existing methods (signatures might need to be adjusted if they conflict or should use the new Result) ---
    /**
     * 현재 로그인한 사용자 프로필 정보를 실시간 스트림으로 가져오기
     * @return 현재 사용자 정보를 실시간으로 제공하는 Flow
     */
    fun getCurrentUserStream(): Flow<kotlin.Result<User>> // Assuming this uses kotlin.Result for now

    /** 
     * 특정 사용자의 프로필 정보 스트림 (실시간 업데이트) 
     * @param userId 조회할 사용자의 ID
     * @return 사용자 정보를 실시간으로 제공하는 Flow
     */
    fun getUserStream(userId: String): Flow<kotlin.Result<User>> // Assuming this uses kotlin.Result for now

    /** 현재 로그인한 사용자의 상태를 가져옵니다. */
    suspend fun getCurrentStatus(): kotlin.Result<UserStatus> // Assuming this uses kotlin.Result for now

    /** 닉네임 중복 확인 */
    suspend fun checkNicknameAvailability(nickname: String): kotlin.Result<Boolean> // Assuming this uses kotlin.Result for now
    
    /**
     * 이름(닉네임)으로 사용자를 검색합니다.
     * @param name 검색할 이름
     * @return 검색 결과에 해당하는 사용자 목록 또는 에러를 포함하는 Result
     */
    suspend fun searchUsersByName(name: String): kotlin.Result<List<User>> // Assuming this uses kotlin.Result for now

    /** 사용자 프로필 생성 */
    suspend fun createUserProfile(user: User): kotlin.Result<Unit> // Assuming this uses kotlin.Result for now
    
    /** 사용자 프로필 업데이트 (기존 메서드, 시그니처 다름) */
    suspend fun updateUserProfile(user: User): kotlin.Result<Unit> // Assuming this uses kotlin.Result for now

    /** 사용자 프로필 이미지 업데이트 (기존 메서드, 시그니처 다름, uploadProfileImage로 대체될 수 있음) */
    // suspend fun updateProfileImage(imageUri: Uri): kotlin.Result<String?> // 성공 시 새 이미지 URL 반환 - 이 메서드는 uploadProfileImage로 대체될 것임.
    // For now, let's comment it out to avoid confusion if its functionality is fully covered by uploadProfileImage.
    // If it's different (e.g. doesn't return URL but just confirms update), it might stay.
    // The new 'uploadProfileImage' returns non-nullable String. This one returns nullable.
    // Let's assume the new one is the target.

    /** 사용자 프로필 이미지 제거 */
    suspend fun removeProfileImage(): kotlin.Result<Unit> // Assuming this uses kotlin.Result for now

    /** 사용자 닉네임 업데이트 */
    suspend fun updateNickname(newNickname: String): kotlin.Result<Unit> // Assuming this uses kotlin.Result for now

    /** 사용자 메모(상태 메시지) 업데이트 */
    suspend fun updateUserMemo(newMemo: String): kotlin.Result<Unit> // Assuming this uses kotlin.Result for now

    /** 현재 사용자 상태 가져오기 */
    suspend fun getUserStatus(userId: String): kotlin.Result<UserStatus> // Assuming this uses kotlin.Result for now

    /** 사용자 상태 업데이트 */
    suspend fun updateUserStatus(status: UserStatus): kotlin.Result<Unit> // Assuming this uses kotlin.Result for now
    
    /** 사용자 계정 상태 업데이트 */
    suspend fun updateAccountStatus(accountStatus: AccountStatus): kotlin.Result<Unit> // Assuming this uses kotlin.Result for now
    
    /** FCM 토큰 업데이트 */
    suspend fun updateFcmToken(token: String): kotlin.Result<Unit> // Assuming this uses kotlin.Result for now
    
    /** 참여 중인 프로젝트 목록 가져오기 */
    suspend fun getParticipatingProjects(userId: String): kotlin.Result<List<String>> // Assuming this uses kotlin.Result for now
    
    /** 참여 프로젝트 목록 업데이트 */
    suspend fun updateParticipatingProjects(projectIds: List<String>): kotlin.Result<Unit> // Assuming this uses kotlin.Result for now
    
    /** 활성 DM 채널 목록 가져오기 */
    suspend fun getActiveDmChannels(userId: String): kotlin.Result<List<String>> // Assuming this uses kotlin.Result for now
    
    /** 활성 DM 채널 목록 업데이트 */
    suspend fun updateActiveDmChannels(dmIds: List<String>): kotlin.Result<Unit> // Assuming this uses kotlin.Result for now

    /**
     * 현재 인증된 사용자 정보를 기반으로 사용자 문서가 존재하는지 확인하고,
     * 없으면 필요한 정보를 사용하여 문서를 생성합니다.
     * @param userId 현재 로그인된 사용자 ID
     * @param email 사용자 이메일 주소
     * @return 성공 시 생성된 User 객체, 실패 시 에러 포함 Result
     */
    suspend fun ensureUserProfileExists(firebaseUser: FirebaseUser): kotlin.Result<User> // Assuming this uses kotlin.Result for now

    /**
     * 현재 로그인된 사용자의 고유 ID를 반환합니다.
     * 사용자가 로그인되어 있지 않거나 ID를 가져올 수 없는 경우 예외를 발생시킵니다.
     *
     * @return 현재 사용자의 ID (String).
     * @throws IllegalStateException 사용자를 찾을 수 없거나 인증되지 않은 경우.
     */
    suspend fun getCurrentUserId(): String
}