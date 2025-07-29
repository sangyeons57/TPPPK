package com.example.data_core.repository.base

// import java.util.Date // For initial timestamp if needed, though serverTimestamp is preferred
import com.example.core_common.result.CustomResult
import com.example.data_core.datasource.remote.DMWrapperRemoteDataSource
import com.example.data_core.repository.DefaultRepositoryImpl
import com.example.data_model.remote.toDto
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.base.DMWrapperRepository
import com.example.domain.repository.factory.context.DMWrapperRepositoryFactoryContext
import javax.inject.Inject

class DMWrapperRepositoryImpl @Inject constructor(
    private val dmWrapperRemoteDataSource: DMWrapperRemoteDataSource,
    override val factoryContext: DMWrapperRepositoryFactoryContext
) : DefaultRepositoryImpl(dmWrapperRemoteDataSource, factoryContext), DMWrapperRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is DMWrapper)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type DMWrapper"))
        ensureCollection()

        return if (entity.isNew) {
            dmWrapperRemoteDataSource.create(entity.toDto())
        } else {
            dmWrapperRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
