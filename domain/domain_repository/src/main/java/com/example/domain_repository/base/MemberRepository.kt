package com.example.domain_repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import com.example.domain_repository.DefaultRepository
import kotlinx.coroutines.flow.Flow

interface MemberRepository : DefaultRepository<Member> {
    /**
     * ACTIVE 상태의 멤버들만 조회합니다.
     */
    suspend fun findAllActiveMembers(): CustomResult<List<Member>, Exception>

    /**
     * ACTIVE 상태의 멤버들을 실시간으로 관찰합니다.
     */
    fun observeActiveMembers(): Flow<CustomResult<List<Member>, Exception>>

    /**
     * BLOCKED 상태의 멤버들만 조회합니다.
     */
    suspend fun findAllBlockedMembers(): CustomResult<List<Member>, Exception>

    /**
     * BLOCKED 상태의 멤버들을 실시간으로 관찰합니다.
     */
    fun observeBlockedMembers(): Flow<CustomResult<List<Member>, Exception>>
}
