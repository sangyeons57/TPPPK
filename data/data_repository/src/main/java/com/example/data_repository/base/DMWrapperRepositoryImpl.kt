package com.example.data_repository.base

// import java.util.Date // For initial timestamp if needed, though serverTimestamp is preferred
import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.DMWrapperRemoteDataSource
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.DocumentId
import com.example.domain_repository.base.DMWrapperRepository
import com.example.mapper.dm.DMWrapperMapper
import javax.inject.Inject

class DMWrapperRepositoryImpl @Inject constructor(
    private val dmWrapperRemoteDataSource: DMWrapperRemoteDataSource,
    private val dmWrapperMapper: DMWrapperMapper,
) : DefaultRepositoryImpl(dmWrapperRemoteDataSource), DMWrapperRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is DMWrapper)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type DMWrapper"))
        ensureCollection()

        return if (entity.isNew) {
            dmWrapperRemoteDataSource.create(dmWrapperMapper.domainToDto(entity))
        } else {
            dmWrapperRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
