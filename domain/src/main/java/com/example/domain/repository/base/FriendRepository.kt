package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.Friend
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.FriendRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

/**
 * 친구 관계 및 친구 요청 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface FriendRepository: DefaultRepository {
    override val factoryContext: FriendRepositoryFactoryContext

}
