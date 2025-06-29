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
import com.example.domain.model.AggregateRoot
import com.example.domain.model.enum.FriendStatus
import com.example.domain.model.base.Friend
import com.example.domain.model.data.UserSession
import com.example.domain.model.vo.DocumentId
import com.example.domain.repository.factory.context.FriendRepositoryFactoryContext
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
    override val factoryContext: FriendRepositoryFactoryContext
) : DefaultRepositoryImpl(friendRemoteDataSource, factoryContext.collectionPath), FriendRepository {

    /**
     * 현재 사용자에게 온 친구 요청 목록을 실시간 스트림으로 가져옵니다.
     * 
     * @param currentUserId 현재 사용자 ID
     * @return 친구 요청 목록을 담은 Flow
     */
    override fun getFriendRequestsStream(): Flow<CustomResult<List<Friend>, Exception>> {
        return friendRemoteDataSource.observeFriendRequests().map { friendDTOResult ->
            when (friendDTOResult) {
                is CustomResult.Success -> {
                    CustomResult.Success( friendDTOResult.data.map {dto -> dto.toDomain()})
                }

                is CustomResult.Failure -> {
                    CustomResult.Failure(friendDTOResult.error)
                }
                is CustomResult.Loading -> CustomResult.Loading
                is CustomResult.Initial -> CustomResult.Initial
                is CustomResult.Progress -> CustomResult.Progress(friendDTOResult.progress)
            }
        }
    }

    override suspend fun save(entity: AggregateRoot): CustomResult<DocumentId, Exception> {
        if (entity !is Friend)
            return CustomResult.Failure(IllegalArgumentException("Entity must be of type Friend"))
        return if (entity.isNew) {
            friendRemoteDataSource.create(entity.toDto())
        } else {
            friendRemoteDataSource.update(entity.id, entity.getChangedFields())
        }
    }
}
