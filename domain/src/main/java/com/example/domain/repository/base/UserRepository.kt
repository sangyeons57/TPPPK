package com.example.domain.repository.base

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

/**
 * 사용자 정보 조회, 업데이트, 계정 관리 등과 관련된 데이터 처리를 위한 인터페이스입니다.
 */
interface UserRepository : DefaultRepository {

    override val factoryContext: UserRepositoryFactoryContext
    /**
     * 주어진 이름(닉네임)과 정확히 일치하는 사용자 1명을 스트림으로 반환합니다.
     */
    fun observeByName(name: UserName): Flow<CustomResult<User, Exception>>

    /**
     * 주어진 이름(닉네임)을 포함하는 사용자 목록을 스트림으로 반환합니다.
     */
    fun observeAllByName(name: String, limit: Int = 10): Flow<CustomResult<List<User>, Exception>>

    /**
     * 주어진 이메일과 정확히 일치하는 사용자 1명을 스트림으로 반환합니다.
     */
    fun observeByEmail(email: String): Flow<CustomResult<User, Exception>>

    /**
     * 사용자 ID로 해당 사용자가 참여하고 있는 프로젝트들의 요약 정보(ProjectsWrapper) 스트림을 가져옵니다.
     *
     * @param userId 사용자 ID
     * @return ProjectsWrapper 목록을 담은 Flow
     */

    /**
     * 사용자 프로필 이미지를 업로드합니다.
     * Firebase Storage에 업로드 후 자동으로 Firebase Functions가 처리합니다.
     *
     * @param uri 업로드할 이미지의 URI
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun uploadProfileImage(uri: Uri): CustomResult<Unit, Exception>

    /**
     * 사용자 프로필을 업데이트합니다.
     * Firebase Functions를 통해 이름, 메모 등의 프로필 정보를 업데이트합니다.
     *
     * @param name 새로운 사용자 이름 (nullable)
     * @param memo 새로운 사용자 메모 (nullable)
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun updateProfile(
        name: String? = null,
        memo: String? = null
    ): CustomResult<Unit, Exception>

}
