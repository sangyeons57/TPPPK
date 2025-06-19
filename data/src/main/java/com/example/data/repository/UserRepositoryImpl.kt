package com.example.data.repository

import com.example.core_common.result.CustomResult
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.model.remote.UserDTO
import com.example.domain.model.base.User
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.MediaRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val mediaRepository: MediaRepository // 이미지 처리를 위해 필요
) : UserRepository {

    /**
     * 사용자 ID로 사용자 정보를 가져옵니다.
     * 
     * @param userId 사용자 ID
     * @return 사용자 정보를 담은 CustomResult
     */
    override suspend fun findById(userId: DocumentId): CustomResult<User, Exception> {
        return when (val dtoResult = userRemoteDataSource.findById(userId)) {
            is CustomResult.Success -> CustomResult.Success(dtoResult.data.toDomain())
            is CustomResult.Failure -> CustomResult.Failure(dtoResult.error)
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Progress -> CustomResult.Progress(dtoResult.progress)
        }
    }

    override fun observe(userId: DocumentId): Flow<CustomResult<User, Exception>> {
        return userRemoteDataSource.observe(userId).map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.toDomain())
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }

    override suspend fun delete(userId: DocumentId): CustomResult<Unit, Exception> =
        userRemoteDataSource.delete(userId)

    // --- Upsert save ---
    override suspend fun save(user: User): CustomResult<DocumentId, Exception>{
        return userRemoteDataSource.update(user)
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