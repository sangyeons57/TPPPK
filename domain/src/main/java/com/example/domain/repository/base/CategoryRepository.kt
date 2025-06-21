package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.repository.DefaultRepository
import kotlinx.coroutines.flow.Flow

/**
 * 프로젝트 내 카테고리 관련 데이터 처리를 위한 인터페이스입니다.
 * 카테고리는 특정 프로젝트에 종속됩니다.
 */
interface CategoryRepository : DefaultRepository {
    suspend fun getCategoriesStream(projectId: String): Flow<CustomResult<List<Category>, Exception>>
}
