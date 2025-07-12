package com.example.domain.usecase.project.structure

import com.example.core_common.result.CustomResult
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.CategoryRepository
import com.example.domain.repository.base.ProjectChannelRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 카테고리를 삭제하는 유스케이스
 * 카테고리 삭제 시 해당 카테고리에 속한 모든 채널들을 soft delete 처리합니다.
 */
interface DeleteCategoryUseCase {
    /**
     * 카테고리를 삭제합니다.
     * 1. 해당 카테고리에 속한 모든 ProjectChannel을 DELETED 상태로 변경 (soft delete)
     * 2. 카테고리를 실제로 삭제 (hard delete)
     * 
     * @param categoryId 삭제할 카테고리 ID
     * @return 삭제 성공 여부를 포함한 CustomResult
     */
    suspend operator fun invoke(categoryId: DocumentId): CustomResult<Unit, Exception>
}

/**
 * DeleteCategoryUseCase 구현체
 * 카테고리 삭제 시 해당 카테고리의 모든 채널들을 soft delete 처리 후 카테고리를 삭제합니다.
 */
class DeleteCategoryUseCaseImpl @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val projectChannelRepository: ProjectChannelRepository
) : DeleteCategoryUseCase {
    
    override suspend operator fun invoke(categoryId: DocumentId): CustomResult<Unit, Exception> {
        return try {
            // 1. 해당 카테고리에 속한 모든 채널을 찾아서 soft delete 처리
            when (val channelsResult = projectChannelRepository.observeAll().first()) {
                is CustomResult.Success -> {
                    val channelsInCategory = channelsResult.data.filter { channel ->
                        channel.categoryId == categoryId
                    }
                    
                    // 각 채널을 DELETED 상태로 변경
                    for (channel in channelsInCategory) {
                        val deletedChannel = channel.markDeleted()
                        when (val saveResult = projectChannelRepository.save(deletedChannel)) {
                            is CustomResult.Failure -> {
                                return CustomResult.Failure(
                                    Exception("Failed to soft delete channel ${channel.id.value}: ${saveResult.error.message}")
                                )
                            }
                            else -> { /* Continue */ }
                        }
                    }
                    
                    // 2. 카테고리 실제 삭제 (hard delete)
                    categoryRepository.delete(categoryId)
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(
                        Exception("Failed to retrieve channels for category deletion: ${channelsResult.error.message}")
                    )
                }
                else -> {
                    CustomResult.Failure(Exception("Unexpected result when retrieving channels"))
                }
            }
        } catch (e: Exception) {
            CustomResult.Failure(e)
        }
    }
}