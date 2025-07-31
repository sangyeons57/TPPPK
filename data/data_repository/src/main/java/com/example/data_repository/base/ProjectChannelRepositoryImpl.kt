package com.example.data_repository.base

import com.example.data_datasource.remote.ProjectChannelRemoteDataSource
import com.example.data_model.remote.ProjectChannelDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.base.ProjectChannel
import com.example.domain_repository.base.ProjectChannelRepository
import com.example.mapper.DtoMapper
import javax.inject.Inject

class ProjectChannelRepositoryImpl @Inject constructor(
    projectChannelRemoteDataSource: ProjectChannelRemoteDataSource,
    private val projectChannelMapper: DtoMapper<ProjectChannel, ProjectChannelDTO>,
) : DefaultRepositoryImpl<ProjectChannel, ProjectChannelDTO>(
    projectChannelRemoteDataSource,
    projectChannelMapper
), ProjectChannelRepository {
    // 모든 기본 CRUD 메서드들은 부모 클래스에서 자동으로 처리됩니다!
}
