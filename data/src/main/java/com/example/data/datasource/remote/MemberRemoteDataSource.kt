
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.model.remote.MemberDTO
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
    suspend fun getProjectMember(projectId: String, userId: String): CustomResult<MemberDTO?, Exception>

    /**
     * 프로젝트에 새로운 멤버를 추가합니다.
     * Firestore Class Diagram의 Members 엔티티에는 joinedAt 필드만 명시되어 있지만,
     * 일반적으로 멤버를 추가할 때 역할을 지정하므로 roleId 파라미터를 추가합니다.
     * @param projectId 멤버를 추가할 프로젝트의 ID
     * @param userId 추가할 사용자의 ID (이것이 members 서브컬렉션의 문서 ID가 됨)
     * @param roleIds 부여할 역할의 ID
     */
    suspend fun addMember(projectId: String, userId: String, roleIds: List<String>): CustomResult<Unit, Exception>

    /**
     * 프로젝트 멤버의 역할을 변경합니다.
     * @param projectId 대상 프로젝트의 ID
     * @param userId 역할을 변경할 멤버의 ID
     * @param newRoleId 새로 부여할 역할의 ID
     */
    suspend fun updateMemberRole(projectId: String, userId: String, newRoleIds: List<String>): CustomResult<Unit, Exception>

    /**
     * 프로젝트에서 멤버를 제거합니다.
     * @param projectId 대상 프로젝트의 ID
     * @param userId 제거할 멤버의 ID
     */
    suspend fun removeMember(projectId: String, userId: String): CustomResult<Unit, Exception>
}

