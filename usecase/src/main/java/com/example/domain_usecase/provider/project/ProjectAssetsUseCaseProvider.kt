package com.example.domain_usecase.provider.project

import android.content.Context
import com.example.domain.vo.CollectionPath
import com.example.domain_repository.base.AuthRepository
import com.example.domain_repository.base.FileRepository
import com.example.domain_repository.base.ProjectRepository
import com.example.domain_usecase.usecase.project.assets.CheckProjectProfileImageExistsUseCase
import com.example.domain_usecase.usecase.project.assets.CheckProjectProfileImageExistsUseCaseImpl
import com.example.domain_usecase.usecase.project.assets.RemoveProjectProfileImageUseCase
import com.example.domain_usecase.usecase.project.assets.RemoveProjectProfileImageUseCaseImpl
import com.example.domain_usecase.usecase.project.assets.UploadProjectProfileImageUseCase
import com.example.domain_usecase.usecase.project.data.ExportProjectUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 자산(이미지, 파일 등) 관리 UseCase들을 제공하는 Provider
 * 
 * 프로젝트 프로필 이미지 업로드, 파일 업로드/다운로드 등의 자산 관리 기능을 담당합니다.
 */
@Singleton
class ProjectAssetsUseCaseProvider @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val fileRepository: FileRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) {

    /**
     * 특정 프로젝트의 자산 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 자산 관리 UseCase 그룹
     */
    fun createForProject(projectId: String): ProjectAssetsUseCases {
        projectRepository.setCollection(CollectionPath.projects)

        return ProjectAssetsUseCases(
            // 프로젝트 이미지/파일 관리
            uploadProjectProfileImageUseCase = UploadProjectProfileImageUseCase(
                projectRepository = this.projectRepository
            ),
            removeProjectProfileImageUseCase = RemoveProjectProfileImageUseCaseImpl(
                projectRepository = this.projectRepository
            ),
            checkProjectProfileImageExistsUseCase = CheckProjectProfileImageExistsUseCaseImpl(
                fileRepository = this.fileRepository
            ),
            // 프로젝트 데이터 내보내기
            exportProjectUseCase = ExportProjectUseCase(
                projectRepository = this.projectRepository,
                authRepository = this.authRepository,
                context = this.context
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
    val removeProjectProfileImageUseCase: RemoveProjectProfileImageUseCase,
    val checkProjectProfileImageExistsUseCase: CheckProjectProfileImageExistsUseCase,
    // 프로젝트 데이터 내보내기
    val exportProjectUseCase: ExportProjectUseCase
)