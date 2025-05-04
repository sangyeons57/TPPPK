package com.example.data.datasource.local.dm

import com.example.domain.model.DmConversation
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DM 관련 로컬 데이터 소스 인터페이스
 * DM 대화 목록 관련 Room 데이터베이스 작업을 정의합니다.
 */
interface DmLocalDataSource {
    /**
     * DM 대화 목록 스트림을 가져옵니다.
     * @return DM 대화 목록의 Flow
     */
    fun getDmConversationsStream(): Flow<List<DmConversation>>
    
    /**
     * DM 대화 목록을 로컬 데이터베이스에 저장합니다.
     * @param dmConversations 저장할 DM 대화 목록
     */
    suspend fun saveDmConversations(dmConversations: List<DmConversation>)
    
    /**
     * 특정 DM 대화 정보를 가져옵니다.
     * @param dmId DM 채널 ID
     * @return DM 대화 정보 또는 null
     */
    suspend fun getDmConversationById(dmId: String): DmConversation?
    
    /**
     * 특정 DM 대화 정보를 저장합니다.
     * @param dmConversation 저장할 DM 대화 정보
     */
    suspend fun saveDmConversation(dmConversation: DmConversation)
    
    /**
     * 특정 DM 대화 정보를 삭제합니다.
     * @param dmId 삭제할 DM 채널 ID
     */
    suspend fun deleteDmConversation(dmId: String)
    
    /**
     * 모든 DM 대화 정보를 삭제합니다.
     */
    suspend fun deleteAllDmConversations()
    
    /**
     * 마지막 메시지를 업데이트합니다.
     * @param dmId DM 채널 ID
     * @param message 메시지 내용
     * @param timestamp 메시지 타임스탬프
     */
    suspend fun updateLastMessage(dmId: String, message: String, timestamp: LocalDateTime)
} 