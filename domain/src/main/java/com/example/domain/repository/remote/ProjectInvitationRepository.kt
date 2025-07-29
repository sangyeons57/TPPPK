package com.example.domain.repository.remote

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectInvitation
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.model.vo.invite.InviteCode

/**
 * Remote ProjectInvitation Repository Interface (Firebase Functions Only)
 * Firebase Functions를 통한 초대 관련 서버 작업 전용
 */
interface ProjectInvitationRepository : Repository {
    
    /**
     * 초대 코드로 초대 정보를 조회합니다.
     */
    suspend fun getInvitationByCode(
        inviteCode: InviteCode
    ): CustomResult<ProjectInvitation, Exception>
    
    /**
     * 초대 코드의 유효성을 검증합니다.
     */
    suspend fun validateInviteCode(
        inviteCode: InviteCode,
        userId: UserId? = null,
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * 프로젝트 초대 링크를 생성합니다.
     */
    suspend fun generateInviteLink(
        projectId: DocumentId,
        expiresInHours: Int = 24,
    ): CustomResult<Map<String, Any?>, Exception>

    /**
     * 초대 코드를 사용하여 프로젝트에 참여합니다.
     */
    suspend fun joinProjectWithInvite(inviteCode: String): CustomResult<Map<String, Any?>, Exception>
}