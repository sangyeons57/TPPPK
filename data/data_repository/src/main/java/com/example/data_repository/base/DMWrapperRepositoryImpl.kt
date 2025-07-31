package com.example.data_repository.base

import com.example.data_datasource.remote.DMWrapperRemoteDataSource
import com.example.data_model.remote.DMWrapperDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.base.DMWrapper
import com.example.domain_repository.base.DMWrapperRepository
import com.example.mapper.DtoMapper
import javax.inject.Inject

class DMWrapperRepositoryImpl @Inject constructor(
    dmWrapperRemoteDataSource: DMWrapperRemoteDataSource,
    private val dmWrapperMapper: DtoMapper<DMWrapper, DMWrapperDTO>,
) : DefaultRepositoryImpl<DMWrapper, DMWrapperDTO>(dmWrapperRemoteDataSource, dmWrapperMapper),
    DMWrapperRepository {
    // 모든 기본 CRUD 메서드들은 부모 클래스에서 자동으로 처리됩니다!
}
