package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain.repository.DefaultRepository
import kotlinx.coroutines.flow.Flow

interface MemberRepository : DefaultRepository {
    /**
     * 특정 프로젝트의 모든 멤버 목록을 스트림으로 가져옵니다.
     */
    fun getProjectMembersStream(projectId: String): Flow<CustomResult<List<Member>, Exception>>

}
