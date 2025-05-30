package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.MemberRemoteDataSource
// import com.example.data.datasource.local.projectmember.ProjectMemberLocalDataSource // 필요시
import com.example.data.model.mapper.toDomain // TODO: 실제 매퍼 경로 및 함수 확인
import com.example.data.model.mapper.toDto // TODO: 실제 매퍼 경로 및 함수 확인
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

    override fun getProjectMembersStream(projectId: String): Flow<CustomResult<List<Member>>> {
        return memberRemoteDataSource.getProjectMembersStream(projectId).map { result ->
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() } // TODO: MemberDto를 Member로 매핑
            }
        }
    }

    override fun getProjectMemberStream(projectId: String, userId: String): Flow<CustomResult<Member>> {
        return memberRemoteDataSource.getProjectMemberStream(projectId, userId).map { result ->
            result.mapCatching { dto ->
                dto.toDomain() // TODO: MemberDto를 Member로 매핑
            }
        }
    }

    override suspend fun fetchProjectMembers(projectId: String): CustomResult<Unit> {
        // TODO: RemoteDataSource에서 멤버 목록을 가져와서 필요시 LocalDataSource에 저장하는 로직
        // 예: val remoteMembers = memberRemoteDataSource.fetchProjectMembers(projectId)
        //     remoteMembers.onSuccess { dtoList ->
        //         projectMemberLocalDataSource.insertMembers(dtoList.map { it.toEntity() })
        //     }
        //     return remoteMembers.map { Unit }
        throw NotImplementedError("fetchProjectMembers 구현 필요")
    }

    override suspend fun addMemberToProject(projectId: String, userId: String, initialRoleIds: List<String>): CustomResult<Unit> {
        // TODO: MemberDto 생성 또는 파라미터 전달 방식에 맞게 memberRemoteDataSource 호출
        return memberRemoteDataSource.addMemberToProject(projectId, userId, initialRoleIds)
    }

    override suspend fun updateProjectMember(projectId: String, member: Member): CustomResult<Unit> {
        // TODO: Member를 MemberDto로 매핑하여 memberRemoteDataSource 호출
        return memberRemoteDataSource.updateProjectMember(projectId, member.toDto())
    }

    override suspend fun removeProjectMember(projectId: String, userId: String): CustomResult<Unit> {
        return memberRemoteDataSource.removeProjectMember(projectId, userId)
    }
}
