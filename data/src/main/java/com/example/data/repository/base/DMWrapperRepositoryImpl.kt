package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.DMWrapperRemoteDataSource
import com.example.data.model.remote.DMWrapperDTO // Assuming DMWrapperDTO is in this package
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.UserId
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.base.DMWrapperRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
// import java.util.Date // For initial timestamp if needed, though serverTimestamp is preferred
import javax.inject.Inject

class DMWrapperRepositoryImpl @Inject constructor(
    private val dmWrapperRemoteDataSource: DMWrapperRemoteDataSource,
    override val factoryContext: DefaultRepositoryFactoryContext
) : DefaultRepositoryImpl(dmWrapperRemoteDataSource, factoryContext.collectionPath), DMWrapperRepository {

    override fun getDMWrappersStream(userId: UserId): Flow<CustomResult<List<DMWrapper>, Exception>> {
        return dmWrapperRemoteDataSource.observeAll()
            .map { dtoListResult ->
                when (dtoListResult) {
                    is CustomResult.Success -> CustomResult.Success(dtoListResult.data.map { (it as DMWrapperDTO).toDomain() })
                    is CustomResult.Failure -> CustomResult.Failure(dtoListResult.error)
                    is CustomResult.Loading -> CustomResult.Loading
                    is CustomResult.Initial -> CustomResult.Initial
                    is CustomResult.Progress -> CustomResult.Progress(dtoListResult.progress)
                }
            }
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is DMWrapper)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type DMWrapper"))

        if (entity.id.isAssigned()) {
            return dmWrapperRemoteDataSource.update(entity.id, entity.getChangedFields())
        } else {
            return dmWrapperRemoteDataSource.create(entity.toDto())
        }
    }

    override suspend fun create(id: DocumentId, entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is DMWrapper)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type DMWrapper"))
        return dmWrapperRemoteDataSource.create(entity.toDto())
    }
}
