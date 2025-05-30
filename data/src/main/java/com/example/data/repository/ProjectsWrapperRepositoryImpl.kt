package com.example.data.repository

import androidx.room.util.recursiveFetchLongSparseArray
import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.ProjectsWrapperRemoteDataSource
import com.example.data.model.mapper.toDomain // TODO: 실제 매퍼 경로 및 함수 확인
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.repository.ProjectsWrapperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProjectsWrapperRepositoryImpl @Inject constructor(
    private val projectsWrapperRemoteDataSource: ProjectsWrapperRemoteDataSource
    // TODO: 필요한 Mapper 주입
) : ProjectsWrapperRepository {

    override fun getProjectsWrapperStream(userId: String): Flow<List<CustomResult<ProjectsWrapper, Exception>>> {
        return projectsWrapperRemoteDataSource.observeProjectsWrappers(userId).map {
            it.map { dto -> CustomResult.Success( dto.toDomain()) }
        }
    }

    override suspend fun refreshProjectsWrapper(userId: String): CustomResult<Unit, Exception> {
        return projectsWrapperRemoteDataSource.refreshProjectsWrapper(userId)
    }
}
