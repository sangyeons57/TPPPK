package com.example.domain.repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.enum.InviteStatus
import com.example.domain.model.base.Invite
import com.example.domain.repository.DefaultRepository
import com.example.domain.repository.factory.context.InviteRepositoryFactoryContext
import kotlinx.coroutines.flow.Flow

/**
 * 초대 코드 생성, 조회, 사용 등 관련 데이터 처리를 위한 인터페이스입니다.
 */
interface InviteRepository : DefaultRepository {
    override val factoryContext: InviteRepositoryFactoryContext

}
