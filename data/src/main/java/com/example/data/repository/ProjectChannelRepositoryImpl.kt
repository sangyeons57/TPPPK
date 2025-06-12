package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.ProjectChannelRemoteDataSource // 프로젝트 채널 데이터 소스 import
import com.example.data.model.remote.ProjectChannelDTO
import com.example.data.model.remote.toDto
import com.example.domain.model.base.ProjectChannel
import com.example.domain.repository.ProjectChannelRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProjectChannelRepositoryImpl @Inject constructor(
    private val projectChannelRemoteDataSource: ProjectChannelRemoteDataSource // 프로젝트 채널 데이터 소스 주입
    // 필요한 경우 LocalDataSource 등 다른 의존성 추가
) : ProjectChannelRepository {

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

    override fun getProjectChannelStream(
        projectId: String,
        channelId: String
    ): Flow<CustomResult<ProjectChannel, Exception>> {
        // 현재 RemoteDataSource에 단일 채널을 관찰하는 API가 없으므로,
        // 카테고리별 채널 스트림을 재활용하여 필터링합니다.
        // 실제 구현에서는 효율을 위해 별도의 API를 추가하는 것이 좋습니다.
        return flow {
            // "No Category" 등 모든 카테고리를 돌면서 첫 매칭 채널을 찾는다.
            // 간단히 프로젝트 내 모든 카테고리를 가져올 수 있는 API가 없으므로 미구현 상태 반환
            emit(CustomResult.Failure(Exception(NotImplementedError("getProjectChannelStream not implemented yet."))))
        }
    }

    override suspend fun addProjectChannel(projectId: String, channel: ProjectChannel): CustomResult<Unit, Exception> {
        // TODO: 기존 ChannelRepositoryImpl의 프로젝트 채널 생성 로직 구현
        // 예: return projectChannelRemoteDataSource.createProjectChannel(projectId, channel.toDto())
        throw NotImplementedError("구현 필요: createProjectChannel")
    }

    override suspend fun setProjectChannel(projectId: String, categoryId: String, channel: ProjectChannel): CustomResult<Unit, Exception> {
        return resultTry {
            val result = projectChannelRemoteDataSource.addProjectChannel(
                projectId = projectId,
                categoryId = categoryId,
                name = channel.channelName,
                type = channel.channelType.name
            )
            result.fold(
                onSuccess = { CustomResult.Success(Unit) },
                onFailure = { CustomResult.Failure(it as? Exception ?: Exception(it)) }
            )
        }
    }

    override suspend fun updateProjectChannel(projectId: String, channel: ProjectChannel): CustomResult<Unit, Exception> {
        return try {
            // Convert ProjectChannel domain model to ProjectChannelDTO
            // The toDto() extension function is available from ProjectChannelDTO.kt
            val channelDto = channel.toDto()
            // The ProjectChannelDTO now includes the 'order' field.
            // The 'updatedAt' field in DTO will be handled by @ServerTimestamp in Firestore if set up correctly,
            // or the value from channel.updatedAt (converted to Timestamp) will be written.
            // For updates, it's common to only send changed fields, but here we send the whole DTO.
            projectChannelRemoteDataSource.updateProjectChannel(projectId, channelDto)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            com.example.core_common.result.CustomResult.Failure(e) // Ensure CustomResult is used
        }
    }

    override suspend fun deleteProjectChannel(projectId: String, channelId: String): CustomResult<Unit, Exception> {
        // TODO: 기존 ChannelRepositoryImpl의 프로젝트 채널 삭제 로직 구현
        // 예: return projectChannelRemoteDataSource.deleteProjectChannel(projectId, channelId)
        throw NotImplementedError("구현 필요: deleteProjectChannel")
    }
}
