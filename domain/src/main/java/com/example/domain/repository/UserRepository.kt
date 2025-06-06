package com.example.domain.repository

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.base.User
import java.io.InputStream
import kotlinx.coroutines.flow.Flow

/**
 * 사용자 정보 조회, 업데이트, 계정 관리 등과 관련된 데이터 처리를 위한 인터페이스입니다.
 */
interface UserRepository {
    /**
     * 사용자 ID로 사용자 정보를 실시간 스트림으로 가져옵니다.
     *
     * @param userId 사용자 ID
     * @return 사용자 정보를 담은 Flow
     */
    fun getUserStream(userId: String): Flow<CustomResult<User, Exception>>

    /**
     * 새로운 사용자 프로필을 생성합니다.
     *
     * @param user 생성할 사용자 정보
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend fun createUserProfile(user: User): CustomResult<Unit, Exception>

    /**
     * 사용자 프로필 정보를 업데이트합니다.
     *
     * @param userId 사용자 ID
     * @param user 새 사용자 정보
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend fun updateUserProfile(
        userId: String,
        user: User,
        localImageUri: Uri? = null
    ): CustomResult<User, Exception>

    /**
     * 사용자 프로필 이미지를 삭제합니다.
     * 
     * @param userId 사용자 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend fun deleteUserProfileImage(userId: String): CustomResult<Unit, Exception>
    
    /**
     * 사용자 접속 상태를 업데이트합니다.
     * 
     * @param userId 사용자 ID
     * @param newStatus 새 상태
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend fun updateUserConnectionStatus(userId: String, newStatus: UserStatus): CustomResult<Unit, Exception>
    
    /**
     * 닉네임 사용 가능 여부를 확인합니다.
     * 
     * @param nickname 확인할 닉네임
     * @return 사용 가능하면 true, 이미 사용 중이면 false
     */
    suspend fun checkNicknameAvailability(nickname: String): CustomResult<Boolean, Exception>
    
    /**
     * 이름으로 사용자를 검색합니다.
     * 
     * @param name 검색할 이름
     * @param limit 검색 결과 제한 개수
     * @return 검색된 사용자 목록
     */
    suspend fun searchUsersByName(name: String, limit: Int = 10): CustomResult<List<User>, Exception>
    
    /**
     * 사용자의 FCM 토큰을 업데이트합니다.
     * 
     * @param userId 사용자 ID
     * @param token 새 FCM 토큰
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend fun updateUserFcmToken(userId: String, token: String): CustomResult<Unit, Exception>
    
    /**
     * 사용자의 FCM 토큰을 제거합니다.
     * 
     * @param userId 사용자 ID
     * @param token 제거할 FCM 토큰
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend fun removeUserFcmToken(userId: String, token: String): CustomResult<Unit, Exception>
    
    /**
     * 관리자가 사용자의 계정 상태를 업데이트합니다.
     * 
     * @param targetUserId 대상 사용자 ID
     * @param newAccountStatus 새 계정 상태
     * @param adminUserId 관리자 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend fun updateUserAccountStatusByAdmin(
        targetUserId: String,
        newAccountStatus: UserAccountStatus,
        adminUserId: String
    ): CustomResult<Unit, Exception>
    
    /**
     * 관리자가 사용자의 메모를 업데이트합니다.
     * 
     * @param targetUserId 대상 사용자 ID
     * @param memo 새 메모
     * @param adminUserId 관리자 ID
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend fun updateUserMemo(
        targetUserId: String,
        memo: String,
        adminUserId: String
    ): CustomResult<Unit, Exception>

    /**
     * 사용자의 프로필 이미지 URL만 업데이트합니다.
     * Firestore의 사용자 문서 내 'profileImageUrl' 필드를 업데이트합니다.
     *
     * @param userId 업데이트할 사용자의 ID.
     * @param imageUrl 새 프로필 이미지의 다운로드 URL. null을 전달하면 이미지 URL을 제거하거나 기본값으로 설정합니다.
     * @return 작업 성공 시 [CustomResult.Success] (Unit), 실패 시 [CustomResult.Failure] (Exception).
     */
    suspend fun updateUserProfileImageUrl(userId: String, imageUrl: String?): CustomResult<Unit, Exception>

    /**
     * 사용자 ID로 해당 사용자가 참여하고 있는 프로젝트들의 요약 정보(ProjectsWrapper) 스트림을 가져옵니다.
     *
     * @param userId 사용자 ID
     * @return ProjectsWrapper 목록을 담은 Flow
     */
    @Deprecated("Use ProjectsWrapperRepository.observeProjectsWrappers(userId) to get project IDs, or GetUserParticipatingProjectsUseCase for full project details. This method relies on a legacy data structure within the user document.")
    fun getProjectWrappersStream(userId: String): Flow<CustomResult<List<ProjectsWrapper>, Exception>>

}
