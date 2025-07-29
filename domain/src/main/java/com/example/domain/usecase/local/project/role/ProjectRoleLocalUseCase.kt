package com.example.domain.usecase.local.project.role

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectRole
import com.example.domain.model.enum.Permission
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.local.ProjectRoleLocalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface CreateProjectRoleLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        roleName: String,
        permissions: Set<Permission>
    ): CustomResult<ProjectRole, Exception>
}

interface CreateRoleLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        roleName: String,
        permissions: Set<Permission> = emptySet()
    ): CustomResult<ProjectRole, Exception>
}

interface DeleteRoleLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        roleId: DocumentId
    ): CustomResult<Unit, Exception>
}

interface GetProjectRolesLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId
    ): CustomResult<List<ProjectRole>, Exception>
}

interface GetProjectRoleLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        roleId: DocumentId
    ): CustomResult<ProjectRole, Exception>
}

interface GetRoleDetailsLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        roleId: DocumentId
    ): CustomResult<ProjectRole, Exception>
}

interface GetRolePermissionsLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        roleId: DocumentId
    ): CustomResult<Set<Permission>, Exception>
}

interface UpdateProjectRoleLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        role: ProjectRole
    ): CustomResult<ProjectRole, Exception>
}

interface UpdateMemberRolesLocalUseCase {
    suspend operator fun invoke(
        projectId: DocumentId,
        memberId: DocumentId,
        roleIds: Set<DocumentId>
    ): CustomResult<Unit, Exception>
}

class CreateProjectRoleLocalUseCaseImpl @Inject constructor(
    private val projectRoleLocalRepository: ProjectRoleLocalRepository
) : CreateProjectRoleLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        roleName: String,
        permissions: Set<Permission>
    ): CustomResult<ProjectRole, Exception> {
        return TODO("로컬 저장소에 새 프로젝트 역할 생성")
    }
}

class CreateRoleLocalUseCaseImpl @Inject constructor(
    private val projectRoleLocalRepository: ProjectRoleLocalRepository
) : CreateRoleLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        roleName: String,
        permissions: Set<Permission>
    ): CustomResult<ProjectRole, Exception> {
        return TODO("로컬 저장소에 새 역할 생성")
    }
}

class DeleteRoleLocalUseCaseImpl @Inject constructor(
    private val projectRoleLocalRepository: ProjectRoleLocalRepository
) : DeleteRoleLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        roleId: DocumentId
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 역할 삭제")
    }
}

class GetProjectRolesLocalUseCaseImpl @Inject constructor(
    private val projectRoleLocalRepository: ProjectRoleLocalRepository
) : GetProjectRolesLocalUseCase {

    override suspend operator fun invoke(projectId: DocumentId): CustomResult<List<ProjectRole>, Exception> {
        return TODO("로컬 저장소에서 프로젝트 역할 목록 조회")
    }
}

class GetProjectRoleLocalUseCaseImpl @Inject constructor(
    private val projectRoleLocalRepository: ProjectRoleLocalRepository
) : GetProjectRoleLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        roleId: DocumentId
    ): CustomResult<ProjectRole, Exception> {
        return TODO("로컬 저장소에서 특정 프로젝트 역할 정보 조회")
    }
}

class GetRoleDetailsLocalUseCaseImpl @Inject constructor(
    private val projectRoleLocalRepository: ProjectRoleLocalRepository
) : GetRoleDetailsLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        roleId: DocumentId
    ): CustomResult<ProjectRole, Exception> {
        return TODO("로컬 저장소에서 역할 상세 정보 조회")
    }
}

class GetRolePermissionsLocalUseCaseImpl @Inject constructor(
    private val projectRoleLocalRepository: ProjectRoleLocalRepository
) : GetRolePermissionsLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        roleId: DocumentId
    ): CustomResult<Set<Permission>, Exception> {
        return TODO("로컬 저장소에서 역할의 권한 목록 조회")
    }
}

class UpdateProjectRoleLocalUseCaseImpl @Inject constructor(
    private val projectRoleLocalRepository: ProjectRoleLocalRepository
) : UpdateProjectRoleLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        role: ProjectRole
    ): CustomResult<ProjectRole, Exception> {
        return TODO("로컬 저장소에서 프로젝트 역할 정보 업데이트")
    }
}

class UpdateMemberRolesLocalUseCaseImpl @Inject constructor(
    private val projectRoleLocalRepository: ProjectRoleLocalRepository
) : UpdateMemberRolesLocalUseCase {

    override suspend operator fun invoke(
        projectId: DocumentId,
        memberId: DocumentId,
        roleIds: Set<DocumentId>
    ): CustomResult<Unit, Exception> {
        return TODO("로컬 저장소에서 멤버의 역할 목록 업데이트")
    }
} 