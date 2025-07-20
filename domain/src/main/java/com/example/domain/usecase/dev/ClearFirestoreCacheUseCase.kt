package com.example.domain.usecase.dev

import com.example.core_common.result.CustomResult
import javax.inject.Inject

/**
 * Firestore 캐시 정보를 확인하고 처리하는 UseCase
 * 실제로는 Firestore가 네이티브 캐싱을 사용하므로 정보 제공 목적
 */
class ClearFirestoreCacheUseCase @Inject constructor() {
    
    /**
     * Firestore 캐시 상태 정보를 반환합니다.
     * Firestore는 네이티브 캐싱을 사용하므로 수동 캐시 삭제가 불필요함을 알려줍니다.
     * 
     * @return 캐시 상태 정보 메시지
     */
    suspend operator fun invoke(): CustomResult<String, Exception> {
        return try {
            val message = "정보: Firestore는 네이티브 캐싱을 사용합니다. 수동 캐시 삭제가 필요하지 않습니다."
            CustomResult.Success(message)
        } catch (e: Exception) {
            CustomResult.Failure(Exception("캐시 정보 확인 중 오류가 발생했습니다: ${e.message}"))
        }
    }
}