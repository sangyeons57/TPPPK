package com.example.domain.usecase.project

import com.example.domain.repository.ProjectSettingRepository // Assuming this repo exists
import javax.inject.Inject

/**
 * 프로젝트 카테고리를 삭제하는 유스케이스 인터페이스
 */
interface DeleteCategoryUseCase {
    suspend operator fun invoke(categoryId: String): Result<Unit>
}

/**
 * DeleteCategoryUseCase의 구현체
 * @param projectSettingRepository 프로젝트 설정 관련 데이터 접근을 위한 Repository (가정)
 */
class DeleteCategoryUseCaseImpl @Inject constructor(
    // TODO: ProjectSettingRepository 또는 CategoryRepository 주입 필요
    // private val projectSettingRepository: ProjectSettingRepository
) : DeleteCategoryUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트 카테고리를 삭제합니다.
     * @param categoryId 삭제할 카테고리의 ID
     * @return Result<Unit> 삭제 처리 결과
     */
    override suspend fun invoke(categoryId: String): Result<Unit> {
        // TODO: Repository 호출 로직 구현 필요
        // return projectSettingRepository.deleteCategory(categoryId)
        println("UseCase: DeleteCategoryUseCase - $categoryId (TODO: Implement actual logic)")
        return Result.success(Unit) // Return success for now
    }
} 