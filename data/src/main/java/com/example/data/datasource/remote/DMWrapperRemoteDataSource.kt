package com.example.data.datasource.remote


import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.DMWrapperDTO
import com.example.domain.model.vo.DocumentId
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
 * 사용자별 DM 채널 요약 정보(DMWrapper) 데이터에 접근하기 위한 인터페이스입니다.
 * DefaultDatasource를 확장하여 기본적인 CRUD 및 단일 문서 관찰 기능을 제공합니다.
 * DMWrapper는 `users/{userId}/dm_wrappers/{dmChannelId}` 경로에 저장되므로,
 * 모든 작업 전에 `setCollection(userId)`를 호출하여 사용자 컨텍스트를 설정해야 합니다.
 */
interface DMWrapperRemoteDataSource : DefaultDatasource<DMWrapperDTO> {
    /**
     * 특정 사용자의 DMWrapper 중에서 지정된 상대방 사용자 ID(`otherUserId`)를 가진 문서를 찾습니다.
     * **중요:** 이 메서드를 호출하기 전에 `setCollection(userId)`를 통해 사용자 컨텍스트를 설정해야 합니다.
     * @param userId 문서를 검색할 주 사용자의 ID. `setCollection`에 전달된 ID와 일치해야 합니다.
     * @param otherUserId 찾고자 하는 DM 상대방의 사용자 ID.
     * @return DMWrapperDTO 또는 오류를 포함한 CustomResult. 찾지 못한 경우 성공 결과에 null 또는 특정 예외를 포함할 수 있습니다.
     * @throws IllegalStateException setCollection(userId)가 호출되지 않은 경우.
     */
    suspend fun findByOtherUserId(userId: String, otherUserId: String): CustomResult<DMWrapperDTO, Exception>
}

@Singleton
class DMWrapperRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<DMWrapperDTO>(firestore, DMWrapperDTO::class.java), DMWrapperRemoteDataSource {

        override suspend fun findByOtherUserId(userId: String, otherUserId: String): CustomResult<DMWrapperDTO, Exception> {
        setCollection(FirestorePaths.userDoc(userId), DMWrapperDTO.COLLECTION_NAME)
        return withContext(Dispatchers.IO) {
            resultTry {
                val snap = collection
                    .whereEqualTo(DMWrapperDTO.OTHER_USER_ID, otherUserId)
                    .limit(1)
                    .get().await()
                if (snap.isEmpty) throw Exception("DMWrapper not found")
                snap.documents.first().toObject(DMWrapperDTO::class.java) ?: throw Exception("Parse error")
            }
        }
    }
}
