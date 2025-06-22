package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.core_common.result.resultTry
import com.example.data.datasource.remote.special.DefaultDatasource
import com.example.data.datasource.remote.special.DefaultDatasourceImpl
import com.example.data.model.remote.DMChannelDTO
import com.example.domain.model.vo.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DM 채널 데이터에 접근하기 위한 인터페이스입니다.
 * DefaultDatasource를 확장하여 기본적인 CRUD 및 관찰 기능을 제공하며,
 * DM 채널 특화된 기능을 추가로 정의합니다.
 */
interface DMChannelRemoteDataSource : DefaultDatasource {
    suspend fun findByParticipants(participants: List<String>): CustomResult<DMChannelDTO, Exception>
}

@Singleton
class DMChannelRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DefaultDatasourceImpl<DMChannelDTO>(firestore, DMChannelDTO::class.java), DMChannelRemoteDataSource {
    override suspend fun findByParticipants(participants: List<String>): CustomResult<DMChannelDTO, Exception> = withContext(Dispatchers.IO) {
        checkCollectionInitialized("findByParticipants")
        resultTry {
            // Firestore에서는 배열 전체 일치 쿼리를 위해 whereEqualTo 사용 (저장 시 정렬 일관성 전제)
            val snapshot = collection
                .whereEqualTo(DMChannelDTO.PARTICIPANTS, participants)
                .limit(1)
                .get()
                .await()
            if (snapshot.isEmpty) throw Exception("DMChannel not found for given participants")
            snapshot.documents.first().toObject(clazz) ?: throw Exception("DMChannel not found for given participants")
        }
    }

}
