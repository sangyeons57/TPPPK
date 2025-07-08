package com.example.domain.repository.base

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.base.Project
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.ProjectRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 생성, 조회, 관리 등 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface ProjectRepository : DefaultRepository {
    override val factoryContext: ProjectRepositoryFactoryContext

    /**
     * 프로젝트 프로필 이미지를 업로드합니다.
     * Firebase Storage에 업로드 후 자동으로 Firebase Functions가 처리합니다.
     *
     * @param projectId 프로젝트 ID
     * @param uri 업로드할 이미지의 URI
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun uploadProfileImage(projectId: DocumentId, uri: Uri): CustomResult<Unit, Exception>

    /**
     * 프로젝트 초대 링크를 생성합니다.
     *
     * @param projectId 프로젝트 ID
     * @param expiresInHours 만료 시간 (시간 단위, 기본 24시간)
     * @param maxUses 최대 사용 횟수 (nullable)
     * @return 성공 시 초대 링크 정보, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun generateInviteLink(
        projectId: DocumentId,
        expiresInHours: Int = 24,
        maxUses: Int? = null
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * 초대 코드를 검증합니다.
     *
     * @param inviteCode 초대 코드
     * @return 성공 시 초대 정보, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun validateInviteCode(inviteCode: String): CustomResult<Map<String, Any?>, Exception>

    /**
     * 초대 코드를 사용하여 프로젝트에 참여합니다.
     *
     * @param inviteCode 초대 코드
     * @return 성공 시 참여 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun joinProjectWithInvite(inviteCode: String): CustomResult<Map<String, Any?>, Exception>

}
