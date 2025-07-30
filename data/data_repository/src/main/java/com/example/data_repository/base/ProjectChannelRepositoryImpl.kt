package com.example.data_repository.base

import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.ProjectChannelRemoteDataSource
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.ProjectChannel
import com.example.domain.model.vo.DocumentId
import com.example.domain_repository.base.ProjectChannelRepository
import com.example.mapper.project.ProjectChannelMapper
import javax.inject.Inject

class ProjectChannelRepositoryImpl @Inject constructor(
    private val projectChannelRemoteDataSource: ProjectChannelRemoteDataSource, // 프로젝트 채널 데이터 소스 주입
    private val projectChannelMapper: ProjectChannelMapper,
    // 필요한 경우 LocalDataSource 등 다른 의존성 추가
) : DefaultRepositoryImpl(projectChannelRemoteDataSource), ProjectChannelRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is ProjectChannel)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type ProjectChannel"))
        ensureCollection()
        return if (entity.isNew) {
            projectChannelRemoteDataSource.create(projectChannelMapper.domainToDto(entity))
        } else {
            projectChannelRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
