package com.example.data.repository.base

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.core_common.util.DateTimeUtil
import com.example.data.datasource.remote.FriendRemoteDataSource
import com.example.data.datasource.remote.UserRemoteDataSource // 사용자 검색 및 정보 업데이트 시 필요
import com.example.data.model.remote.FriendDTO
import com.example.data.model.remote.toDto
import com.example.data.repository.DefaultRepositoryImpl
import com.example.domain.event.AggregateRoot
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.base.Friend
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.DefaultRepositoryFactoryContext
import com.example.domain.repository.base.FriendRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 친구 관련 기능을 제공하는 Repository 구현체
 * Firebase를 사용하여 친구 관계 데이터를 관리합니다.
 */
class FriendRepositoryImpl @Inject constructor(
    private val friendRemoteDataSource: FriendRemoteDataSource,
    override val factoryContext: DefaultRepositoryFactoryContext
) : DefaultRepositoryImpl(friendRemoteDataSource, factoryContext.collectionPath), FriendRepository {

    /**
     * 현재 사용자의 친구 목록을 실시간 스트림으로 가져옵니다.
     * 
     * @param currentUserId 현재 사용자 ID
     * @return 친구 목록을 담은 Flow
     */
    override fun getFriendsStream(currentUserId: String): Flow<CustomResult<List<Friend>, Exception>> {
        Log.d("FriendRepositoryImpl", "currentUserId: $currentUserId")
        return friendRemoteDataSource.observeFriends(currentUserId).map { result ->
            when (result) {
                is CustomResult.Success -> {
                    try {
                        Log.d("FriendRepositoryImpl", "getFriendsStream: $result")
                        val friends = result.data.map { dto ->
                            Log.d("FriendRepositoryImpl", "getFriendsStream: $dto")
                            // Use toDomain() extension function instead of manual mapping
                            dto.toDomain()
                        }
                        CustomResult.Success(friends)
                    } catch (e: Exception) {
                        CustomResult.Failure(e)
                    }
                }
                is CustomResult.Failure -> {
                    CustomResult.Failure(result.error)
                }
                else -> {
                    CustomResult.Failure(Exception("Unknown error in getFriendsStream"))
                }
            }
        }
    }

    /**
     * 현재 사용자에게 온 친구 요청 목록을 실시간 스트림으로 가져옵니다.
     * 
     * @param currentUserId 현재 사용자 ID
     * @return 친구 요청 목록을 담은 Flow
     */
    override fun getFriendRequestsStream(currentUserId: String): Flow<CustomResult<List<Friend>, Exception>> {
        return friendRemoteDataSource.observeFriendRequests(currentUserId).map { friendDTOResult ->
            when (friendDTOResult) {
                is CustomResult.Success -> {
                    CustomResult.Success( friendDTOResult.data.map {dto -> dto.toDomain()})
                }

                is CustomResult.Failure -> {
                    CustomResult.Failure(friendDTOResult.error)
                }

                else -> {
                    CustomResult.Failure(Exception("Unknown error in getFriendRequestsStream"))
                }
            }
        }
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Friend)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Friend"))
        return if (entity.id.isAssigned()) {
            friendRemoteDataSource.update(entity.id, entity.getChangedFields())
        } else {
            friendRemoteDataSource.create(entity.toDto())
        }
    }

}
