package com.example.domain.provider

import com.example.domain.repository.local.LocalCategoryRepository
import com.example.domain.usecase.local.project.category.DeleteCategoryLocalUseCase
import com.example.domain.usecase.local.project.category.DeleteCategoryLocalUseCaseImpl
import com.example.domain.usecase.local.project.category.GetCategoryDetailsLocalUseCase
import com.example.domain.usecase.local.project.category.GetCategoryDetailsLocalUseCaseImpl
import com.example.domain.usecase.local.project.category.RenameCategoryLocalUseCase
import com.example.domain.usecase.local.project.category.RenameCategoryLocalUseCaseImpl
import com.example.domain.usecase.local.project.category.ReorderCategoriesLocalUseCase
import com.example.domain.usecase.local.project.category.ReorderCategoriesLocalUseCaseImpl
import com.example.domain.usecase.local.project.category.UpdateCategoryLocalUseCase
import com.example.domain.usecase.local.project.category.UpdateCategoryLocalUseCaseImpl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프로젝트 카테고리 관리 관련 Local UseCase들을 제공하는 Provider
 * 
 * 로컬 저장소를 기반으로 한 카테고리 생성, 수정, 삭제, 순서 변경 등의 기능을 담당합니다.
 */
@Singleton
class ProjectCategoryUseCaseProvider @Inject constructor(
    private val categoryLocalRepository: LocalCategoryRepository
) {

    /**
     * 프로젝트 카테고리 관리 관련 UseCase들을 생성합니다.
     * 
     * @return 프로젝트 카테고리 관리 UseCase 그룹
     */
    fun create(): ProjectCategoryLocalUseCases {
        return ProjectCategoryLocalUseCases(
            // 카테고리 조회
            getCategoryDetailsLocalUseCase = GetCategoryDetailsLocalUseCaseImpl(
                categoryLocalRepository = categoryLocalRepository
            ),
            
            // 카테고리 수정
            updateCategoryLocalUseCase = UpdateCategoryLocalUseCaseImpl(
                categoryLocalRepository = categoryLocalRepository
            ),
            
            renameCategoryLocalUseCase = RenameCategoryLocalUseCaseImpl(
                categoryLocalRepository = categoryLocalRepository
            ),
            
            // 카테고리 삭제
            deleteCategoryLocalUseCase = DeleteCategoryLocalUseCaseImpl(
                categoryLocalRepository = categoryLocalRepository
            ),
            
            // 카테고리 순서 변경
            reorderCategoriesLocalUseCase = ReorderCategoriesLocalUseCaseImpl(
                categoryLocalRepository = categoryLocalRepository
            ),
            
            categoryLocalRepository = categoryLocalRepository
        )
    }
}

/**
 * 프로젝트 카테고리 관리 Local UseCase 그룹
 */
data class ProjectCategoryLocalUseCases(
    // 카테고리 조회
    val getCategoryDetailsLocalUseCase: GetCategoryDetailsLocalUseCase,
    
    // 카테고리 수정
    val updateCategoryLocalUseCase: UpdateCategoryLocalUseCase,
    val renameCategoryLocalUseCase: RenameCategoryLocalUseCase,
    
    // 카테고리 삭제
    val deleteCategoryLocalUseCase: DeleteCategoryLocalUseCase,
    
    // 카테고리 순서 변경
    val reorderCategoriesLocalUseCase: ReorderCategoriesLocalUseCase,

    val categoryLocalRepository: LocalCategoryRepository
) 