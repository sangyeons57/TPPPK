package com.example.data.repository.base

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.model.remote.UserDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.event.AggregateRoot
import com.example.domain.model.base.User
import com.example.domain.model.vo.CollectionPath
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.RepositoryFactoryContext
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    override val factoryContext: UserRepositoryFactoryContext,
) : DefaultRepositoryImpl(userRemoteDataSource, factoryContext.collectionPath), UserRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is User)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type User"))

        return if(entity.id.isAssigned()) {
            userRemoteDataSource.create(entity.toDto())
        } else {
            userRemoteDataSource.update(entity.id, entity.getChangedFields())
            CustomResult.Success(entity.id)
        }
    }

    override fun findByNameStream(name: String): Flow<CustomResult<User, Exception>> {
        return userRemoteDataSource.findByNameStream(name).map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.toDomain())
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }

    override fun findByEmailStream(email: String): Flow<CustomResult<User, Exception>> {
        return userRemoteDataSource.findByNameStream(email).map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.toDomain())
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }

    override fun findAllByNameStream(name: String, limit: Int): Flow<CustomResult<List<User>, Exception>> {
        return userRemoteDataSource.findAllByNameStream(name, limit).map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.map { it.toDomain() })
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }
}