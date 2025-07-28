package com.example.domain.provider

import com.example.domain.repository.local.ProjectChannelLocalRepository
import com.example.domain.usecase.local.project.channel.AddProjectChannelLocalUseCase
import com.example.domain.usecase.local.project.channel.AddProjectChannelLocalUseCaseImpl
import com.example.domain.usecase.local.project.channel.CreateProjectChannelLocalUseCase
import com.example.domain.usecase.local.project.channel.CreateProjectChannelLocalUseCaseImpl
import com.example.domain.usecase.local.project.channel.DeleteChannelLocalUseCase
import com.example.domain.usecase.local.project.channel.DeleteChannelLocalUseCaseImpl
import com.example.domain.usecase.local.project.channel.GetCategoryChannelsLocalUseCase
import com.example.domain.usecase.local.project.channel.GetCategoryChannelsLocalUseCaseImpl
import com.example.domain.usecase.local.project.channel.GetProjectChannelLocalUseCase
import com.example.domain.usecase.local.project.channel.GetProjectChannelLocalUseCaseImpl
import com.example.domain.usecase.local.project.channel.RenameChannelLocalUseCase
import com.example.domain.usecase.local.project.channel.RenameChannelLocalUseCaseImpl
import com.example.domain.usecase.local.project.channel.ReorderChannelsLocalUseCase
import com.example.domain.usecase.local.project.channel.ReorderChannelsLocalUseCaseImpl
import com.example.domain.usecase.local.project.channel.UpdateProjectChannelLocalUseCase
import com.example.domain.usecase.local.project.channel.UpdateProjectChannelLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 채널 관리 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 채널 생성, 수정, 삭제, 순서 변경 등의 기능을 담당합니다.
 */
@Singleton
class ProjectChannelUseCaseProvider @Inject constructor(
    private val projectChannelLocalRepository: ProjectChannelLocalRepository
) {

    /**
     * 프로젝트 채널 기본 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 채널 기본 관리 UseCase 그룹
     */
    fun createBasicUseCases(): ProjectChannelLocalBasicUseCases {
        return ProjectChannelLocalBasicUseCases(
            // 채널 생성
            createProjectChannelLocalUseCase = CreateProjectChannelLocalUseCaseImpl(
                projectChannelLocalRepository = projectChannelLocalRepository
            ),
            
            addProjectChannelLocalUseCase = AddProjectChannelLocalUseCaseImpl(
                projectChannelLocalRepository = projectChannelLocalRepository
            ),
            
            // 채널 조회
            getProjectChannelLocalUseCase = GetProjectChannelLocalUseCaseImpl(
                projectChannelLocalRepository = projectChannelLocalRepository
            ),
            
            getCategoryChannelsLocalUseCase = GetCategoryChannelsLocalUseCaseImpl(
                projectChannelLocalRepository = projectChannelLocalRepository
            ),
            
            // 채널 삭제
            deleteChannelLocalUseCase = DeleteChannelLocalUseCaseImpl(
                projectChannelLocalRepository = projectChannelLocalRepository
            ),
            
            projectChannelLocalRepository = projectChannelLocalRepository
        )
    }

    /**
     * 프로젝트 채널 고급 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 채널 고급 관리 UseCase 그룹
     */
    fun createAdvancedUseCases(): ProjectChannelLocalAdvancedUseCases {
        return ProjectChannelLocalAdvancedUseCases(
            // 채널 수정
            updateProjectChannelLocalUseCase = UpdateProjectChannelLocalUseCaseImpl(
                projectChannelLocalRepository = projectChannelLocalRepository
            ),
            
            renameChannelLocalUseCase = RenameChannelLocalUseCaseImpl(
                projectChannelLocalRepository = projectChannelLocalRepository
            ),
            
            // 채널 순서 변경
            reorderChannelsLocalUseCase = ReorderChannelsLocalUseCaseImpl(
                projectChannelLocalRepository = projectChannelLocalRepository
            ),
            
            projectChannelLocalRepository = projectChannelLocalRepository
        )
    }
}

/**
 * 프로젝트 채널 기본 관리 Local UseCase 그룹
 */
data class ProjectChannelLocalBasicUseCases(
    // 채널 생성
    val createProjectChannelLocalUseCase: CreateProjectChannelLocalUseCase,
    val addProjectChannelLocalUseCase: AddProjectChannelLocalUseCase,
    
    // 채널 조회
    val getProjectChannelLocalUseCase: GetProjectChannelLocalUseCase,
    val getCategoryChannelsLocalUseCase: GetCategoryChannelsLocalUseCase,
    
    // 채널 삭제
    val deleteChannelLocalUseCase: DeleteChannelLocalUseCase,
    
    val projectChannelLocalRepository: ProjectChannelLocalRepository
)

/**
 * 프로젝트 채널 고급 관리 Local UseCase 그룹
 */
data class ProjectChannelLocalAdvancedUseCases(
    // 채널 수정
    val updateProjectChannelLocalUseCase: UpdateProjectChannelLocalUseCase,
    val renameChannelLocalUseCase: RenameChannelLocalUseCase,
    
    // 채널 순서 변경
    val reorderChannelsLocalUseCase: ReorderChannelsLocalUseCase,
    
    val projectChannelLocalRepository: ProjectChannelLocalRepository
) 