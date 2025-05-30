
package com.example.data.datasource.remote

import com.example.core_common.result.CustomResult
import com.example.data.model.remote.DMChannelDTO
import kotlinx.coroutines.flow.Flow

interface DMChannelRemoteDataSource {
    /**
     * 특정 DM 채널의 메타데이터를 실시간으로 관찰합니다.
     * @param channelId 관찰할 채널의 ID
     */
    fun observeDMChannel(channelId: String): Flow<DMChannelDTO?>

    /**
     * 두 사용자 간의 DM 채널을 생성하거나, 이미 존재하면 해당 채널 ID를 반환합니다.
     * @param otherUserId 대화를 시작할 상대방 사용자의 ID
     * @return 생성되거나 기존에 있던 채널의 ID를 포함한 Result 객체
     */
    suspend fun findOrCreateDMChannel(otherUserId: String): CustomResult<String, Exception>

    /**
     * DM 채널의 마지막 메시지 정보를 업데이트합니다.
     * @param channelId 업데이트할 채널의 ID
     * @param messagePreview 마지막 메시지 내용 미리보기
     */
    suspend fun updateLastMessage(channelId: String, messagePreview: String): CustomResult<Unit, Exception>
}

