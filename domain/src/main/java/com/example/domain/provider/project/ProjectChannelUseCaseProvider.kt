package com.example.domain.provider.project

import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.base.ProjectChannelRepository

import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.CategoryRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectChannelRepositoryFactoryContext
import com.example.domain.usecase.project.channel.AddProjectChannelUseCase
import com.example.domain.usecase.project.channel.AddProjectChannelUseCaseImpl
import com.example.domain.usecase.project.channel.CreateProjectChannelUseCase
import com.example.domain.usecase.project.channel.DeleteChannelUseCase
import com.example.domain.usecase.project.channel.DeleteChannelUseCaseImpl
import com.example.domain.usecase.project.channel.GetCategoryChannelsUseCase
import com.example.domain.usecase.project.channel.GetCategoryChannelsUseCaseImpl
import com.example.domain.usecase.project.channel.GetProjectChannelUseCase
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
    private val authRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * 특정 프로젝트의 채널 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @return 프로젝트 채널 관리 UseCase 그룹
     */
    fun createForProject(projectId: DocumentId, categoryId: DocumentId): ProjectChannelUseCases {
        // repository 생성은 viewmodel 에서 해야함
        // 정확히는 provider 에서 해야함 provider를 viemodel 에서 주입받고
        val categoryRepository = categoryRepositoryFactory.create(
            CategoryRepositoryFactoryContext(
                collectionPath = CollectionPath.projectCategories(projectId.value)
            )
        )
        
        val projectChannelRepository = projectChannelRepositoryFactory.create(
            ProjectChannelRepositoryFactoryContext(
                collectionPath = CollectionPath.projectChannels(projectId.value)
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
            
            getCategoryChannelsUseCase = GetCategoryChannelsUseCaseImpl(
                projectChannelRepository = projectChannelRepository
            ),
            
            updateProjectChannelUseCase = UpdateProjectChannelUseCase(
                categoryRepository = categoryRepository,
                authRepository = authRepository
            ),
            
            // TODO: CategoryCollectionRepository 제거로 인해 임시 비활성화
            // deleteChannelUseCase = DeleteChannelUseCaseImpl(),
            
            // 채널 고급 관리
            addProjectChannelUseCase = AddProjectChannelUseCaseImpl(
                projectChannelRepository = projectChannelRepository
            ),
            
            // TODO: CategoryCollectionRepository 제거로 인해 임시 비활성화
            // renameChannelUseCase = RenameChannelUseCaseImpl(),
            

            // 공통 Repository
            authRepository = authRepository,
            projectChannelRepository = projectChannelRepository
        )
    }

    /**
     * 현재 사용자를 위한 프로젝트 채널 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @return 프로젝트 채널 관리 UseCase 그룹
     */
    fun createForCurrentUser(projectId: DocumentId, categoryId: DocumentId): ProjectChannelUseCases {
        return createForProject(projectId, categoryId)
    }

    /**
     * 특정 채널에 대한 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 카테고리 ID
     * @param channelId 채널 ID
     * @return 채널별 UseCase 그룹
     */
    fun createForChannel(projectId: DocumentId, categoryId: DocumentId, channelId: String): ProjectChannelUseCases {
        return createForProject(projectId, categoryId)
    }
}

/**
 * 프로젝트 채널 관리 UseCase 그룹
 */
data class ProjectChannelUseCases(
    // 채널 기본 CRUD
    val createProjectChannelUseCase: CreateProjectChannelUseCase,
    val getProjectChannelUseCase: GetProjectChannelUseCase,
    val getCategoryChannelsUseCase: GetCategoryChannelsUseCase,
    val updateProjectChannelUseCase: UpdateProjectChannelUseCase,
    // TODO: CategoryCollectionRepository 제거로 인해 임시 비활성화
    // val deleteChannelUseCase: DeleteChannelUseCase,
    
    // 채널 고급 관리
    val addProjectChannelUseCase: AddProjectChannelUseCase,
    // TODO: CategoryCollectionRepository 제거로 인해 임시 비활성화
    // val renameChannelUseCase: RenameChannelUseCase,

    // 공통 Repository
    val authRepository: AuthRepository,
    val projectChannelRepository: ProjectChannelRepository
)