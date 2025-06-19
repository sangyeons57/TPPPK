
package com.example.data.datasource.remote


import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.FriendDTO
import com.example.domain.model.enum.FriendStatus
import com.google.firebase.Timestamp
import com.example.core_common.constants.FirestorePaths
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 간의 친구 관계 데이터에 접근하기 위한 인터페이스입니다.
 * DefaultDatasource를 확장하여 특정 사용자의 친구 목록 내 개별 친구 문서에 대한 CRUD 및 관찰 기능을 제공합니다.
 * 친구 데이터는 `users/{userId}/friends/{friendUid}` 경로에 저장되므로,
 * 대부분의 작업 전에 `setCollection(userId)`를 호출하여 주 사용자 컨텍스트를 설정해야 합니다.
 * 일부 작업(예: 친구 요청, 수락)은 두 사용자의 데이터에 영향을 미칠 수 있으며, 구현 시 이를 고려해야 합니다.
 */
interface FriendRemoteDataSource : DefaultDatasource<FriendDTO> {


    /**
     * 특정 사용자에게 온 친구 요청("pending" 상태) 목록을 실시간으로 관찰합니다.
     * @param userId 조회할 사용자의 ID
     */
    fun observeFriendRequests(userId: String): Flow<CustomResult<List<FriendDTO>, Exception>>
}

@Singleton
class FriendRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<FriendDTO>(firestore, FriendDTO::class.java), FriendRemoteDataSource {

        override fun observeFriendRequests(userId: String): Flow<CustomResult<List<FriendDTO>, Exception>> {
        setCollection(FirestorePaths.userDoc(userId), FriendDTO.COLLECTION_NAME)
        return callbackFlow {
            val listener = collection
                .whereEqualTo(FriendDTO.STATUS, FriendStatus.PENDING.name)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { trySend(CustomResult.Failure(error)); close(error); return@addSnapshotListener }
                    val list = snapshot?.documents?.mapNotNull { it.toObject(FriendDTO::class.java) } ?: emptyList()
                    trySend(CustomResult.Success(list))
                }
            awaitClose { listener.remove() }
        }.flowOn(Dispatchers.IO)
    }
}

