package com.example.domain.usecase.project

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.util.DateTimeUtil
import com.example.domain.model.base.Category
import com.example.domain.repository.AuthRepository // Added
import com.example.domain.repository.CategoryRepository // Added
import com.example.domain.model.Constants // Added
import kotlinx.coroutines.flow.first // Added
import javax.inject.Inject

/**
 * 프로젝트에 새 카테고리를 추가하는 유스케이스 인터페이스입니다.
 *
 * 이 유스케이스는 지정된 프로젝트에 새로운 카테고리를 생성하고 추가하는 비즈니스 로직을 정의합니다.
 * 카테고리 이름의 유효성을 검사하고, 적절한 순서 번호를 할당한 후, 저장소 계층을 통해 데이터를 영속화합니다.
 */
interface AddCategoryUseCase {
    /**
     * 지정된 프로젝트에 새 카테고리를 추가합니다.
     *
     * @param projectId 카테고리를 추가할 대상 프로젝트의 고유 ID입니다.
     * @param categoryName 새로 추가할 카테고리의 이름입니다. 공백이거나 너무 길 경우 실패할 수 있습니다.
     * @return 작업 성공 시 생성된 [Category] 객체를 포함하는 [CustomResult.Success]를 반환합니다.
     *         실패 시 예외 정보를 포함하는 [CustomResult.Failure]를 반환합니다.
     */
    suspend operator fun invoke(
        projectId: String,
        categoryName: String
    ): CustomResult<Category, Exception>
}

/**
 * [AddCategoryUseCase]의 구현체입니다.
 *
 * 실제 카테고리 추가 로직을 수행하며, 사용자 인증 정보 확인, 기존 카테고리 목록 조회,
 * 새 카테고리 객체 생성 및 저장소 호출을 담당합니다.
 */
class AddCategoryUseCaseImpl @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : AddCategoryUseCase {
    
    /**
     * 지정된 프로젝트에 새 카테고리를 추가하는 로직을 실행합니다.
     *
     * 1. 현재 사용자 ID를 가져옵니다.
     * 2. 프로젝트의 기존 카테고리 목록을 조회하여 새 카테고리의 순서([Category.order])를 결정합니다.
     *    - "카테고리 없음"([Constants.NO_CATEGORY_NAME])은 순서 `0.0`으로 고정되며, 새 카테고리는 그 이후 순번으로 할당됩니다.
     *    - 기존 카테고리가 없거나 "카테고리 없음"만 있는 경우 `1.0`부터 시작합니다.
     *    - 그 외의 경우, 기존 카테고리 중 가장 큰 순서 값에 `1.0`을 더한 값을 사용합니다.
     * 3. 새 [Category] 객체를 생성합니다. ID는 저장소에서 자동 생성됩니다.
     * 4. [CategoryRepository]를 통해 새 카테고리를 저장합니다.
     *
     * @param projectId 카테고리를 추가할 대상 프로젝트의 고유 ID입니다.
     * @param categoryName 새로 추가할 카테고리의 이름입니다.
     * @return 작업 성공 시 생성되고 ID가 할당된 [Category] 객체를 포함하는 [CustomResult.Success]를 반환합니다.
     *         실패(예: 사용자 인증 실패, 데이터베이스 오류 등) 시 예외 정보를 포함하는 [CustomResult.Failure]를 반환합니다.
     */
    override suspend operator fun invoke(
        projectId: String,
        categoryName: String
    ): CustomResult<Category, Exception> {
        // 1. Get current user ID
        val currentUserId = when (val currentUserResult = authRepository.getCurrentUserId()) {
            is CustomResult.Success -> currentUserResult.data
            is CustomResult.Failure -> return CustomResult.Failure(currentUserResult.error) // Propagate error
            else -> return CustomResult.Failure(Exception("Failed to get current user ID."))
        }

        // 2. Fetch existing categories for the project to determine the next order
        val existingCategoriesResult = categoryRepository.getCategoriesStream(projectId).first() // Get the first emission
        val nextOrder = when (existingCategoriesResult) {
            is CustomResult.Success -> {
                val categories = existingCategoriesResult.data
                // Exclude NO_CATEGORY_ORDER when finding max, as it's fixed.
                // New categories should be ordered after regular categories.
                // 기존 카테고리들의 최대 order 값을 찾습니다. "카테고리 없음"의 order (0.0) 보다 큰 값을 기준으로 합니다.
                // 만약 "카테고리 없음"만 있거나 아무 카테고리도 없다면, 새로운 카테고리는 1.0부터 시작합니다.
                val newOrder = if (categories.isEmpty() || (categories.size == 1 && categories.first().order == Constants.NO_CATEGORY_ORDER)) {
                    1.0 // First actual category gets order 1.0
                } else {
                    (categories.filter { it.order > Constants.NO_CATEGORY_ORDER }.maxOfOrNull { it.order } ?: 0.0) + 1.0
                }
                newOrder
            }
            is CustomResult.Failure -> {
                return CustomResult.Failure(existingCategoriesResult.error) // Propagate error
            }
            else -> {
                return CustomResult.Failure(Exception("Failed to get existing categories."))
            }
        }

        // 3. Create new Category object
        val newCategory = Category(
            name = categoryName.trim(),
            order = nextOrder,
            createdBy = currentUserId,
            createdAt = DateTimeUtil.nowInstant(),
            updatedAt = DateTimeUtil.nowInstant(),
        )

        // 4. Add category using repository
        return when (val addResult = categoryRepository.addCategory(projectId, newCategory)) {
            is CustomResult.Success -> {
                // The repository returns the ID. We can update our newCategory object's ID if needed,
                // but since we generated it, it should be the same. We return the full Category object.
                CustomResult.Success(newCategory.copy(id = addResult.data)) // Ensure ID from repo is used
            }
            is CustomResult.Failure -> CustomResult.Failure(addResult.error)
            else -> return CustomResult.Failure(Exception("Failed to add category."))
        }
    }
}
