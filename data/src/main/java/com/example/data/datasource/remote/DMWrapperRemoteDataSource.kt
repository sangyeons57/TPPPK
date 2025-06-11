package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult // Added for CustomResult
import com.example.data.model.remote.DMWrapperDTO
import kotlinx.coroutines.flow.Flow

interface DMWrapperRemoteDataSource {

    /**
     * 현재 로그인한 사용자의 DM 채널 요약 정보 목록을 실시간으로 관찰합니다.
     * 목록은 마지막 메시지 시간 순서대로 정렬됩니다.
     */
    fun observeDmWrappers(): Flow<List<DMWrapperDTO>> // This might need userId if not implicitly handled by auth state
    
    /**
     * 특정 사용자의 DM 채널 요약 정보 목록을 실시간으로 관찰합니다.
     * 목록은 마지막 메시지 시간 순서대로 정렬됩니다.
     * 
     * @param userId 조회할 사용자의 ID
     */
    fun observeDmWrappers(userId: String): Flow<List<DMWrapperDTO>>

    /**
     * 새로운 DMWrapper 문서를 생성합니다.
     * @param dmWrapperDto 생성할 DMWrapper 데이터
     * @return 생성된 DMWrapper 문서의 ID 또는 오류
     */
    suspend fun createDMWrapper(userId: String, dmWrapperDto: DMWrapperDTO): CustomResult<String, Exception>

    /**
     * 정확히 일치하는 참여자들을 가진 DMWrapper를 찾습니다.
     * @param participantIds 참여자 ID 목록 (정렬된 상태여야 함)
     * @return DMWrapperDTO 또는 null (찾지 못한 경우), 또는 오류
     */
    suspend fun findDMWrapperByExactParticipants(userId: String, otherUserId: String): CustomResult<DMWrapperDTO, Exception>
}
