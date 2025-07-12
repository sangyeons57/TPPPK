package com.example.domain.usecase.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 특정 카테고리의 채널 목록을 가져오는 유스케이스 인터페이스
 * DDD 방식에 따라 ProjectChannel 도메인 엔티티만 사용합니다.
 * 
 * 새로운 구조에서는 모든 프로젝트 채널이 프로젝트 레벨에 저장되고,
 * categoryId 속성으로 카테고리를 구분합니다.
 */
interface GetCategoryChannelsUseCase {
    /**
     * 특정 카테고리에 속한 채널 목록을 반환합니다.
     * @param categoryId 카테고리 ID
     * @return Flow<CustomResult<List<ProjectChannel>, Exception>> 해당 카테고리의 채널 목록
     */
    suspend operator fun invoke(categoryId: DocumentId): Flow<CustomResult<List<ProjectChannel>, Exception>>
}

/**
 * GetCategoryChannelsUseCase의 구현체
 * Provider로부터 ProjectChannelRepository를 받아 채널 데이터를 가져옵니다.
 */
class GetCategoryChannelsUseCaseImpl @Inject constructor(
    private val projectChannelRepository: ProjectChannelRepository
) : GetCategoryChannelsUseCase {

    /**
     * 유스케이스를 실행하여 특정 카테고리의 채널 목록을 가져옵니다.
     * ProjectChannelRepository의 observeAll()을 사용하여 모든 프로젝트 채널을 조회한 후,
     * categoryId로 필터링합니다.
     * 
     * @param categoryId 카테고리 ID
     * @return Flow<CustomResult<List<ProjectChannel>, Exception>> 해당 카테고리의 채널 목록
     */
    override suspend fun invoke(categoryId: DocumentId): Flow<CustomResult<List<ProjectChannel>, Exception>> {
        // 모든 프로젝트 채널을 조회하고 categoryId로 필터링
        return projectChannelRepository.observeAll().map { result ->
            when (result) {
                is CustomResult.Success -> {
                    val filteredChannels = result.data
                        .filterIsInstance<ProjectChannel>()
                        .filter { it.categoryId == categoryId } // 특정 카테고리만 필터링
                        .filter { it.isActive() } // 활성 채널만 표시
                        .sortedBy { it.order.value }
                    CustomResult.Success(filteredChannels)
                }
                is CustomResult.Failure -> result
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }
}