package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.ProjectChannelRemoteDataSource // 프로젝트 채널 데이터 소스 import
import com.example.data.model.remote.ProjectChannelDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.event.AggregateRoot
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.base.ProjectChannelRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProjectChannelRepositoryImpl @Inject constructor(
    private val projectChannelRemoteDataSource: ProjectChannelRemoteDataSource // 프로젝트 채널 데이터 소스 주입
    , override val factoryContext: DefaultRepositoryFactoryContext
    // 필요한 경우 LocalDataSource 등 다른 의존성 추가
) : DefaultRepositoryImpl(projectChannelRemoteDataSource, factoryContext.collectionPath), ProjectChannelRepository {

    override fun getProjectChannelsByCategoryStream(projectId: String, categoryId: String): Flow<CustomResult<List<ProjectChannel>, Exception>> {
        // Remote data source에서 실시간 스트림을 가져와 DTO -> Domain 변환 후 Result 래핑
        return projectChannelRemoteDataSource
            .observeProjectChannels(projectId, categoryId)
            .map { dtoList: List<ProjectChannelDTO> ->
                val domainList = dtoList.map { it.toDomain() }
                // Success는 E 타입이 Nothing이므로 Exception 자리에도 할당 가능 (공변)
                CustomResult.Success(domainList) as CustomResult<List<ProjectChannel>, Exception>
            }
            .catch { e ->
                // Flow exception transparency 유지
                if (e is CancellationException) throw e
                emit(CustomResult.Failure(Exception(e)))
            }
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is ProjectChannel)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type ProjectChannel"))

        return if (entity.id.isAssigned()) {
            projectChannelRemoteDataSource.update(entity.id, entity.getChangedFields())
        } else {
            projectChannelRemoteDataSource.create(entity.toDto())
        }
    }
}
