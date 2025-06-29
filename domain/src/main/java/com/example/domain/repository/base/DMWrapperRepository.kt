package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMWrapper
import com.example.domain.model.vo.UserId
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.DMWrapperRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

interface DMWrapperRepository : DefaultRepository {
    override val factoryContext: DMWrapperRepositoryFactoryContext

    /**
     * 현재 사용자의 모든 DM 채널 정보를 스트림으로 가져옵니다.
     * DMWrapper는 다른 사용자와의 DM 채널, 마지막 메시지, 안 읽은 메시지 수 등을 포함합니다.
     */
    fun getDMWrappersStream(userId: UserId): Flow<CustomResult<List<DMWrapper>, Exception>>

}
