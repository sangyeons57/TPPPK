package com.example.domain.usecase.project.channel

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.ProjectChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 카테고리별 채널 목록을 가져오는 유스케이스 인터페이스
 * DDD 방식에 따라 ProjectChannel 도메인 엔티티만 사용합니다.
 */
interface GetCategoryChannelsUseCase {
    /**
     * 카테고리의 채널 목록을 반환합니다.
     * @param categoryId 카테고리 ID
     * @return Flow<CustomResult<List<ProjectChannel>, Exception>> 채널 목록을 포함한 결과
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
     * 유스케이스를 실행하여 카테고리의 채널 목록을 가져옵니다.
     * ProjectChannelRepository의 observeAll()을 사용하여 실제 Firestore 데이터를 조회합니다.
     * 
     * @param categoryId 카테고리 ID (Repository에서 이미 카테고리별로 생성됨)
     * @return Flow<CustomResult<List<ProjectChannel>, Exception>> 채널 목록을 포함한 결과
     */
    override suspend fun invoke(categoryId: DocumentId): Flow<CustomResult<List<ProjectChannel>, Exception>> {
        // observeAll()을 사용하여 채널 목록 조회
        return projectChannelRepository.observeAll().map { result ->
            when (result) {
                is CustomResult.Success -> {
                    val channels = result.data
                        .filterIsInstance<ProjectChannel>()
                        .filter { it.isActive() } // 활성 채널만 표시
                        .sortedBy { it.order.value }
                    CustomResult.Success(channels)
                }
                is CustomResult.Failure -> result
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }
}