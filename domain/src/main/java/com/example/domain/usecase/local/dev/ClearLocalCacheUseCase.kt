package com.example.domain.usecase.local.dev

import com.example.core_common.result.CustomResult
import com.example.domain.repository.local.CacheLocalRepository
import javax.inject.Inject

interface ClearLocalCacheUseCase {
    suspend operator fun invoke(): CustomResult<String, Exception>
}

class ClearLocalCacheUseCaseImpl @Inject constructor(
    private val cacheLocalRepository: CacheLocalRepository
) : ClearLocalCacheUseCase {

    override suspend operator fun invoke(): CustomResult<String, Exception> {
        return TODO("로컬 저장소의 모든 캐시 데이터를 정리하고 결과 메시지 반환")
    }
} 