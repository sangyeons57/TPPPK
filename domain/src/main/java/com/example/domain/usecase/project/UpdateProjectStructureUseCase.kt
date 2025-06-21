package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.base.ProjectChannelRepository
import javax.inject.Inject

/**
 * 프로젝트 구조를 업데이트하는 유스케이스
 * 카테고리 및 채널의 추가/삭제/수정/순서 변경 등 전체 프로젝트 구조 변경 사항을 저장합니다.
 */
class UpdateProjectStructureUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val projectChannelRepository: ProjectChannelRepository
) {
    /**
     * 프로젝트 구조를 업데이트합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryCollections 업데이트할 카테고리 컬렉션 목록
     * @return 작업 결과
     */
    suspend operator fun invoke(projectId: String, categoryCollections: List<CategoryCollection>): CustomResult<Unit, Exception> {
        try {
            // 카테고리 목록 추출
            val categories = categoryCollections.map { it.category }
            
            // 카테고리 업데이트
            val categoryResult = categoryRepository.updateCategories(projectId, categories)
            if (categoryResult is CustomResult.Failure) {
                return categoryResult
            }
            
            // 각 카테고리별 채널 업데이트
            for (categoryCollection in categoryCollections) {
                val categoryId = categoryCollection.category.id
                
                // 각 채널 업데이트
                for (channel in categoryCollection.channels) {
                    val channelResult = projectChannelRepository.updateProjectChannel(projectId, channel)
                    if (channelResult is CustomResult.Failure) {
                        return channelResult
                    }
                }
            }
            
            return CustomResult.Success(Unit)
        } catch (e: Exception) {
            return CustomResult.Failure(e)
        }
    }
}