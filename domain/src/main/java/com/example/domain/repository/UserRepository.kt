// 경로: domain/repository/UserRepository.kt
package com.example.domain.repository

import android.net.Uri
import com.example.domain.model.User
import com.example.domain.model.UserStatus
import com.example.domain.model.AccountStatus
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlin.Result

/**
 * 사용자 프로필 데이터 관리를 위한 인터페이스
 * Firestore 'users' 컬렉션과의 상호작용을 정의합니다.
 */
interface UserRepository {

    /** 특정 사용자의 프로필 정보 가져오기 */
    suspend fun getUserProfile(userId: String): Result<User>
    
    /** 현재 로그인한 사용자 프로필 정보 가져오기 (Flow 형태로 실시간 업데이트) */
    fun getCurrentUserProfile(): Flow<Result<User?>>

    fun getCurrentStatus(): Result<UserStatus>

    /** 닉네임 중복 확인 */
    suspend fun checkNicknameAvailability(nickname: String): Result<Boolean>

    /** 사용자 프로필 생성 */
    suspend fun createUserProfile(user: User): Result<Unit>
    
    /** 사용자 프로필 업데이트 */
    suspend fun updateUserProfile(user: User): Result<Unit>

    /** 사용자 프로필 이미지 업데이트 */
    suspend fun updateProfileImage(imageUri: Uri): Result<String?> // 성공 시 새 이미지 URL 반환

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
}