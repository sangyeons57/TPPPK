package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.DMWrapperRemoteDataSource
import com.example.data.model._remote.DMWrapper
import com.example.domain.repository.DMWrapperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DMWrapperRepositoryImpl @Inject constructor(
    private val dmWrapperRemoteDataSource: DMWrapperRemoteDataSource
    // TODO: 필요한 경우 LocalDataSource 및 Mapper 주입
) : DMWrapperRepository {

    override fun getDMWrappersStream(userId: String): Flow<CustomResult<List<DMWrapper>, Exception>> {
        return dmWrapperRemoteDataSource.getDMWrappersStream(userId).map { result ->
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() } // TODO: DMWrapperDto를 DMWrapper로 매핑
            }
        }
    }

    override fun getDMWrapperStream(dmChannelId: String): Flow<CustomResult<DMWrapper, Exception>> {
        return dmWrapperRemoteDataSource.getDMWrapperStream(dmChannelId).map { result ->
            result.mapCatching { dto ->
                dto.toDomain() // TODO: DMWrapperDto를 DMWrapper로 매핑
            }
        }
    }

    override fun createDmChannel(otherUserId: String): CustomResult<String, Exception> {
        TODO("Not yet implemented")
    }

    override fun findDmChannelWithUser(otherUserId: String): CustomResult<String, Exception> {
        TODO("Not yet implemented")
    }
}
