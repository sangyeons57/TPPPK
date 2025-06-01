package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.MemberRemoteDataSource
// import com.example.data.datasource.local.projectmember.ProjectMemberLocalDataSource // 필요시
import com.example.data.model.remote.toDto
import com.example.domain.model.base.Member
import com.example.domain.repository.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MemberRepositoryImpl @Inject constructor(
    private val memberRemoteDataSource: MemberRemoteDataSource
    // private val projectMemberLocalDataSource: ProjectMemberLocalDataSource, // 예시: 로컬 캐싱/조회시
    // TODO: 필요한 Mapper 주입
) : MemberRepository {

    override fun getProjectMembersStream(projectId: String): Flow<CustomResult<List<Member>, Exception>> {
        // Using observeMembers from the data source, which returns Flow<List<MemberDTO>>
        // Wrap it in CustomResult and map to domain models
        return memberRemoteDataSource.observeMembers(projectId)
            .map { dtoList -> 
                CustomResult.Success(dtoList.map { it.toDomain() })
            }
    }

    override fun getProjectMemberStream(projectId: String, userId: String): Flow<CustomResult<Member, Exception>> {
        // Since there's no direct stream method for a single member in the data source,
        // we'll filter from the members stream
        return memberRemoteDataSource.observeMembers(projectId)
            .map { membersList ->
                val member = membersList.find { it.userId == userId }
                if (member != null) {
                    CustomResult.Success(member.toDomain())
                } else {
                    CustomResult.Failure(Exception("Member not found"))
                }
            }
    }

    override suspend fun fetchProjectMembers(projectId: String): CustomResult<Unit, Exception> {
        // There's no direct refresh method in the remote data source
        // In a real implementation, we might want to force a refresh of the Firestore cache
        // For now, just return success since Firestore will automatically refresh data
        return CustomResult.Success(Unit)
    }

    override suspend fun addMemberToProject(projectId: String, userId: String, initialRoleIds: List<String>): CustomResult<Unit, Exception> {
        // Use the addMember method that takes a List<String> for roleIds
        return memberRemoteDataSource.addMember(projectId, userId, initialRoleIds)
    }

    override suspend fun updateProjectMember(projectId: String, member: Member): CustomResult<Unit, Exception> {
        // The data source only has updateMemberRole, so we'll use that
        // In a real implementation, we might need more comprehensive updates
        return memberRemoteDataSource.updateMemberRole(projectId, member.userId, member.roleIds)
    }

    override suspend fun removeProjectMember(projectId: String, userId: String): CustomResult<Unit, Exception> {
        return memberRemoteDataSource.removeMember(projectId, userId)
    }
}
