package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.base.Invite
import com.example.domain.repository.DefaultRepository
import kotlinx.coroutines.flow.Flow

/**
 * 초대 코드 생성, 조회, 사용 등 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface InviteRepository : DefaultRepository {

    /**
     * 특정 프로젝트에 대해 생성된 활성 초대 코드 목록을 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return 활성 초대 코드 목록을 담은 Result Flow.
     */
    fun getActiveProjectInvitesStream(projectId: String): Flow<CustomResult<List<Invite>, Exception>>
}
