package com.example.domain.usecase.project.structure

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 특정 프로젝트의 카테고리 목록을 가져오는 유스케이스 인터페이스
 * DDD 방식에 따라 Category 도메인 엔티티만 사용합니다.
 */
interface GetProjectAllCategoriesUseCase {
    /**
     * 프로젝트의 모든 카테고리 목록을 반환합니다.
     * @param projectId 프로젝트 ID
     * @return Flow<CustomResult<List<Category>, Exception>> 카테고리 목록을 포함한 결과
     */
    suspend operator fun invoke(): Flow<CustomResult<List<Category>, Exception>>
}

/**
 * GetProjectAllCategoriesUseCase의 구현체
 * Provider로부터 CategoryRepository를 받아 카테고리 데이터를 가져옵니다.
 */
class GetProjectAllCategoriesUseCaseImpl @Inject constructor(
    private val categoryRepository: CategoryRepository
) : GetProjectAllCategoriesUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트의 모든 카테고리 목록을 가져옵니다.
     * CategoryRepository의 observeAll()을 사용하여 실제 Firestore 데이터를 조회합니다.
     * 
     * @param projectId 프로젝트 ID (사용되지 않음 - Repository에서 이미 프로젝트별로 생성됨)
     * @return Flow<CustomResult<List<Category>, Exception>> 카테고리 목록을 포함한 결과
     */
    override suspend fun invoke(): Flow<CustomResult<List<Category>, Exception>> {
        // observeAll()을 사용하여 카테고리 목록 조회
        return categoryRepository.observeAll().map { result ->
            when (result) {
                is CustomResult.Success -> {
                    val categories = result.data
                        .filterIsInstance<Category>()
                        .sortedBy { it.order.value }
                        .toMutableList()
                    
                    // NoCategory는 UI 레이어에서 처리하도록 변경
                    // 여기서는 실제 DB 데이터만 반환하여 무한 로딩 방지
                    CustomResult.Success(categories)
                }
                is CustomResult.Failure -> result
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }
}