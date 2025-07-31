package com.example.data_repository.base

import android.net.Uri
import com.example.core_common.result.CustomResult
import com.example.data_datasource.remote.UserRemoteDataSource
import com.example.data_datasource.remote.special.FunctionsRemoteDataSource
import com.example.data_model.remote.UserDTO
import com.example.data_repository.DefaultRepositoryImpl
import com.example.domain.model.AggregateRoot
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.model.vo.user.UserName
import com.example.domain_repository.base.UserRepository
import com.example.mapper.user.UserMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val functionsRemoteDataSource: FunctionsRemoteDataSource,
    userMapper: UserMapper,
) : DefaultRepositoryImpl<User, UserDTO>(userRemoteDataSource, userMapper), UserRepository {

    override suspend fun save(entity: User): CustomResult<DocumentId, Exception> {
        ensureCollection()
        return if(entity.isNew) {
            userRemoteDataSource.create(mapper.domainToDto(entity))
        } else {
            userRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }

    override fun observeByName(name: UserName): Flow<CustomResult<User, Exception>> {
        ensureCollection()
        return userRemoteDataSource.findByNameStream(name.value).map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(mapper.dtoToDomain(result.data))
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
                is CustomResult.Success -> CustomResult.Success(mapper.dtoToDomain(result.data))
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
                is CustomResult.Success -> CustomResult.Success(result.data.map {
                    mapper.dtoToDomain(
                        it
                    )
                })
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

    override suspend fun sendFcmTestNotification(
        userId: String,
        channelId: String
    ): CustomResult<Map<String, Any?>, Exception> {
        val title = "FCM 테스트 알림"
        val body = "채널 입장 테스트 알림 (채널ID: $channelId)"
        val data = mapOf(
            "type" to "mention",
            "channelId" to channelId
        )
        return functionsRemoteDataSource.callFunction(
            "sendCustomNotification",
            mapOf(
                "userId" to userId,
                "title" to title,
                "body" to body,
                "data" to data
            )
        )
    }

    override fun observeUserUpdatedAt(userId: String): Flow<CustomResult<Long, Exception>> {
        return userRemoteDataSource.observeUserUpdatedAt(userId)
    }

    override fun observeUsers(userIds: List<String>): Flow<CustomResult<List<User>, Exception>> {
        ensureCollection()
        return userRemoteDataSource.observeUsers(userIds).map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.map {
                    mapper.dtoToDomain(
                        it
                    )
                })
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }
}