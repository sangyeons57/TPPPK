package com.example.domain_usecase.provider.project

import com.example.domain.vo.CollectionPath
import com.example.domain_repository.RepositoryFactory
import com.example.domain_repository.base.MediaRepository
import com.example.domain_repository.base.ProjectRepository
import com.example.domain_repository.context.MediaRepositoryFactoryContext
import com.example.domain_repository.context.ProjectRepositoryFactoryContext
import com.example.domain_usecase.usecase.project.assets.RemoveProjectProfileImageUseCase
import com.example.domain_usecase.usecase.project.assets.RemoveProjectProfileImageUseCaseImpl
import com.example.domain_usecase.usecase.project.assets.UploadProjectProfileImageUseCase
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
    private val mediaRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<MediaRepositoryFactoryContext, MediaRepository>,
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

        mediaRepositoryFactory.create(
            MediaRepositoryFactoryContext()
        )

        return ProjectAssetsUseCases(
            // 프로젝트 이미지/파일 관리
            uploadProjectProfileImageUseCase = UploadProjectProfileImageUseCase(
                projectRepository = projectRepository
            ),
            removeProjectProfileImageUseCase = RemoveProjectProfileImageUseCaseImpl(
                projectRepository = projectRepository
            )
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
    val removeProjectProfileImageUseCase: RemoveProjectProfileImageUseCase
)