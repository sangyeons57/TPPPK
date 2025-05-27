package com.example.data._repository

import com.example.core_common.result.resultTry
import com.example.data.datasource._remote.FriendRemoteDataSource
import com.example.data.model._remote.FriendRequestDTO // 이 DTO 파일이 존재해야 합니다.
import com.example.data.model.mapper.toDomain // FriendRequestDTO -> FriendRequest 매퍼 함수
import com.example.domain.model.FriendRequest
import com.example.domain._repository.FriendRequestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.Result

class FriendRequestRepositoryImpl @Inject constructor(
    private val friendRemoteDataSource: FriendRemoteDataSource
    // private val friendRequestMapper: FriendRequestMapper // 개별 매퍼 사용시
) : FriendRequestRepository {

    override fun getSentFriendRequestsStream(currentUserId: String): Flow<Result<List<FriendRequest>>> {
        // FriendRemoteDataSource에 observeSentFriendRequests(senderId: String) 함수 필요
        return friendRemoteDataSource.observeSentFriendRequests(currentUserId).map { result ->
            result.mapCatching { dtoList ->
                dtoList.map { it.toDomain() }
            }
        }
    }

    override suspend fun cancelFriendRequest(requestId: String, currentUserId: String): Result<Unit> = resultTry {
        // FriendRemoteDataSource에 cancelFriendRequest(requestId: String) 함수 필요
        // currentUserId는 DataSource 또는 Firestore 규칙에서 권한 확인에 사용될 수 있음
        friendRemoteDataSource.cancelFriendRequest(requestId).getOrThrow()
    }

    override suspend fun getFriendRequestDetails(requestId: String): Result<FriendRequest> = resultTry {
        // FriendRemoteDataSource에 getFriendRequest(requestId: String) 함수 필요
        friendRemoteDataSource.getFriendRequest(requestId).getOrThrow().toDomain()
    }
}
