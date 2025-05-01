package com.example.teamnovapersonalprojectprojectingkotlin.data.repository

import com.example.teamnovapersonalprojectprojectingkotlin.domain.model.ProjectMember
import com.example.teamnovapersonalprojectprojectingkotlin.domain.repository.ProjectMemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import kotlin.Result

class ProjectMemberRepositoryImpl @Inject constructor(
    // TODO: ProjectMemberApiService, ProjectMemberDao 등 주입
) : ProjectMemberRepository {

    override fun getProjectMembersStream(projectId: String): Flow<List<ProjectMember>> {
        println("ProjectMemberRepositoryImpl: getProjectMembersStream called for $projectId (returning empty flow)")
        return flowOf(emptyList())
    }

    override suspend fun fetchProjectMembers(projectId: String): Result<Unit> {
        println("ProjectMemberRepositoryImpl: fetchProjectMembers called for $projectId (returning success)")
        return Result.success(Unit)
    }

    override suspend fun getProjectMember(projectId: String, userId: String): Result<ProjectMember> {
        println("ProjectMemberRepositoryImpl: getProjectMember called for $userId in $projectId (returning failure)")
        return Result.failure(NotImplementedError("구현 필요"))
        // 임시 성공 예시:
        // return Result.success(ProjectMember(userId, "임시 멤버", null, listOf("r1")))
    }

    override suspend fun updateMemberRoles(projectId: String, userId: String, roleIds: List<String>): Result<Unit> {
        println("ProjectMemberRepositoryImpl: updateMemberRoles called for $userId in $projectId (returning success)")
        return Result.success(Unit)
    }
    // TODO: ProjectMemberRepository 인터페이스의 다른 함수들 구현
}