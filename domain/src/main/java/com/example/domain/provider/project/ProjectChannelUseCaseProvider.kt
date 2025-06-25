package com.example.domain.provider.project

import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.base.ProjectChannelRepository
import com.example.domain.repository.collection.CategoryCollectionRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.CategoryRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectChannelRepositoryFactoryContext
import com.example.domain.usecase.project.channel.AddProjectChannelUseCase
import com.example.domain.usecase.project.channel.AddProjectChannelUseCaseImpl
import com.example.domain.usecase.project.channel.CreateProjectChannelUseCase
import com.example.domain.usecase.project.channel.DeleteChannelUseCase
import com.example.domain.usecase.project.channel.DeleteChannelUseCaseImpl
import com.example.domain.usecase.project.channel.GetProjectChannelUseCase
import com.example.domain.usecase.project.channel.MoveChannelUseCase
import com.example.domain.usecase.project.channel.MoveChannelUseCaseImpl
import com.example.domain.usecase.project.channel.RenameChannelUseCase
import com.example.domain.usecase.project.channel.RenameChannelUseCaseImpl
import com.example.domain.usecase.project.channel.UpdateProjectChannelUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 채널 관리 UseCase들을 제공하는 Provider
 * 
 * 채널 생성, 삭제, 수정, 이동, 이름 변경 등의 채널 전용 관리 기능을 담당합니다.
 */
@Singleton
class ProjectChannelUseCaseProvider @Inject constructor(
    private val categoryRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<CategoryRepositoryFactoryContext, CategoryRepository>,
    private val projectChannelRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<ProjectChannelRepositoryFactoryContext, ProjectChannelRepository>,
    private val authRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>,
    private val categoryCollectionRepository: CategoryCollectionRepository
) {

    /**
     * 특정 프로젝트의 채널 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 채널 관리 UseCase 그룹
     */
    fun createForProject(projectId: DocumentId): ProjectChannelUseCases {
        val categoryRepository = categoryRepositoryFactory.create(
            CategoryRepositoryFactoryContext(
                collectionPath = CollectionPath.projectCategories(projectId.value)
            )
        )
        
        val projectChannelRepository = projectChannelRepositoryFactory.create(
            ProjectChannelRepositoryFactoryContext(
                collectionPath = CollectionPath.projectCategories(projectId.value)
            )
        )

        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        return ProjectChannelUseCases(
            // 채널 기본 CRUD
            createProjectChannelUseCase = CreateProjectChannelUseCase(
                projectChannelRepository = projectChannelRepository,
                authRepository = authRepository
            ),
            
            getProjectChannelUseCase = GetProjectChannelUseCase(
                projectChannelRepository = projectChannelRepository
            ),
            
            updateProjectChannelUseCase = UpdateProjectChannelUseCase(
                categoryRepository = categoryRepository,
                authRepository = authRepository
            ),
            
            deleteChannelUseCase = DeleteChannelUseCaseImpl(
                categoryCollectionRepository = categoryCollectionRepository
            ),
            
            // 채널 고급 관리
            addProjectChannelUseCase = AddProjectChannelUseCaseImpl(
                projectChannelRepository = projectChannelRepository
            ),
            
            renameChannelUseCase = RenameChannelUseCaseImpl(
                categoryCollectionRepository = categoryCollectionRepository
            ),
            
            moveChannelUseCase = MoveChannelUseCaseImpl(),
            
            // 공통 Repository
            authRepository = authRepository,
            projectChannelRepository = projectChannelRepository
        )
    }

    /**
     * 현재 사용자를 위한 프로젝트 채널 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 채널 관리 UseCase 그룹
     */
    fun createForCurrentUser(projectId: DocumentId): ProjectChannelUseCases {
        return createForProject(projectId)
    }

    /**
     * 특정 채널에 대한 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelId 채널 ID
     * @return 채널별 UseCase 그룹
     */
    fun createForChannel(projectId: DocumentId, channelId: String): ProjectChannelUseCases {
        return createForProject(projectId)
    }
}

/**
 * 프로젝트 채널 관리 UseCase 그룹
 */
data class ProjectChannelUseCases(
    // 채널 기본 CRUD
    val createProjectChannelUseCase: CreateProjectChannelUseCase,
    val getProjectChannelUseCase: GetProjectChannelUseCase,
    val updateProjectChannelUseCase: UpdateProjectChannelUseCase,
    val deleteChannelUseCase: DeleteChannelUseCase,
    
    // 채널 고급 관리
    val addProjectChannelUseCase: AddProjectChannelUseCase,
    val renameChannelUseCase: RenameChannelUseCase,
    val moveChannelUseCase: MoveChannelUseCase,
    
    // 공통 Repository
    val authRepository: AuthRepository,
    val projectChannelRepository: ProjectChannelRepository
)