package com.example.data.datasource.remote.dm

import com.example.domain.model.DmConversation
import kotlinx.coroutines.flow.Flow

/**
 * DM(Direct Message) 관련 원격 데이터 소스 인터페이스
 * DM 채팅방 목록 및 관리 관련 Firebase Firestore 작업을 정의합니다.
 */
interface DmRemoteDataSource {
    /**
     * DM 목록 실시간 스트림을 가져옵니다.
     * @return DM 목록의 Flow
     */
    fun getDmListStream(): Flow<List<DmConversation>>
    
    /**
     * DM 목록을 Firestore에서 가져옵니다.
     * @return 작업 성공 여부
     */
    suspend fun fetchDmList(): Result<Unit>
    
    /**
     * 새 DM 채널을 생성합니다.
     * @param otherUserId 대화 상대 사용자 ID
     * @return 생성된 DM 채널 ID
     */
    suspend fun createDmChannel(otherUserId: String): Result<String>
    
    /**
     * DM 채널을 삭제합니다.
     * @param dmId 삭제할 DM 채널 ID
     * @return 작업 성공 여부
     */
    suspend fun deleteDmChannel(dmId: String): Result<Unit>
} 