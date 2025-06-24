package com.example.domain.usecase.project.structure


import com.example.core_common.result.CustomResult
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.repository.collection.CategoryCollectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 특정 프로젝트의 구조(카테고리 및 채널 목록)를 가져오는 유스케이스 인터페이스
 */
interface GetProjectAllCategoriesUseCase {
    // 프로젝트 구조를 반환
    suspend operator fun invoke(projectId: String): Flow<CustomResult<List<CategoryCollection>, Exception>>
}

/**
 * GetProjectAllCategoriesUseCase의 구현체
 * CategoryCollectionRepository를 사용하여 카테고리와 채널 데이터를 함께 가져옵니다.
 */
class GetProjectAllCategoriesUseCaseImpl @Inject constructor(
    private val categoryCollectionRepository: CategoryCollectionRepository
) : GetProjectAllCategoriesUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트의 모든 카테고리와 채널 목록을 가져옵니다.
     * CategoryCollectionRepository를 통해 카테고리 컬렉션 데이터를 가져옵니다.
     * 
     * @param projectId 프로젝트 ID
     * @return Flow<CustomResult<List<CategoryCollection>, Exception>> 카테고리 목록과 각 카테고리에 속한 채널 목록을 포함한 결과
     */
    override suspend fun invoke(projectId: String): Flow<CustomResult<List<CategoryCollection>, Exception>> {
        // CategoryCollectionRepository에 데이터 가져오기 위임
        val c = categoryCollectionRepository.getCategoryCollections(projectId)
        //("GetProjectAllCategoriesUseCaseImpl", "invoke: $c")
        return c
    }
}