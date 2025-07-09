package com.example.data.service

import com.example.core_common.result.CustomResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 캐시 관리 관련 서비스를 제공하는 클래스
 * 로그아웃이나 기타 상황에서 캐시를 정리하는 기능을 담당합니다.
 */
interface CacheService {
    /**
     * Firestore 로컬 캐시를 완전히 삭제합니다.
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend fun clearFirestoreCache(): CustomResult<Unit, Exception>

    /**
     * 모든 캐시를 정리합니다.
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend fun clearAllCache(): CustomResult<Unit, Exception>
}

@Singleton
class CacheServiceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CacheService {

    override suspend fun clearFirestoreCache(): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                // 현재 Firestore 인스턴스 종료
                firestore.terminate().await()
                
                // 로컬 캐시 삭제
                firestore.clearPersistence().await()
                
                // 새 인스턴스도 캐시 삭제 (안전성을 위해)
                FirebaseFirestore.getInstance().clearPersistence()
                
                CustomResult.Success(Unit)
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }

    override suspend fun clearAllCache(): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                // Firestore 캐시 삭제
                when (val result = clearFirestoreCache()) {
                    is CustomResult.Success -> {
                        // 추가적인 캐시 정리 작업이 필요하면 여기서 수행
                        // 예: SharedPreferences, DataStore, 기타 로컬 데이터
                        
                        CustomResult.Success(Unit)
                    }
                    is CustomResult.Failure -> result
                    else -> CustomResult.Failure(Exception("Unknown error occurred during cache clearing"))
                }
            } catch (e: Exception) {
                CustomResult.Failure(e)
            }
        }
    }
} 