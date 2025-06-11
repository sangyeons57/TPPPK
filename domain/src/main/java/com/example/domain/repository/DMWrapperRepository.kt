package com.example.domain.repository

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMWrapper
import kotlinx.coroutines.flow.Flow

interface DMWrapperRepository {
    /**
     * 현재 사용자의 모든 DM 채널 정보를 스트림으로 가져옵니다.
     * DMWrapper는 다른 사용자와의 DM 채널, 마지막 메시지, 안 읽은 메시지 수 등을 포함합니다.
     */
    fun getDMWrappersStream(userId: String): Flow<CustomResult<List<DMWrapper>, Exception>>

    /**
     * 특정 DM 채널 ID에 해당하는 DMWrapper 정보를 스트림으로 가져옵니다.
     */
    fun getDMWrapperStream(currentUserId: String, dmChannelId: String): Flow<CustomResult<DMWrapper, Exception>>

    /**
     * 새로운 DMWrapper를 생성합니다.
     *
     * @param dmChannelId 연결된 DM 채널의 ID
     */
    suspend fun createDMWrapper(userId: String, dmChannelId: String, otherUserId: String): CustomResult<String, Exception>

    /**
     * 특정 사용자와의 기존 DM 채널 ID를 찾습니다.
     *
     * @param otherUserId 상대방 사용자의 ID
     * @return DM 채널 ID (찾은 경우), null (찾지 못한 경우), 또는 오류
     */
    suspend fun findDmChannelIdWithUser(currentUserId: String, otherUserId: String): CustomResult<String, Exception>
}
