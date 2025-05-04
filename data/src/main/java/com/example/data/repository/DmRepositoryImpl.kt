// 경로: data/repository/DmRepositoryImpl.kt
package com.example.data.repository

import com.example.data.datasource.local.dm.DmLocalDataSource
import com.example.data.datasource.remote.dm.DmRemoteDataSource
import com.example.domain.model.DmConversation
import com.example.domain.repository.DmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.Result

/**
 * DmRepository 인터페이스 구현
 * 로컬 및 원격 데이터 소스를 활용하여 DM 관련 기능을 제공합니다.
 */
class DmRepositoryImpl @Inject constructor(
    private val remoteDataSource: DmRemoteDataSource,
    private val localDataSource: DmLocalDataSource
) : DmRepository {

    /**
     * DM 목록 실시간 스트림
     * 서버에서 새로운 DM 목록이 동기화되면 로컬에도 저장합니다.
     */
    override fun getDmListStream(): Flow<List<DmConversation>> {
        // 원격 데이터를 가져와서 로컬에 캐싱
        return remoteDataSource.getDmListStream().onEach { dmConversations ->
            // 원격에서 데이터가 변경되면 로컬에 저장
            localDataSource.saveDmConversations(dmConversations)
        }
    }

    /**
     * DM 목록 새로고침 (API 호출)
     * 명시적으로 서버에서 DM 목록을 가져옵니다.
     */
    override suspend fun fetchDmList(): Result<Unit> {
        return remoteDataSource.fetchDmList()
    }

    /**
     * 새 DM 채널 생성
     * @param otherUserId 대화 상대방 사용자 ID
     * @return 생성된 DM 채널 ID
     */
    override suspend fun createDmChannel(otherUserId: String): Result<String> {
        val result = remoteDataSource.createDmChannel(otherUserId)
        
        // 성공 시 채널 정보를 로컬에서 조회하여 업데이트
        if (result.isSuccess) {
            val dmId = result.getOrNull()
            dmId?.let {
                // 첫 번째 값만 가져와서 처리
                val dmList = remoteDataSource.getDmListStream().firstOrNull() ?: emptyList()
                val newDm = dmList.find { dm -> dm.channelId == dmId }
                newDm?.let { dm -> localDataSource.saveDmConversation(dm) }
            }
        }
        
        return result
    }

    /**
     * DM 채널 삭제
     * @param dmId 삭제할 DM 채널 ID
     * @return 작업 성공 여부
     */
    override suspend fun deleteDmChannel(dmId: String): Result<Unit> {
        val result = remoteDataSource.deleteDmChannel(dmId)
        
        // 성공 시 로컬 데이터에서도 삭제
        if (result.isSuccess) {
            localDataSource.deleteDmConversation(dmId)
        }
        
        return result
    }
}