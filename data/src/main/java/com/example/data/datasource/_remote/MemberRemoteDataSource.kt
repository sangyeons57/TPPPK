
package com.example.data.datasource._remote

import com.example.data.model._remote.MemberDTO
import kotlinx.coroutines.flow.Flow

interface MemberRemoteDataSource {

    /**
     * 특정 프로젝트의 모든 멤버 목록을 실시간으로 관찰합니다.
     * @param projectId 멤버 목록을 가져올 프로젝트의 ID
     */
    fun observeMembers(projectId: String): Flow<List<MemberDTO>>

    /**
     * 특정 프로젝트의 특정 멤버 정보를 한 번 가져옵니다.
     * @param projectId 대상 프로젝트의 ID
     * @param userId 조회할 멤버의 ID
     */
    suspend fun getProjectMember(projectId: String, userId: String): Result<MemberDTO?>

    /**
     * 프로젝트에 새로운 멤버를 추가합니다.
     * @param projectId 멤버를 추가할 프로젝트의 ID
     * @param userId 추가할 사용자의 ID
     * @param roleId 부여할 역할의 ID
     */
    suspend fun addMember(projectId: String, userId: String, roleId: String): Result<Unit>

    /**
     * 프로젝트 멤버의 역할을 변경합니다.
     * @param projectId 대상 프로젝트의 ID
     * @param userId 역할을 변경할 멤버의 ID
     * @param newRoleId 새로 부여할 역할의 ID
     */
    suspend fun updateMemberRole(projectId: String, userId: String, newRoleId: String): Result<Unit>

    /**
     * 프로젝트에서 멤버를 제거합니다.
     * @param projectId 대상 프로젝트의 ID
     * @param userId 제거할 멤버의 ID
     */
    suspend fun removeMember(projectId: String, userId: String): Result<Unit>
}

