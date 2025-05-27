package com.example.data.repository

import com.example.data.datasource.remote.CategoryRemoteDataSource
import com.example.data.model._remote.CategoryDTO // DTO 임포트
import com.example.data.model.mapper.toDomain // Domain으로 매핑하는 확장함수
import com.example.domain.model.Category
import com.example.domain.repository.CategoryRepository
import com.google.firebase.Timestamp // Timestamp 사용 시
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result // 표준 Result 사용
import com.example.core_common.result.resultTry // resultTry 확장 함수

class CategoryRepositoryImpl @Inject constructor(
    private val categoryRemoteDataSource: CategoryRemoteDataSource
    // private val categoryLocalDataSource: CategoryLocalDataSource // 로컬 사용시 주석 해제
    // private val categoryMapper: CategoryMapper // 개별 매퍼 클래스 사용시
) : CategoryRepository {

    override fun getCategoriesStream(projectId: String): Flow<Result<List<Category>>> {
        return categoryRemoteDataSource.getCategoriesStream(projectId).map { result ->
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() }
            }
        }
    }

    override suspend fun getCategory(categoryId: String): Result<Category> = resultTry {
        categoryRemoteDataSource.getCategory(categoryId).getOrThrow().toDomain()
    }

    override suspend fun createCategory(categoryName: String, projectId: String): Result<String> = resultTry {
        // DataSource는 DTO를 받도록 설계되었을 가능성이 높으므로, DTO를 여기서 생성합니다.
        // ID는 Firestore에서 자동 생성될 것이므로, DTO 생성 시 id는 null 또는 기본값일 수 있습니다.
        // Firestore에서 자동 생성 ID를 사용하고 반환받는 로직이 DataSource에 필요합니다.
        // 여기서는 임시로 DTO를 구성합니다. 실제 DTO 구조에 맞춰야 합니다.
        val newCategoryDto = CategoryDTO(
            name = categoryName,
            projectId = projectId,
            order = 0, // 기본 순서 또는 DataSource에서 다음 순서를 계산해야 할 수 있음
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
            // id는 datasource에서 설정하거나, 반환받은 ID를 사용
        )
        categoryRemoteDataSource.createCategory(newCategoryDto).getOrThrow() // DataSource가 ID를 반환한다고 가정
    }

    override suspend fun updateCategoryName(categoryId: String, newName: String): Result<Unit> = resultTry {
        // 전체 CategoryDTO를 업데이트 하는 방식일 수도 있고, 특정 필드만 업데이트하는 함수가 DataSource에 있을 수도 있습니다.
        // 여기서는 get -> DTO 수정 -> update DTO 흐름을 가정합니다.
        // 또는 DataSource에 updateCategoryName(categoryId, newName) 같은 함수가 직접 있을 수 있습니다.
        // 현재 CategoryRemoteDataSource에는 updateCategory(CategoryDTO)만 있으므로 이를 활용합니다.
        val currentCategoryDto = categoryRemoteDataSource.getCategory(categoryId).getOrThrow()
        val updatedCategoryDto = currentCategoryDto.copy(
            name = newName,
            updatedAt = Timestamp.now()
        )
        categoryRemoteDataSource.updateCategory(updatedCategoryDto).getOrThrow()
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> = resultTry {
        categoryRemoteDataSource.deleteCategory(categoryId).getOrThrow()
    }

    override suspend fun updateCategoryOrder(projectId: String, categoryOrders: Map<String, Int>): Result<Unit> = resultTry {
        // 이 기능은 Firestore 트랜잭션 또는 일괄 쓰기를 통해 DataSource에서 구현될 가능성이 높습니다.
        // DataSource에 해당 기능(예: updateCategoryOrdersInProject)이 필요합니다.
        // 현재 CategoryRemoteDataSource에는 이 기능이 명시적으로 없으므로, 추가가 필요할 수 있습니다. (2.1 항목 참고)
        // 여기서는 DataSource에 이 기능이 있다고 가정하고 호출합니다.
        // categoryRemoteDataSource.updateCategoryOrder(projectId, categoryOrders).getOrThrow()
        // 위 주석 처리된 부분은 해당 기능이 DataSource에 추가되면 활성화합니다.
        // 임시로 Not Implemented 처리:
        throw NotImplementedError(\
updateCategoryOrder
is
not
implemented
in
CategoryRemoteDataSource
yet.\)
    }
}
