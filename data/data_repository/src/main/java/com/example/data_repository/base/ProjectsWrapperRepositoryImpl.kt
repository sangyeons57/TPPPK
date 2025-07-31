package com.example.data_repository.base

import com.example.data_datasource.remote.ProjectsWrapperRemoteDataSource
import com.example.data_model.remote.ProjectsWrapperDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain_repository.base.ProjectsWrapperRepository
import com.example.mapper.DtoMapper
import javax.inject.Inject

class ProjectsWrapperRepositoryImpl @Inject constructor(
    projectsWrapperRemoteDataSource: ProjectsWrapperRemoteDataSource,
    private val projectsWrapperMapper: DtoMapper<ProjectsWrapper, ProjectsWrapperDTO>,
) : DefaultRepositoryImpl<ProjectsWrapper, ProjectsWrapperDTO>(
    projectsWrapperRemoteDataSource,
    projectsWrapperMapper
), ProjectsWrapperRepository {
    // 모든 기본 CRUD 메서드들은 부모 클래스에서 자동으로 처리됩니다!
}
