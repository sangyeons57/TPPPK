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
import com.example.domain.usecase.project.category.GetCategoryDetailsUseCase
import com.example.domain.usecase.project.category.GetCategoryDetailsUseCaseImpl
import com.example.domain.usecase.project.category.UpdateCategoryUseCase
import com.example.domain.usecase.project.category.UpdateCategoryUseCaseImpl
import com.example.domain.usecase.project.structure.AddCategoryUseCase
import com.example.domain.usecase.project.structure.AddCategoryUseCaseImpl
import com.example.domain.usecase.project.structure.ConvertProjectStructureToDraggableItemsUseCase
import com.example.domain.usecase.project.structure.ConvertProjectStructureToDraggableItemsUseCaseImpl
import com.example.domain.usecase.project.structure.DeleteCategoryUseCase
import com.example.domain.usecase.project.structure.DeleteCategoryUseCaseImpl
import com.example.domain.usecase.project.structure.GetProjectAllCategoriesUseCase
import com.example.domain.usecase.project.structure.GetProjectAllCategoriesUseCaseImpl
import com.example.domain.usecase.project.structure.MoveCategoryUseCase
import com.example.domain.usecase.project.structure.MoveCategoryUseCaseImpl
import com.example.domain.usecase.project.structure.MoveChannelBetweenCategoriesUseCase
import com.example.domain.usecase.project.structure.MoveChannelBetweenCategoriesUseCaseImpl
import com.example.domain.usecase.project.structure.RenameCategoryUseCase
import com.example.domain.usecase.project.structure.RenameCategoryUseCaseImpl
import com.example.domain.usecase.project.structure.UpdateProjectStructureUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 구조 관리 UseCase들을 제공하는 Provider
 * 
 * 카테고리 생성/삭제/이동, 채널-카테고리 간 이동, 구조 변환 등의 기능을 담당합니다.
 */
@Singleton
class ProjectStructureUseCaseProvider @Inject constructor(
    private val categoryRepositoryFactory: RepositoryFactory<CategoryRepositoryFactoryContext, CategoryRepository>,
    private val projectChannelRepositoryFactory: RepositoryFactory<ProjectChannelRepositoryFactoryContext, ProjectChannelRepository>,
    private val authRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>,
    private val categoryCollectionRepository: CategoryCollectionRepository
) {

    /**
     * 특정 프로젝트의 구조 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 관리 UseCase 그룹
     */
    fun createForProject(projectId: DocumentId): ProjectStructureUseCases {
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

        return ProjectStructureUseCases(
            // 카테고리 관리
            addCategoryUseCase = AddCategoryUseCaseImpl(
                categoryRepository = categoryRepository,
                authRepository = authRepository
            ),
            
            deleteCategoryUseCase = DeleteCategoryUseCaseImpl(
                categoryCollectionRepository = categoryCollectionRepository
            ),
            
            renameCategoryUseCase = RenameCategoryUseCaseImpl(
                categoryCollectionRepository = categoryCollectionRepository
            ),
            
            moveCategoryUseCase = MoveCategoryUseCaseImpl(),
            
            // 프로젝트 구조 조회 및 변환
            getProjectAllCategoriesUseCase = GetProjectAllCategoriesUseCaseImpl(
                categoryCollectionRepository = categoryCollectionRepository
            ),
            
            convertProjectStructureToDraggableItemsUseCase = ConvertProjectStructureToDraggableItemsUseCaseImpl(),
            
            // 구조 업데이트
            updateProjectStructureUseCase = UpdateProjectStructureUseCase(
                categoryRepository = categoryRepository,
                projectChannelRepository = projectChannelRepository
            ),
            
            // 채널-카테고리 간 이동
            moveChannelBetweenCategoriesUseCase = MoveChannelBetweenCategoriesUseCaseImpl(
                categoryCollectionRepository = categoryCollectionRepository
            ),
            
            // 카테고리 도메인 UseCases
            getCategoryDetailsUseCase = GetCategoryDetailsUseCaseImpl(
                categoryRepository = categoryRepository
            ),
            
            updateCategoryUseCase = UpdateCategoryUseCaseImpl(
                categoryRepository = categoryRepository
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
    val moveCategoryUseCase: MoveCategoryUseCase,
    
    // 프로젝트 구조 조회 및 변환
    val getProjectAllCategoriesUseCase: GetProjectAllCategoriesUseCase,
    val convertProjectStructureToDraggableItemsUseCase: ConvertProjectStructureToDraggableItemsUseCase,
    
    // 구조 업데이트
    val updateProjectStructureUseCase: UpdateProjectStructureUseCase,
    
    // 채널-카테고리 간 이동
    val moveChannelBetweenCategoriesUseCase: MoveChannelBetweenCategoriesUseCase,
    
    // 카테고리 도메인 UseCases
    val getCategoryDetailsUseCase: GetCategoryDetailsUseCase,
    val updateCategoryUseCase: UpdateCategoryUseCase,
    
    // 공통 Repository
    val authRepository: AuthRepository,
    val categoryRepository: CategoryRepository
)