package com.example.domain.provider.project

import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.MediaRepository
import com.example.domain.repository.base.ProjectRepository
import com.example.domain.repository.factory.context.MediaRepositoryFactoryContext
import com.example.domain.repository.factory.context.ProjectRepositoryFactoryContext
import com.example.domain.usecase.project.assets.UploadProjectProfileImageUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 자산(이미지, 파일 등) 관리 UseCase들을 제공하는 Provider
 * 
 * 프로젝트 프로필 이미지 업로드, 파일 업로드/다운로드 등의 자산 관리 기능을 담당합니다.
 */
@Singleton
class ProjectAssetsUseCaseProvider @Inject constructor(
    private val projectRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<ProjectRepositoryFactoryContext, ProjectRepository>,
    private val mediaRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<MediaRepositoryFactoryContext, MediaRepository>
) {

    /**
     * 특정 프로젝트의 자산 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 자산 관리 UseCase 그룹
     */
    fun createForProject(projectId: String): ProjectAssetsUseCases {
        val projectRepository = projectRepositoryFactory.create(
            ProjectRepositoryFactoryContext(
                collectionPath = CollectionPath.projects
            )
        )

        val mediaRepository = mediaRepositoryFactory.create(
            MediaRepositoryFactoryContext()
        )

        return ProjectAssetsUseCases(
            // 프로젝트 이미지/파일 관리
            uploadProjectProfileImageUseCase = UploadProjectProfileImageUseCase(
                projectRepository = projectRepository,
                mediaRepository = mediaRepository
            ),
            
            // 공통 Repository
            projectRepository = projectRepository,
            mediaRepository = mediaRepository
        )
    }

    /**
     * 현재 사용자를 위한 프로젝트 자산 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 자산 관리 UseCase 그룹
     */
    fun createForCurrentUser(projectId: String): ProjectAssetsUseCases {
        return createForProject(projectId)
    }
}

/**
 * 프로젝트 자산 관리 UseCase 그룹
 */
data class ProjectAssetsUseCases(
    // 프로젝트 이미지/파일 관리
    val uploadProjectProfileImageUseCase: UploadProjectProfileImageUseCase,
    
    // 공통 Repository
    val projectRepository: ProjectRepository,
    val mediaRepository: MediaRepository
)