package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.enum.ProjectChannelType
import com.example.domain.repository.collection.CategoryCollectionRepository
import com.example.core_common.util.DateTimeUtil
import com.example.core_common.constants.Constants // Added
import kotlinx.coroutines.flow.first // Added
import java.text.DecimalFormat
import javax.inject.Inject

/**
 * 지정된 카테고리에 새 프로젝트 채널을 추가하는 유스케이스 인터페이스입니다.
 *
 * 이 유스케이스는 특정 프로젝트 내의 특정 카테고리에 새로운 채널을 생성하고 추가하는 비즈니스 로직을 정의합니다.
 * 채널 이름의 유효성을 검사하고, 채널 순서를 계산하며, 카테고리 당 최대 채널 수를 확인한 후,
 * 저장소 계층을 통해 데이터를 영속화합니다.
 */
interface AddProjectChannelUseCase {
    /**
     * 지정된 프로젝트의 특정 카테고리에 새 채널을 추가합니다.
     *
     * @param projectId 채널을 추가할 대상 프로젝트의 고유 ID입니다.
     * @param channelName 새로 추가할 채널의 이름입니다.
     * @param channelType 새로 추가할 채널의 타입([ProjectChannelType])입니다.
     * @param categoryId 채널이 속하게 될 부모 카테고리의 고유 ID입니다.
     * @return 작업 성공 시 생성된 [ProjectChannel] 객체를 포함하는 [CustomResult.Success]를 반환합니다.
     *         실패 시 (예: 채널 이름 부재, 카테고리 미존재, 최대 채널 수 초과 등) 예외 정보를 포함하는 [CustomResult.Failure]를 반환합니다.
     */
    suspend operator fun invoke(
        projectId: String,
        channelName: String,
        channelType: ProjectChannelType,
        categoryId: String
    ): CustomResult<ProjectChannel, Exception> // Changed return type
}

/**
 * [AddProjectChannelUseCase]의 구현체입니다.
 *
 * 실제 프로젝트 채널 추가 로직을 수행하며, 채널 이름 유효성 검사, 대상 카테고리 조회,
 * 카테고리 내 채널 수 제한 확인, 새 채널 순서 계산, 새 채널 객체 생성 및 저장소 호출을 담당합니다.
 */
class AddProjectChannelUseCaseImpl @Inject constructor(
    private val categoryCollectionRepository: CategoryCollectionRepository
) : AddProjectChannelUseCase {
    
    /**
     * 지정된 프로젝트의 특정 카테고리에 새 채널을 추가하는 로직을 실행합니다.
     *
     * 1. 채널 이름이 비어 있는지 확인합니다.
     * 2. [CategoryCollectionRepository]를 통해 프로젝트의 카테고리 컬렉션 목록을 가져오고, 대상 카테고리를 찾습니다.
     *    - 대상 카테고리가 없으면 오류를 반환합니다.
     * 3. 대상 카테고리의 기존 채널 수를 확인하여 [Constants.MAX_CHANNELS_PER_CATEGORY]를 초과하는지 검사합니다.
     *    - 초과 시 오류를 반환합니다.
     * 4. 새 채널의 순서([ProjectChannel.order])를 계산합니다. 기존 채널 중 가장 큰 순서 값에 `0.01`을 더하여 할당합니다.
     *    - 순서는 소수점 두 자리로 포맷됩니다.
     * 5. 새 [ProjectChannel] 객체를 생성합니다. ID는 저장소에서 자동 생성됩니다.
     * 6. [CategoryCollectionRepository]를 통해 새 채널을 대상 카테고리에 추가합니다.
     *
     * @param projectId 채널을 추가할 대상 프로젝트의 고유 ID입니다.
     * @param channelName 새로 추가할 채널의 이름입니다. 앞뒤 공백은 제거됩니다.
     * @param channelType 새로 추가할 채널의 타입([ProjectChannelType])입니다.
     * @param categoryId 채널이 속하게 될 부모 카테고리의 고유 ID입니다.
     * @return 작업 성공 시 생성된 [ProjectChannel] 객체(ID는 저장소에서 할당됨)를 포함하는 [CustomResult.Success]를 반환합니다.
     *         실패 시 예외 정보를 포함하는 [CustomResult.Failure]를 반환합니다.
     */
    override suspend operator fun invoke(
        projectId: String,
        channelName: String,
        channelType: ProjectChannelType,
        categoryId: String
    ): CustomResult<ProjectChannel, Exception> {
        val trimmedChannelName = channelName.trim()
        if (trimmedChannelName.isBlank()) {
            return CustomResult.Failure(IllegalArgumentException("Channel name cannot be blank."))
        }

        // 1. Fetch the category collection to get current channels in the target category
        val categoryCollectionsResult = categoryCollectionRepository.getCategoryCollections(projectId).first()
        val targetCategoryCollection = when (categoryCollectionsResult) {
            is CustomResult.Success -> categoryCollectionsResult.data.find { it.category.id.value == categoryId }
            is CustomResult.Failure -> return CustomResult.Failure(categoryCollectionsResult.error)
            else -> return CustomResult.Failure(Exception("Failed to get category collections."))
        }

        if (targetCategoryCollection == null) {
            return CustomResult.Failure(NoSuchElementException("Category with ID '$categoryId' not found in project '$projectId'."))
        }

        val existingChannelsInCat = targetCategoryCollection.channels

        // 2. Validate channel count
        if (existingChannelsInCat.size >= Constants.MAX_CHANNELS_PER_CATEGORY) {
            return CustomResult.Failure(IllegalStateException("Maximum channels per category (${Constants.MAX_CHANNELS_PER_CATEGORY}) reached for category '$categoryId'."))
        }

        // 3. Calculate new channel order
        val maxOrderInCat = existingChannelsInCat.maxOfOrNull { it.order } ?: 0.0
        // Add a small increment. Ensure formatting to two decimal places.
        // Adding 1.0 and then finding the next available slot like 1.01, 1.02 is also an option.
        // For simplicity, using a direct increment of 0.01. This could lead to floating point inaccuracies over many additions.
        // A more robust system might use integer parts and fractional parts separately or scaled integers.
        val newOrderValue = maxOrderInCat + 0.01
        // Format to ensure two decimal places, crucial for consistent ordering and comparison
        val newOrder = DecimalFormat("0.00").format(newOrderValue).toDouble()

        // 4. Create new ProjectChannel object
        val now = DateTimeUtil.nowInstant()
        val newChannel = ProjectChannel(
            channelName = trimmedChannelName,
            order = newOrder,
            channelType = channelType,
            createdAt = now,
            updatedAt = now
        )

        // 5. Add channel using repository
        val addResult = categoryCollectionRepository.addChannelToCategory(
            projectId = projectId,
            categoryId = categoryId,
            channel = newChannel
        )

        return when (addResult) {
            is CustomResult.Success -> CustomResult.Success(newChannel) // Return the channel we attempted to add
            is CustomResult.Failure -> CustomResult.Failure(addResult.error)
            else -> CustomResult.Failure(Exception("Failed to add channel."))
        }
    }
}
