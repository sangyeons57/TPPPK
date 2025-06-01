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
    fun getDMWrapperStream(dmChannelId: String): Flow<CustomResult<DMWrapper, Exception>>


    fun createDmChannel(otherUserId: String): CustomResult<String, Exception>

    fun findDmChannelWithUser(otherUserId: String): CustomResult<String, Exception>
}
