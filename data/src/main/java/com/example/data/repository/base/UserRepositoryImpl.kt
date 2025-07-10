package com.example.data.repository.base

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.datasource.remote.special.FunctionsRemoteDataSource
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserName
import com.example.domain.repository.base.UserRepository
import com.example.domain.repository.factory.context.UserRepositoryFactoryContext
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
    override val factoryContext: UserRepositoryFactoryContext,
) : DefaultRepositoryImpl(userRemoteDataSource, factoryContext), UserRepository {

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is User)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type User"))
        ensureCollection()
        return if(entity.isNew) {
            userRemoteDataSource.create(entity.toDto())
        } else {
            userRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }

    override fun observeByName(name: UserName): Flow<CustomResult<User, Exception>> {
        ensureCollection()
        return userRemoteDataSource.findByNameStream(name.value).map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.toDomain())
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }

    override fun observeByEmail(email: String): Flow<CustomResult<User, Exception>> {
        ensureCollection()
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

    override fun observeAllByName(name: String, limit: Int): Flow<CustomResult<List<User>, Exception>> {
        ensureCollection()
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

    override suspend fun uploadProfileImage(uri: Uri): CustomResult<Unit, Exception> {
        return functionsRemoteDataSource.uploadUserProfileImage(uri)
    }

    override suspend fun removeProfileImage(): CustomResult<Unit, Exception> {
        return functionsRemoteDataSource.removeUserProfileImage()
    }

    override suspend fun updateProfile(name: String?, memo: String?): CustomResult<Unit, Exception> {
        return when (val result = functionsRemoteDataSource.updateUserProfile(name, memo)) {
            is CustomResult.Success -> CustomResult.Success(Unit)
            is CustomResult.Failure -> CustomResult.Failure(result.error)
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Progress -> CustomResult.Progress(result.progress)
        }
    }

    override suspend fun callFunction(
        functionName: String,
        data: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.callFunction(functionName, data)
    }

    override suspend fun getHelloWorld(): CustomResult<String, Exception> {
        return functionsRemoteDataSource.getHelloWorld()
    }

    override suspend fun callFunctionWithUserData(
        functionName: String,
        userId: String,
        customData: Map<String, Any?>?
    ): CustomResult<Map<String, Any?>, Exception> {
        return functionsRemoteDataSource.callFunctionWithUserData(functionName, userId, customData)
    }

}