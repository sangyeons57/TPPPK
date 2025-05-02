// 경로: domain/repository/ProjectMemberRepository.kt (신규 생성 또는 기존 파일 수정)
package com.example.domain.repository

import com.example.domain.model.ProjectMember
import kotlinx.coroutines.flow.Flow
import kotlin.Result

interface ProjectMemberRepository {
    // --- MemberListViewModel 용 ---
    /** 특정 프로젝트의 멤버 목록 가져오기 (Flow 사용 가능) */
    fun getProjectMembersStream(projectId: String): Flow<List<ProjectMember>>
    /** 멤버 목록 새로고침 */
    suspend fun fetchProjectMembers(projectId: String): Result<Unit>

    // --- EditMemberViewModel 용 (미리 추가) ---
    /** 특정 멤버의 역할 업데이트 */
     suspend fun updateMemberRoles(projectId: String, userId: String, roleIds: List<String>): Result<Unit>
    /** 특정 멤버의 정보 가져오기 (역할 ID 목록 포함 가정) */
    suspend fun getProjectMember(projectId: String, userId: String): Result<ProjectMember> // 역할 ID 포함하도록 모델 수정 필요 가정
    // TODO: 멤버 초대, 추방 등 기능 추가
}