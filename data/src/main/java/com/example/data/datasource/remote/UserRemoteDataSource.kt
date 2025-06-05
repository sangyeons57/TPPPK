
package com.example.data.datasource.remote

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.data.model.remote.UserDTO
import com.example.data.model.remote.ProjectsWrapperDTO // Added import
import com.example.data.model.remote.DMWrapperDTO
import kotlinx.coroutines.flow.Flow

interface UserRemoteDataSource {

    /**
     * 현재 로그인된 사용자의 정보를 Firestore에서 가져옵니다.
     */
    suspend fun getMyUserInfo(): CustomResult<UserDTO, Exception>

    /**
     * 특정 userId를 가진 사용자의 정보를 실시간으로 관찰합니다.
     * @param userId 관찰할 사용자의 ID
     */
    fun observeUser(userId: String): Flow<CustomResult<UserDTO, Exception>>

    /**
     * 회원가입 후 Firestore에 새로운 사용자 문서를 생성합니다.
     * @param user 생성할 사용자 정보 DTO
     */
    suspend fun createUser(user: UserDTO): CustomResult<Unit, Exception>

    /**
     * 사용자의 프로필 주요 정보를 업데이트합니다. (이름, 프로필 이미지)
     * @param name 변경할 이름
     * @param profileImageUrl 변경할 프로필 이미지 URL
     */
    suspend fun updateUserProfile(name: String, userDTO: UserDTO): CustomResult<Unit, Exception>
    
    /**
     * 사용자의 FCM 토큰을 업데이트합니다.
     * @param token 새로운 FCM 토큰
     */
    suspend fun updateFcmToken(token: String): CustomResult<Unit, Exception>

    /**
     * 프로필 이미지를 Storage에 업로드하고 다운로드 URL을 반환합니다.
     * @param imageUri 디바이스에 있는 이미지의 Uri
     * @return Firebase Storage에 저장된 이미지의 다운로드 URL
     */
    suspend fun uploadProfileImage(imageUri: Uri): CustomResult<String, Exception>

    /**
     * 이름(닉네임)으로 사용자를 검색합니다.
     * @param nameQuery 검색할 이름 문자열
     * @return 검색 결과에 해당하는 사용자 DTO 목록
     */
    suspend fun searchUsersByName(nameQuery: String): CustomResult<List<UserDTO>, Exception>
    
    /**
     * 이름(닉네임)으로 사용자를 검색하고 결과 수를 제한합니다.
     * @param nameQuery 검색할 이름 문자열
     * @param maxResults 결과 수 제한
     * @return 검색 결과에 해당하는 사용자 DTO 목록
     */
    suspend fun searchUsersByName(nameQuery: String, maxResults: Int): CustomResult<List<UserDTO>, Exception>

    /**
     * 닉네임(이름)이 이미 사용 중인지 확인합니다.
     * @param nickname 확인할 닉네임
     * @return 사용 가능하면 true, 아니면 false
     */
    suspend fun checkNicknameAvailability(nickname: String): CustomResult<Boolean, Exception>

    /**
     * 현재 로그인된 사용자의 상태 메시지를 업데이트합니다.
     * @param status 새로운 상태 메시지
     */
    suspend fun updateUserStatus(status: String): CustomResult<Unit, Exception>

    /**
     * 현재 로그인된 사용자의 계정 상태를 업데이트합니다.
     * @param accountStatus 새로운 계정 상태
     */
    suspend fun updateUserAccountStatus(accountStatus: String): CustomResult<Unit, Exception>

    /**
     * 현재 로그인된 사용자의 메모를 업데이트합니다.
     * @param memo 새로운 메모
     */
    suspend fun updateUserMemo(memo: String): CustomResult<Unit, Exception>

    /**
     * Firestore에서 특정 사용자의 프로필 이미지 URL 필드만 업데이트합니다.
     *
     * @param userId 업데이트할 사용자의 ID.
     * @param imageUrl 새 프로필 이미지의 다운로드 URL. null일 경우 필드를 제거하거나 기본값으로 설정할 수 있습니다.
     * @return 작업 성공 시 [CustomResult.Success] (Unit), 실패 시 [CustomResult.Failure] (Exception).
     */
    suspend fun updateUserProfileImageUrl(userId: String, imageUrl: String?): CustomResult<Unit, Exception>

    /**
     * 특정 userId를 가진 사용자의 프로젝트 요약 정보(ProjectsWrapper)를 실시간으로 관찰합니다.
     * @param userId 관찰할 사용자의 ID
     * @return ProjectsWrapperDTO 목록을 담은 Flow
     */
    @Deprecated("Reads from a legacy field in the user document. Project participation IDs are now primarily managed via ProjectsWrapperRemoteDataSource (accessed via ProjectsWrapperRepository).")
    fun getProjectWrappersStream(userId: String): Flow<CustomResult<List<ProjectsWrapperDTO>, Exception>>

    /**
     * 특정 userId를 가진 사용자의 DM 요약 정보(DMWrapper)를 실시간으로 관찰합니다.
     * @param userId 관찰할 사용자의 ID
     * @return DMWrapperDTO 목록을 담은 Flow
     */
    fun getDmWrappersStream(userId: String): Flow<CustomResult<List<DMWrapperDTO>, Exception>>
}
