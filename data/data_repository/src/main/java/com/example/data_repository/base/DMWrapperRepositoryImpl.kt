package com.example.data_repository.base

import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.DMWrapperRemoteDataSource
import com.example.data_model.remote.DMWrapperDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.base.DMWrapper
import com.example.domain.vo.UserId
import com.example.domain_repository.base.DMWrapperRepository
import com.example.mapper.DtoMapper
import javax.inject.Inject

class DMWrapperRepositoryImpl @Inject constructor(
    private val dmWrapperRemoteDataSource: DMWrapperRemoteDataSource,
    private val dmWrapperMapper: DtoMapper<DMWrapper, DMWrapperDTO>,
) : DefaultRepositoryImpl<DMWrapper, DMWrapperDTO>(dmWrapperRemoteDataSource, dmWrapperMapper),
    DMWrapperRepository {

    override suspend fun findByOtherUserId(otherUserId: UserId): CustomResult<DMWrapper, Exception> {
        return when (val result = dmWrapperRemoteDataSource.findByOtherUserId(otherUserId.value)) {
            is CustomResult.Success -> {
                CustomResult.Success(dmWrapperMapper.dtoToDomain(result.data))
            }

            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
            is CustomResult.Initial -> CustomResult.Initial
        }
    }
}
