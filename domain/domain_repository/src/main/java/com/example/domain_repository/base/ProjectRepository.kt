package com.example.domain_repository.base

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Project
import com.example.domain.vo.DocumentId
import com.example.domain_repository.DefaultRepository

/**
 * 프로젝트 생성, 조회, 관리 등 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface ProjectRepository : DefaultRepository<Project> {

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
     * 프로젝트 프로필 이미지를 삭제합니다.
     * Firebase Functions를 통해 프로젝트 프로필 이미지를 제거합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun removeProfileImage(projectId: DocumentId): CustomResult<Unit, Exception>

    // Invite-related operations have been moved to ProjectInvitationRepository

    /**
     * 프로젝트를 삭제합니다 (soft delete).
     *
     * @param projectId 삭제할 프로젝트 ID
     * @return 성공 시 삭제 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun deleteProject(projectId: DocumentId): CustomResult<Map<String, Any?>, Exception>

    /**
     * 프로젝트에서 나갑니다.
     *
     * @param projectId 나갈 프로젝트 ID
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun leaveProject(projectId: DocumentId): CustomResult<Unit, Exception>

    /**
     * 프로젝트에서 멤버를 제거합니다.
     *
     * @param projectId 프로젝트 ID
     * @param targetUserId 제거할 사용자 ID
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun removeMember(projectId: String, targetUserId: String): CustomResult<Unit, Exception>

    /**
     * 프로젝트에 참여합니다.
     *
     * @param projectId 참여할 프로젝트 ID
     * @return 성공 시 참여 결과 데이터 맵, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun joinProject(projectId: DocumentId): CustomResult<Map<String, Any?>, Exception>

    /**
     * 프로젝트 소유권을 다른 멤버에게 전달합니다.
     *
     * @param projectId 프로젝트 ID
     * @param newOwnerId 새로운 소유자 ID
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun transferOwnership(projectId: DocumentId, newOwnerId: String): CustomResult<Unit, Exception>

    /**
     * 프로젝트 멤버를 차단/금지합니다.
     *
     * @param projectId 프로젝트 ID
     * @param targetUserId 차단할 사용자 ID
     * @param blockType 차단 유형 ("blocked" 또는 "banned")
     * @return 성공 시 Unit, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun blockMember(
        projectId: DocumentId,
        targetUserId: String,
        blockType: String
    ): CustomResult<Unit, Exception>

}
