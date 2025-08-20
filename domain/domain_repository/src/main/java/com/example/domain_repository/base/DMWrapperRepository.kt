package com.example.domain_repository.base

import com.example.core_common.result.CustomResult
import com.example.domain.model.base.DMWrapper
import com.example.domain.vo.UserId
import com.example.domain_repository.DefaultRepository

interface DMWrapperRepository : DefaultRepository<DMWrapper> {

    /**
     * 다른 사용자 ID를 통해 DM 래퍼를 찾습니다.
     *
     * @param otherUserId 대상 사용자 ID
     * @return 성공 시 DM 래퍼, 실패 시 Exception을 담은 CustomResult
     */
    suspend fun findByOtherUserId(otherUserId: UserId): CustomResult<DMWrapper, Exception>
}
