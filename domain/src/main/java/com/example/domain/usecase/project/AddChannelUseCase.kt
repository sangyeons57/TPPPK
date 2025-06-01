package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.repository.CategoryCollectionRepository
import com.example.core_common.util.DateTimeUtil
import java.util.UUID
import javax.inject.Inject

/**
 * 프로젝트 구조에 새 채널을 추가하는 유스케이스
 * 
 * 이 유스케이스는 프로젝트 구조에 새 채널을 생성하고 추가하는 기능을 제공합니다.
 * 카테고리에 속한 채널을 추가합니다.
 */
interface AddChannelUseCase {
    /**
     * 새 채널을 추가합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelName 새 채널 이름
     * @param channelType 채널 타입
     * @param categoryId 채널이 속할 카테고리 ID
     * @return 추가된 채널과 업데이트된 카테고리 컬렉션을 포함한 CustomResult
     */
    suspend operator fun invoke(
        projectId: String,
        channelName: String,
        channelType: ProjectChannelType,
        categoryId: String
    ): CustomResult<CategoryCollection, Exception>
}

/**
 * AddChannelUseCase 구현체
 */
class AddChannelUseCaseImpl @Inject constructor(
    private val categoryCollectionRepository: CategoryCollectionRepository
) : AddChannelUseCase {
    
    /**
     * 새 채널을 추가합니다.
     * 
     * @param projectId 프로젝트 ID
     * @param channelName 새 채널 이름
     * @param channelType 채널 타입
     * @param categoryId 채널이 속할 카테고리 ID
     * @return 추가된 채널과 업데이트된 카테고리 컬렉션을 포함한 CustomResult
     */
    override suspend operator fun invoke(
        projectId: String,
        channelName: String,
        channelType: ProjectChannelType,
        categoryId: String
    ): CustomResult<CategoryCollection, Exception> {
        // 입력값 검증
        if (channelName.isBlank()) {
            return CustomResult.Failure(Exception("Channel name is blank"))
        }
        
        // 새 채널 생성
        val now = DateTimeUtil.nowInstant()
        val newChannel = ProjectChannel(
            id = UUID.randomUUID().toString(),
            channelName = channelName,
            channelType = channelType,
            order = 0.0, // 저장소에서 적절한 order 값을 설정할 것임
            createdAt = now,
            updatedAt = now
        )
        
        // 저장소를 통해 채널 추가
        return categoryCollectionRepository.addChannelToCategory(
            projectId = projectId,
            categoryId = categoryId,
            channel = newChannel
        )
    }
}
