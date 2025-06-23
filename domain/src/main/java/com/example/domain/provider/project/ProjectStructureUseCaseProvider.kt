package com.example.domain.provider.project

import com.example.domain.model.vo.CollectionPath
import com.example.domain.repository.RepositoryFactory
import com.example.domain.repository.base.AuthRepository
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.factory.context.AuthRepositoryFactoryContext
import com.example.domain.repository.factory.context.CategoryRepositoryFactoryContext
import com.example.domain.usecase.project.structure.AddCategoryUseCase
import com.example.domain.usecase.project.structure.ConvertProjectStructureToDraggableItemsUseCase
import com.example.domain.usecase.project.structure.DeleteCategoryUseCase
import com.example.domain.usecase.project.structure.GetProjectAllCategoriesUseCase
import com.example.domain.usecase.project.structure.MoveChannelBetweenCategoriesUseCase
import com.example.domain.usecase.project.structure.MoveCategoryUseCase
import com.example.domain.usecase.project.structure.RenameCategoryUseCase
import com.example.domain.usecase.project.structure.UpdateProjectStructureUseCase
import com.example.domain.usecase.project.category.GetCategoryDetailsUseCase
import com.example.domain.usecase.project.category.GetCategoryDetailsUseCaseImpl
import com.example.domain.usecase.project.category.UpdateCategoryUseCase
import com.example.domain.usecase.project.category.UpdateCategoryUseCaseImpl
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
    private val authRepositoryFactory: RepositoryFactory<AuthRepositoryFactoryContext, AuthRepository>
) {

    /**
     * 특정 프로젝트의 구조 관리 UseCase들을 생성합니다.
     * 
     * @param projectId 프로젝트 ID
     * @return 프로젝트 구조 관리 UseCase 그룹
     */
    fun createForProject(projectId: String): ProjectStructureUseCases {
        val categoryRepository = categoryRepositoryFactory.create(
            CategoryRepositoryFactoryContext(
                collectionPath = CollectionPath.projectCategories(projectId)
            )
        )

        val authRepository = authRepositoryFactory.create(
            AuthRepositoryFactoryContext()
        )

        return ProjectStructureUseCases(
            // 카테고리 관리
            addCategoryUseCase = AddCategoryUseCase(
                categoryRepository = categoryRepository,
                authRepository = authRepository
            ),
            
            deleteCategoryUseCase = DeleteCategoryUseCase(
                categoryRepository = categoryRepository
            ),
            
            renameCategoryUseCase = RenameCategoryUseCase(
                categoryRepository = categoryRepository
            ),
            
            moveCategoryUseCase = MoveCategoryUseCase(
                categoryRepository = categoryRepository
            ),
            
            // 프로젝트 구조 조회 및 변환
            getProjectAllCategoriesUseCase = GetProjectAllCategoriesUseCase(
                categoryRepository = categoryRepository
            ),
            
            convertProjectStructureToDraggableItemsUseCase = ConvertProjectStructureToDraggableItemsUseCase(
                categoryRepository = categoryRepository
            ),
            
            // 구조 업데이트
            updateProjectStructureUseCase = UpdateProjectStructureUseCase(
                categoryRepository = categoryRepository
            ),
            
            // 채널-카테고리 간 이동
            moveChannelBetweenCategoriesUseCase = MoveChannelBetweenCategoriesUseCase(
                categoryRepository = categoryRepository
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
    fun createForCurrentUser(projectId: String): ProjectStructureUseCases {
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