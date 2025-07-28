package com.example.domain.provider.project

import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId

import com.example.domain.repository.remote.AuthRepository
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
import com.example.domain.usecase.project.channel.ReorderChannelsUseCase
import com.example.domain.usecase.project.channel.ReorderChannelsUseCaseImpl
import com.example.domain.usecase.project.channel.UpdateProjectChannelUseCase
import com.example.domain.usecase.project.channel.UpdateProjectChannelUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton
import com.example.domain.repository.remote.DefaultRepository
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel

/**
 * 프로젝트 채널 관리 UseCase들을 제공하는 Provider
 * 
 * 채널 생성, 삭제, 수정, 이동, 이름 변경 등의 채널 전용 관리 기능을 담당합니다.
 */
@Singleton
class ProjectChannelUseCaseProvider @Inject constructor(
    private val categoryRepository: DefaultRepository<Category>,
    private val projectChannelRepository: DefaultRepository<ProjectChannel>,
    private val authRepository: AuthRepository
) {

    /**
     * 특정 프로젝트의 채널 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 채널 관리 UseCase 그룹
     */
    fun createForProject(projectId: DocumentId): ProjectChannelUseCases {
        // repository 생성은 viewmodel 에서 해야함
        // 정확히는 provider 에서 해야함 provider를 viemodel 에서 주입받고
        categoryRepository.setCollection(CollectionPath.projectCategories(projectId.value))
        projectChannelRepository.setCollection(CollectionPath.projectChannels(projectId.value))

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
            
            updateProjectChannelUseCase = UpdateProjectChannelUseCaseImpl(
                projectChannelRepository = projectChannelRepository
            ),
            
            deleteChannelUseCase = DeleteChannelUseCaseImpl(
                projectChannelRepository = projectChannelRepository
            ),
            
            // 채널 고급 관리
            addProjectChannelUseCase = AddProjectChannelUseCaseImpl(
                projectChannelRepository = projectChannelRepository
            ),
            
            reorderChannelsUseCase = ReorderChannelsUseCaseImpl(
                projectChannelRepository = projectChannelRepository
            ),
            
            categoryRepository= categoryRepository,
            projectChannelRepository= projectChannelRepository,
            authRepository= authRepository
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
    val getCategoryChannelsUseCase: GetCategoryChannelsUseCase,
    val updateProjectChannelUseCase: UpdateProjectChannelUseCase,
    val deleteChannelUseCase: DeleteChannelUseCase,
    
    // 채널 고급 관리
    val addProjectChannelUseCase: AddProjectChannelUseCase,
    val reorderChannelsUseCase: ReorderChannelsUseCase,
    // TODO: CategoryCollectionRepository 제거로 인해 임시 비활성화
    // val renameChannelUseCase: RenameChannelUseCase
    val categoryRepository: DefaultRepository<Category>,
    val projectChannelRepository: DefaultRepository<ProjectChannel>,
    val authRepository: AuthRepository
)