package com.example.domain.provider

import com.example.domain.repository.local.LocalProjectRepository
import com.example.domain.repository.remote.AuthRepository
import com.example.domain.repository.remote.FileRepository
import com.example.domain.usecase.local.project.assets.RemoveProjectProfileImageLocalUseCase
import com.example.domain.usecase.local.project.assets.RemoveProjectProfileImageLocalUseCaseImpl
import com.example.domain.usecase.local.project.assets.UploadProjectProfileImageLocalUseCase
import com.example.domain.usecase.local.project.assets.UploadProjectProfileImageLocalUseCaseImpl
import com.example.domain.usecase.local.project.core.CreateProjectLocalUseCase
import com.example.domain.usecase.local.project.core.CreateProjectLocalUseCaseImpl
import com.example.domain.usecase.local.project.core.GetProjectDetailsLocalUseCase
import com.example.domain.usecase.local.project.core.GetProjectDetailsLocalUseCaseImpl
import com.example.domain.usecase.local.project.core.GetProjectDetailsStreamLocalUseCase
import com.example.domain.usecase.local.project.core.GetProjectDetailsStreamLocalUseCaseImpl
import com.example.domain.usecase.local.projects.DeleteProjectUseCase
import com.example.domain.usecase.local.projects.DeleteProjectUseCaseImpl
import com.example.domain.usecase.local.projects.GetAllProjectsUseCase
import com.example.domain.usecase.local.projects.GetAllProjectsUseCaseImpl
import com.example.domain.usecase.local.projects.GetProjectUseCase
import com.example.domain.usecase.local.projects.GetProjectUseCaseImpl
import com.example.domain.usecase.local.projects.GetProjectsByUserUseCase
import com.example.domain.usecase.local.projects.GetProjectsByUserUseCaseImpl
import com.example.domain.usecase.local.projects.InsertProjectUseCase
import com.example.domain.usecase.local.projects.InsertProjectUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 관리 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 프로젝트 생성, 조회, 수정, 삭제 및 자산 관리 기능을 담당합니다.
 */
@Singleton
class ProjectUseCaseProvider @Inject constructor(
    private val projectLocalRepository: LocalProjectRepository,
    private val fileLocalRepository: FileRepository,
    private val authRepository: AuthRepository
) {

    /**
     * 프로젝트 기본 CRUD 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 기본 관리 UseCase 그룹
     */
    fun createBasicUseCases(): ProjectLocalBasicUseCases {
        return ProjectLocalBasicUseCases(
            // 프로젝트 조회
            getAllProjectsUseCase = GetAllProjectsUseCaseImpl(
                projectLocalRepository = projectLocalRepository
            ),
            
            getProjectUseCase = GetProjectUseCaseImpl(
                projectLocalRepository = projectLocalRepository
            ),
            
            getProjectsByUserUseCase = GetProjectsByUserUseCaseImpl(
                projectLocalRepository = projectLocalRepository
            ),
            
            // 프로젝트 생성/수정/삭제
            insertProjectUseCase = InsertProjectUseCaseImpl(
                projectLocalRepository = projectLocalRepository
            ),
            
            deleteProjectUseCase = DeleteProjectUseCaseImpl(
                projectLocalRepository = projectLocalRepository
            ),
            
            projectLocalRepository = projectLocalRepository
        )
    }

    /**
     * 프로젝트 상세 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 상세 관리 UseCase 그룹
     */
    fun createDetailUseCases(): ProjectLocalDetailUseCases {
        return ProjectLocalDetailUseCases(
            // 프로젝트 생성
            createProjectLocalUseCase = CreateProjectLocalUseCaseImpl(
                projectLocalRepository = projectLocalRepository,
                authRepository = authRepository
            ),
            
            // 프로젝트 상세 조회
            getProjectDetailsLocalUseCase = GetProjectDetailsLocalUseCaseImpl(
                projectLocalRepository = projectLocalRepository
            ),
            
            getProjectDetailsStreamLocalUseCase = GetProjectDetailsStreamLocalUseCaseImpl(
                projectLocalRepository = projectLocalRepository
            ),
            
            projectLocalRepository = projectLocalRepository,
            authRepository = authRepository
        )
    }

    /**
     * 프로젝트 자산(이미지, 파일) 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 자산 관리 UseCase 그룹
     */
    fun createAssetUseCases(): ProjectLocalAssetUseCases {
        return ProjectLocalAssetUseCases(
            // 프로젝트 프로필 이미지 업로드
            uploadProjectProfileImageLocalUseCase = UploadProjectProfileImageLocalUseCaseImpl(
                projectLocalRepository = projectLocalRepository,
                fileLocalRepository = fileLocalRepository
            ),
            
            // 프로젝트 프로필 이미지 제거
            removeProjectProfileImageLocalUseCase = RemoveProjectProfileImageLocalUseCaseImpl(
                projectLocalRepository = projectLocalRepository,
                fileLocalRepository = fileLocalRepository
            ),
            
            projectLocalRepository = projectLocalRepository,
            fileLocalRepository = fileLocalRepository
        )
    }
}

/**
 * 프로젝트 기본 관리 Local UseCase 그룹
 */
data class ProjectLocalBasicUseCases(
    // 프로젝트 조회
    val getAllProjectsUseCase: GetAllProjectsUseCase,
    val getProjectUseCase: GetProjectUseCase,
    val getProjectsByUserUseCase: GetProjectsByUserUseCase,
    
    // 프로젝트 생성/수정/삭제
    val insertProjectUseCase: InsertProjectUseCase,
    val deleteProjectUseCase: DeleteProjectUseCase,

    val projectLocalRepository: LocalProjectRepository
)

/**
 * 프로젝트 상세 관리 Local UseCase 그룹
 */
data class ProjectLocalDetailUseCases(
    // 프로젝트 생성
    val createProjectLocalUseCase: CreateProjectLocalUseCase,
    
    // 프로젝트 상세 조회
    val getProjectDetailsLocalUseCase: GetProjectDetailsLocalUseCase,
    val getProjectDetailsStreamLocalUseCase: GetProjectDetailsStreamLocalUseCase,

    val projectLocalRepository: LocalProjectRepository,
    val authRepository: AuthRepository
)

/**
 * 프로젝트 자산 관리 Local UseCase 그룹
 */
data class ProjectLocalAssetUseCases(
    // 프로젝트 프로필 이미지 업로드
    val uploadProjectProfileImageLocalUseCase: UploadProjectProfileImageLocalUseCase,
    
    // 프로젝트 프로필 이미지 제거
    val removeProjectProfileImageLocalUseCase: RemoveProjectProfileImageLocalUseCase,

    val projectLocalRepository: LocalProjectRepository,
    val fileLocalRepository: FileRepository
) 