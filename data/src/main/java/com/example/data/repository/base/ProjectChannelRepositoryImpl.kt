package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.ProjectChannelRemoteDataSource // 프로젝트 채널 데이터 소스 import
import com.example.data.model.DTO
import com.example.data.model.remote.ProjectChannelDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.factory.context.ProjectChannelRepositoryFactoryContext
import com.example.domain.repository.base.ProjectChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProjectChannelRepositoryImpl @Inject constructor(
    private val projectChannelRemoteDataSource: ProjectChannelRemoteDataSource // 프로젝트 채널 데이터 소스 주입
    , override val factoryContext: ProjectChannelRepositoryFactoryContext
    // 필요한 경우 LocalDataSource 등 다른 의존성 추가
) : DefaultRepositoryImpl(projectChannelRemoteDataSource, factoryContext), ProjectChannelRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is ProjectChannel)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type ProjectChannel"))
        ensureCollection()
        return if (entity.isNew) {
            projectChannelRemoteDataSource.create(entity.toDto())
        } else {
            projectChannelRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
