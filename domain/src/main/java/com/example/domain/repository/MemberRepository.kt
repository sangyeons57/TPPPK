package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Member
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    /**
     * 특정 프로젝트의 모든 멤버 목록을 스트림으로 가져옵니다.
     */
    fun getProjectMembersStream(projectId: String): Flow<CustomResult<List<Member>, Exception>>

    /**
     * 특정 프로젝트의 특정 멤버 정보를 스트림으로 가져옵니다.
     */
    fun getProjectMemberStream(projectId: String, userId: String): Flow<CustomResult<Member, Exception>>

    /**
     * 특정 프로젝트의 멤버 목록을 서버로부터 새로고침합니다.
     */
    suspend fun fetchProjectMembers(projectId: String): CustomResult<Unit, Exception>

    /**
     * 사용자를 프로젝트에 멤버로 추가합니다.
     * 초기 역할 등을 함께 설정할 수 있습니다.
     */
    suspend fun addMemberToProject(projectId: String, userId: String, initialRoleIds: List<String>): CustomResult<Unit, Exception>

    /**
     * 프로젝트 멤버의 정보를 업데이트합니다. (예: 프로젝트 내 닉네임 변경, 역할 변경)
     */
    suspend fun updateProjectMember(projectId: String, member: Member): CustomResult<Unit, Exception>

    /**
     * 프로젝트에서 멤버를 제거(추방)합니다.
     */
    suspend fun removeProjectMember(projectId: String, userId: String): CustomResult<Unit, Exception>

    // TODO: 필요에 따라 멤버 검색, 멤버 초대 수락/거절 등의 함수 추가 고려
}
