package com.example.data.repository

import android.net.Uri
import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.constants.FirestoreConstants
import com.example.core_common.result.resultTry
import com.example.core_common.util.DateTimeUtil
import com.example.core_common.util.MediaUtil
import com.example.data.datasource.remote.UserRemoteDataSource
import com.example.data.model.remote.toDto
import com.example.data.model.remote.UserDTO
import com.example.domain.model.base.ProjectsWrapper
import com.example.domain.model.enum.UserAccountStatus
import com.example.domain.model.enum.UserStatus
import com.example.domain.model.base.User
import com.example.domain.repository.MediaRepository
import com.example.domain.repository.UserRepository
import com.google.type.DateTime
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onErrorResume
import kotlinx.coroutines.flow.flowOf
import java.io.InputStream
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val mediaRepository: MediaRepository // 이미지 처리를 위해 필요
) : UserRepository {

    /**
     * 사용자 ID로 사용자 정보를 실시간 스트림으로 가져옵니다.
     * 
     * @param userId 사용자 ID
     * @return 사용자 정보를 담은 Flow
     */
    // --- New Domain-Port Implementations ---
    // --- Domain Aggregate Operations ---
    override suspend fun delete(userId: String): CustomResult<Unit, Exception> =
        userRemoteDataSource.deleteUser(userId)

    // --- Upsert save ---
    override suspend fun save(user: User): CustomResult<String, Exception>{
        userRemoteDataSource.updateUser(user.toDto())
        return CustomResult.Success(user.uid.value)
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

    /**
     * 사용자 ID로 사용자 정보를 가져옵니다.
     * 
     * @param userId 사용자 ID
     * @return 사용자 정보를 담은 CustomResult
     */
    override suspend fun findById(userId: String): CustomResult<User, Exception> {
        return when (val dtoResult = userRemoteDataSource.fetchUserByIdServer(userId)) {
            is CustomResult.Success -> CustomResult.Success(dtoResult.data.toDomain())
            is CustomResult.Failure -> CustomResult.Failure(dtoResult.error)
            is CustomResult.Loading -> CustomResult.Loading
            is CustomResult.Initial -> CustomResult.Initial
            is CustomResult.Progress -> CustomResult.Progress(dtoResult.progress)
        }
    }

    override fun observe(userId: String): Flow<CustomResult<User, Exception>> {
        return userRemoteDataSource.observeUser(userId).map { result ->
            when (result) {
                is CustomResult.Success -> CustomResult.Success(result.data.toDomain())
                is CustomResult.Failure -> CustomResult.Failure(result.error)
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(result.progress)
            }
        }
    }

}
