package com.example.domain_usecase.provider.project

import com.example.domain_usecase.usecase.project.authorization.HasProjectPermissionUseCase
import com.example.domain_usecase.usecase.project.authorization.HasProjectPermissionUseCaseImpl
import com.example.domain_usecase.usecase.project.authorization.IsCurrentUserOwnerUseCase
import com.example.domain_usecase.usecase.project.authorization.IsCurrentUserOwnerUseCaseImpl
import com.example.domain_usecase.usecase.project.authorization.OwnerOrPermissionUseCase
import com.example.domain_usecase.usecase.project.authorization.OwnerOrPermissionUseCaseImpl
import com.example.domain_usecase.usecase.project.authorization.PermissionDeniedMessageUseCase
import com.example.domain_usecase.usecase.project.authorization.PermissionDeniedMessageUseCaseImpl
import com.example.domain.vo.CollectionPath
import com.example.domain.vo.DocumentId
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.MemberRepository
import com.example.domain_repository.base.ProjectRoleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectAuthorizationUseCaseProvider @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val projectRoleRepository: ProjectRoleRepository
) {
    fun createForProject(projectId: DocumentId): ProjectAuthorizationUseCases {
        // scope member repo to project members collection
        memberRepository.setCollection(CollectionPath.projectMembers(projectId.value))

        return ProjectAuthorizationUseCases(
            isCurrentUserOwnerUseCase = IsCurrentUserOwnerUseCaseImpl(
                authRepository = authRepository,
                memberRepository = memberRepository,
            ),
            hasProjectPermissionUseCase = HasProjectPermissionUseCaseImpl(
                memberRepository = memberRepository,
                projectRoleRepository = projectRoleRepository,
            ),
            ownerOrPermissionUseCase = OwnerOrPermissionUseCaseImpl(
                authRepository = authRepository,
                memberRepository = memberRepository,
                projectRoleRepository = projectRoleRepository,
            ),
            permissionDeniedMessageUseCase = PermissionDeniedMessageUseCaseImpl()
        )
    }
}

data class ProjectAuthorizationUseCases(
    val isCurrentUserOwnerUseCase: IsCurrentUserOwnerUseCase,
    val hasProjectPermissionUseCase: HasProjectPermissionUseCase,
    val ownerOrPermissionUseCase: OwnerOrPermissionUseCase,
    val permissionDeniedMessageUseCase: PermissionDeniedMessageUseCase,
)

