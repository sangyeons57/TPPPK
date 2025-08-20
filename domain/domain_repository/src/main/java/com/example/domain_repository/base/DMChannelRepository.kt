package com.example.domain_repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain_repository.DefaultRepository

interface DMChannelRepository : DefaultRepository<DMChannel> {

    suspend fun findByOtherUserId(otherUserId: String): CustomResult<DMChannel, Exception>
    
    /**
     * 사용자 ID를 통해 DM 채널을 생성합니다.
     *
     * @param targetUserId 대상 사용자 ID
     * @return 성공 시 DM 채널 정보, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun createDMChannel(targetUserId: String): CustomResult<Map<String, Any?>, Exception>
    
    /**
     * DM 채널을 차단합니다.
     *
     * @param channelId 차단할 DM 채널 ID
     * @return 성공 시 차단 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun blockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception>
    
    /**
     * DM 채널 차단을 해제합니다.
     *
     * @param channelId 차단 해제할 DM 채널 ID
     * @return 성공 시 차단 해제 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun unblockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception>
    
    /**
     * 사용자 ID를 통해 DM 채널 차단을 해제합니다.
     *
     * @param targetUserId 차단 해제할 대상 사용자 ID
     * @return 성공 시 차단 해제 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun unblockDMChannelByUserId(targetUserId: String): CustomResult<Map<String, Any?>, Exception>
}
