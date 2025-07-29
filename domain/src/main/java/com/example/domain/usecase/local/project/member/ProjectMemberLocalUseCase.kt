package com.example.domain.usecase.local.project.member

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectMember
import com.example.domain.model.vo.DocumentId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface AddProjectMemberLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        userId: String
    ): CustomResult<Unit, Exception>
}

interface RemoveProjectMemberLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        userId: String
    ): CustomResult<Unit, Exception>
}

interface ObserveProjectMembersLocalUseCase {
    operator fun invoke(projectId: DocumentId): Flow<CustomResult<List<ProjectMember>, Exception>>
}

interface GetProjectMemberLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        userId: String
    ): CustomResult<ProjectMember, Exception>
}

class AddProjectMemberLocalUseCaseImpl @Inject constructor(
    private val projectMemberLocalRepository: ProjectMemberLocalRepository
) : AddProjectMemberLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        userId: String
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 프로젝트에 새 멤버 추가")
    }
}

class RemoveProjectMemberLocalUseCaseImpl @Inject constructor(
    private val projectMemberLocalRepository: ProjectMemberLocalRepository
) : RemoveProjectMemberLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        userId: String
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 프로젝트 멤버 제거")
    }
}

class ObserveProjectMembersLocalUseCaseImpl @Inject constructor(
    private val projectMemberLocalRepository: ProjectMemberLocalRepository
) : ObserveProjectMembersLocalUseCase {

    override operator fun invoke(projectId: DocumentId): Flow<CustomResult<List<ProjectMember>, Exception>> {
        return TODO("로컬 저장소에서 프로젝트 멤버 목록을 실시간으로 관찰")
    }
}

class GetProjectMemberLocalUseCaseImpl @Inject constructor(
    private val projectMemberLocalRepository: ProjectMemberLocalRepository
) : GetProjectMemberLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        userId: String
    ): CustomResult<ProjectMember, Exception> {
        return TODO("로컬 저장소에서 특정 프로젝트 멤버 정보 조회")
    }
} 