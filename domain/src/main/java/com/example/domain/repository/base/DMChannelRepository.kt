package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMChannel
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.DMChannelRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

interface DMChannelRepository : DefaultRepository {
    override val factoryContext: DMChannelRepositoryFactoryContext

    suspend fun findByOtherUserId(otherUserId: String): CustomResult<DMChannel, Exception>
    
    /**
     * 사용자 이름을 통해 DM 채널을 생성합니다.
     *
     * @param targetUserName 대상 사용자 이름
     * @return 성공 시 DM 채널 정보, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun createDMChannel(targetUserName: String): CustomResult<Map<String, Any?>, Exception>
    
    /**
     * DM 채널을 차단합니다.
     *
     * @param channelId 차단할 DM 채널 ID
     * @return 성공 시 차단 결과, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun blockDMChannel(channelId: String): CustomResult<Map<String, Any?>, Exception>
}
