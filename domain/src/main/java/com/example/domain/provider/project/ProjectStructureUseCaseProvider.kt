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
import com.example.domain.usecase.project.category.GetCategoryDetailsUseCase
import com.example.domain.usecase.project.category.GetCategoryDetailsUseCaseImpl
import com.example.domain.usecase.project.category.ReorderCategoriesUseCase
import com.example.domain.usecase.project.category.ReorderCategoriesUseCaseImpl
import com.example.domain.usecase.project.category.UpdateCategoryUseCase
import com.example.domain.usecase.project.category.UpdateCategoryUseCaseImpl
import com.example.domain.usecase.project.structure.AddCategoryUseCase
import com.example.domain.usecase.project.structure.AddCategoryUseCaseImpl
import com.example.domain.usecase.project.structure.DeleteCategoryUseCase
import com.example.domain.usecase.project.structure.DeleteCategoryUseCaseImpl
import com.example.domain.usecase.project.structure.GetProjectAllCategoriesUseCase
import com.example.domain.usecase.project.structure.GetProjectAllCategoriesUseCaseImpl
import com.example.domain.usecase.project.structure.GetProjectStructureUseCase
import com.example.domain.usecase.project.structure.GetProjectStructureUseCaseImpl
import com.example.domain.usecase.project.structure.RenameCategoryUseCase
import com.example.domain.usecase.project.structure.RenameCategoryUseCaseImpl
import com.example.domain.usecase.project.structure.ReorderUnifiedProjectStructureUseCase
import com.example.domain.usecase.project.structure.ReorderUnifiedProjectStructureUseCaseImpl
import com.example.domain.usecase.project.channel.ReorderChannelsUseCase
import com.example.domain.usecase.project.channel.ReorderChannelsUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 구조 관리 UseCase들을 제공하는 Provider
 * 
 * 카테고리 생성/삭제/이동, 채널-카테고리 간 이동, 구조 변환 등의 기능을 담당합니다.
 */
@Singleton
class ProjectStructureUseCaseProvider @Inject constructor(
    private val categoryRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<CategoryRepositoryFactoryContext, CategoryRepository>,
    private val projectChannelRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<ProjectChannelRepositoryFactoryContext, ProjectChannelRepository>,
    private val authRepositoryFactory: @JvmSuppressWildcards RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * 특정 프로젝트의 구조 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 관리 UseCase 그룹
     */
    fun createForProject(projectId: DocumentId): ProjectStructureUseCases {
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

        return ProjectStructureUseCases(
            // 카테고리 관리
            addCategoryUseCase = AddCategoryUseCaseImpl(
                categoryRepository = categoryRepository,
                authRepository = authRepository
            ),
            
            deleteCategoryUseCase = DeleteCategoryUseCaseImpl(
                categoryRepository = categoryRepository,
                projectChannelRepository = projectChannelRepository
            ),
            
            renameCategoryUseCase = RenameCategoryUseCaseImpl(
                categoryRepository = categoryRepository
            ),
            

            // 프로젝트 구조 조회 및 변환
            getProjectAllCategoriesUseCase = GetProjectAllCategoriesUseCaseImpl(
                categoryRepository = categoryRepository
            ),
            
            getProjectStructureUseCase = GetProjectStructureUseCaseImpl(
                categoryRepository = categoryRepository,
                projectChannelRepository = projectChannelRepository
            ),
            

            // TODO: CategoryCollectionRepository 제거로 인해 임시 비활성화
            // moveChannelBetweenCategoriesUseCase = MoveChannelBetweenCategoriesUseCaseImpl(),
            
            // 카테고리 도메인 UseCases
            getCategoryDetailsUseCase = GetCategoryDetailsUseCaseImpl(
                categoryRepository = categoryRepository
            ),
            
            updateCategoryUseCase = UpdateCategoryUseCaseImpl(
                categoryRepository = categoryRepository
            ),
            
            reorderCategoriesUseCase = ReorderCategoriesUseCaseImpl(
                categoryRepository = categoryRepository
            ),
            
            reorderChannelsUseCase = ReorderChannelsUseCaseImpl(
                projectChannelRepository = projectChannelRepository
            ),
            
            reorderUnifiedProjectStructureUseCase = ReorderUnifiedProjectStructureUseCaseImpl(
                categoryRepository = categoryRepository,
                projectChannelRepository = projectChannelRepository
            ),
            
            // 공통 Repository
            authRepository = authRepository,
            categoryRepository = categoryRepository
        )
    }

    /**
     * 현재 사용자를 위한 프로젝트 구조 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 관리 UseCase 그룹
     */
    fun createForCurrentUser(projectId: DocumentId): ProjectStructureUseCases {
        return createForProject(projectId)
    }
}

/**
 * 프로젝트 구조 관리 UseCase 그룹
 */
data class ProjectStructureUseCases(
    // 카테고리 관리
    val addCategoryUseCase: AddCategoryUseCase,
    val deleteCategoryUseCase: DeleteCategoryUseCase,
    val renameCategoryUseCase: RenameCategoryUseCase,

    // 프로젝트 구조 조회 및 변환
    val getProjectAllCategoriesUseCase: GetProjectAllCategoriesUseCase,
    val getProjectStructureUseCase: GetProjectStructureUseCase,

    // 구조 업데이트

    // 카테고리 도메인 UseCases
    val getCategoryDetailsUseCase: GetCategoryDetailsUseCase,
    val updateCategoryUseCase: UpdateCategoryUseCase,
    val reorderCategoriesUseCase: ReorderCategoriesUseCase,
    
    // 채널 도메인 UseCases
    val reorderChannelsUseCase: ReorderChannelsUseCase,
    
    // 통합 구조 관리 UseCases
    val reorderUnifiedProjectStructureUseCase: ReorderUnifiedProjectStructureUseCase,
    
    // 공통 Repository
    val authRepository: AuthRepository,
    val categoryRepository: CategoryRepository
)