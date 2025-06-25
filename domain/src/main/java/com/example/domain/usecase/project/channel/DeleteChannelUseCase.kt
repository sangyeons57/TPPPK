package com.example.domain.usecase.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.collection.CategoryCollectionRepository
import javax.inject.Inject

/**
 * 프로젝트 구조에서 채널을 삭제하는 유스케이스
 * 
 * 이 유스케이스는 프로젝트 구조에서 특정 카테고리 내의 채널을 삭제하고
 * 남은 채널들의 순서를 재정렬하는 기능을 제공합니다.
 */
interface DeleteChannelUseCase {
    /**
     * 프로젝트 구조에서 채널을 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 채널이 속한 카테고리 ID
     * @param channelId 삭제할 채널 ID
     * @return 채널이 삭제된 카테고리 컬렉션을 포함한 CustomResult
     */
    suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId,
        channelId: DocumentId
    ): CustomResult<CategoryCollection, Exception>
}

/**
 * DeleteChannelUseCase 구현체
 */
class DeleteChannelUseCaseImpl @Inject constructor(
    private val categoryCollectionRepository: CategoryCollectionRepository
) : DeleteChannelUseCase {
    
    /**
     * 프로젝트 구조에서 채널을 삭제합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 채널이 속한 카테고리 ID
     * @param channelId 삭제할 채널 ID
     * @return 채널이 삭제된 카테고리 컬렉션을 포함한 CustomResult
     */
    override suspend operator fun invoke(
        projectId: DocumentId,
        categoryId: DocumentId,
        channelId: DocumentId
    ): CustomResult<CategoryCollection, Exception> {
        // 저장소를 통해 채널 삭제
        return categoryCollectionRepository.removeChannelFromCategory(
            projectId = projectId.value,
            categoryId = categoryId.value,
            channelId = channelId.value
        )
    }
}