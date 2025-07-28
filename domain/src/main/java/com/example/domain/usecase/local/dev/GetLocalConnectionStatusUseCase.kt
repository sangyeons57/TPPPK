package com.example.domain.usecase.local.dev

import com.example.core_common.result.CustomResult
import com.example.domain.model.data.UserSession
import com.example.domain.repository.local.AuthLocalRepository
import javax.inject.Inject

interface GetLocalConnectionStatusUseCase {
    suspend operator fun invoke(): CustomResult<UserSession, Exception>
    suspend fun isLocalDataAvailable(): Boolean
}

class GetLocalConnectionStatusUseCaseImpl @Inject constructor(
    private val authLocalRepository: AuthLocalRepository
) : GetLocalConnectionStatusUseCase {

    override suspend operator fun invoke(): CustomResult<UserSession, Exception> {
        return TODO("로컬 저장소에서 연결 상태 확인을 위한 사용자 세션 정보 조회")
    }

    override suspend fun isLocalDataAvailable(): Boolean {
        return TODO("로컬 저장소에 데이터가 있는지 확인")
    }
} 