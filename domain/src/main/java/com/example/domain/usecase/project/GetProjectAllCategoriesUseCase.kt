package com.example.domain.usecase.project

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Category
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.collection.CategoryCollection
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.ProjectChannelRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import javax.inject.Inject

/**
 * 특정 프로젝트의 구조(카테고리 및 채널 목록)를 가져오는 유스케이스 인터페이스
 */
interface GetProjectStructureUseCase {
    // 프로젝트 구조를 반환
    suspend operator fun invoke(projectId: String): Flow<CustomResult<List<CategoryCollection>, Exception>>
}

/**
 * GetProjectStructureUseCase의 구현체
 */
class GetProjectStructureUseCaseImpl @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val projectChannelRepository: ProjectChannelRepository,
) : GetProjectStructureUseCase {

    /**
     * 유스케이스를 실행하여 프로젝트 구조를 가져옵니다.
     * @param projectId 프로젝트 ID
     * @return Result<ProjectStructure> 프로젝트 구조 로드 결과
     */
    override suspend fun invoke(projectId: String): Flow<CustomResult<List<CategoryCollection>, Exception>> {
        return categoryRepository.getCategoriesStream(projectId).map { categories ->
            when (categories) {
                is CustomResult.Success -> {
                    val categoiesResult = categories.data;
                    CustomResult.Success(
                        categoiesResult.map { category ->
                            val projectChannels = projectChannelRepository.getProjectChannelsStream(
                                projectId,
                                category.id
                            ).map { projectChannelsResult ->
                                when (projectChannelsResult) {
                                    is CustomResult.Success -> projectChannelsResult.data
                                    else -> emptyList()
                                }
                            }
                            CategoryCollection(
                                category = category,
                                channels = projectChannels.first()
                            )
                        }.toList()
                    )
                }
                else -> {
                    CustomResult.Failure(Exception("Failed to load categories"))
                }
            }
        }
    }
}