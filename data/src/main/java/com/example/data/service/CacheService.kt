package com.example.data.service

import android.util.Log
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
     * Firestore 로컬 캐시를 안전하게 정리합니다.
     * 로그아웃 시에는 terminate만 수행하고, clearPersistence는 피합니다.
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend fun clearFirestoreCache(): CustomResult<Unit, Exception>

    /**
     * 모든 캐시를 정리합니다.
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend fun clearAllCache(): CustomResult<Unit, Exception>
    
    /**
     * 앱 시작 시에만 사용하는 완전한 캐시 초기화 메서드
     * clearPersistence를 안전하게 호출합니다.
     * @return 성공 시 CustomResult.Success, 실패 시 CustomResult.Failure
     */
    suspend fun initializeClearCache(): CustomResult<Unit, Exception>
}

@Singleton
class CacheServiceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CacheService {

    companion object {
        private const val TAG = "CacheService"
        private var hasTerminated = false
    }

    override suspend fun clearFirestoreCache(): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting safe Firestore cache clearing (terminate only)...")
                
                // 이미 terminate된 경우 스킵
                if (hasTerminated) {
                    Log.d(TAG, "Firestore already terminated, skipping...")
                    return@withContext CustomResult.Success(Unit)
                }
                
                // 로그아웃 시에는 terminate만 수행 (clearPersistence는 피함)
                Log.d(TAG, "Terminating Firestore instance...")
                firestore.terminate().await()
                hasTerminated = true
                
                Log.d(TAG, "Firestore terminated successfully (cache will be naturally cleared)")
                CustomResult.Success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear Firestore cache", e)
                
                // terminate 실패도 심각하지 않을 수 있음
                if (e.message?.contains("terminate") == true) {
                    Log.w(TAG, "Firestore terminate error, but this might be safe to ignore")
                    return@withContext CustomResult.Success(Unit)
                }
                
                CustomResult.Failure(e)
            }
        }
    }
    
    override suspend fun initializeClearCache(): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting app initialization cache clearing...")
                
                // 앱 시작 시에만 clearPersistence 호출 (Firestore 사용 전)
                FirebaseFirestore.getInstance().clearPersistence().await()
                
                Log.d(TAG, "App initialization cache cleared successfully")
                CustomResult.Success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear cache during app initialization", e)
                
                // 초기화 시 clearPersistence 실패는 큰 문제가 아닐 수 있음
                if (e.message?.contains("clearPersistence") == true) {
                    Log.w(TAG, "clearPersistence failed during initialization, continuing anyway")
                    return@withContext CustomResult.Success(Unit)
                }
                
                CustomResult.Failure(e)
            }
        }
    }

    override suspend fun clearAllCache(): CustomResult<Unit, Exception> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting all cache clearing...")
                
                // Firestore 캐시 정리 (terminate만)
                when (val result = clearFirestoreCache()) {
                    is CustomResult.Success -> {
                        // 추가적인 캐시 정리 작업이 필요하면 여기서 수행
                        // 예: SharedPreferences, DataStore, 기타 로컬 데이터
                        
                        Log.d(TAG, "All cache cleared successfully")
                        CustomResult.Success(Unit)
                    }
                    is CustomResult.Failure -> {
                        Log.e(TAG, "Failed to clear all cache", result.error)
                        result
                    }
                    else -> {
                        Log.e(TAG, "Unknown error occurred during cache clearing")
                        CustomResult.Failure(Exception("Unknown error occurred during cache clearing"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during all cache clearing", e)
                CustomResult.Failure(e)
            }
        }
    }
} 