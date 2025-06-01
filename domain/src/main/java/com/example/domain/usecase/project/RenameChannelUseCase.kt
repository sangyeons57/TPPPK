package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.repository.CategoryCollectionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 채널 이름을 변경하는 유스케이스
 * 
 * 이 유스케이스는 프로젝트 구조에서 특정 카테고리 내의 채널 이름을 변경하는 기능을 제공합니다.
 */
interface RenameChannelUseCase {
    /**
     * 채널 이름을 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 채널이 속한 카테고리 ID
     * @param channelId 이름을 변경할 채널 ID
     * @param newName 새로운 채널 이름
     * @return 채널 이름이 변경된 카테고리 컬렉션을 포함한 CustomResult
     */
    suspend operator fun invoke(
        projectId: String,
        categoryId: String,
        channelId: String,
        newName: String
    ): CustomResult<CategoryCollection, Exception>
}

/**
 * RenameChannelUseCase 구현체
 */
class RenameChannelUseCaseImpl @Inject constructor(
    private val categoryCollectionRepository: CategoryCollectionRepository
) : RenameChannelUseCase {
    
    /**
     * 채널 이름을 변경합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param categoryId 채널이 속한 카테고리 ID
     * @param channelId 이름을 변경할 채널 ID
     * @param newName 새로운 채널 이름
     * @return 채널 이름이 변경된 카테고리 컬렉션을 포함한 CustomResult
     */
    override suspend operator fun invoke(
        projectId: String,
        categoryId: String,
        channelId: String,
        newName: String
    ): CustomResult<CategoryCollection, Exception> {
        // 입력값 검증
        if (newName.isBlank()) {
            return CustomResult.Failure(Exception("New name is blank"))
        }
        
        // 저장소를 통해 채널 이름 변경
        return categoryCollectionRepository.renameChannel(
            projectId = projectId,
            categoryId = categoryId,
            channelId = channelId,
            newName = newName
        )
    }
}
