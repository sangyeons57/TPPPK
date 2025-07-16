package com.example.feature_home.viewmodel.service

import android.util.Log
import com.example.core_common.result.CustomResult
import com.example.domain.model.base.User
import com.example.domain.model.vo.UserId
import com.example.domain.provider.user.UserUseCases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * 사용자 데이터 로딩을 담당하는 Service
 * Domain UseCase들을 조합하여 UI에 특화된 사용자 데이터를 제공합니다.
 */
class LoadUserDataService(
    private val userUseCases: UserUseCases
) {
    
    data class UserData(
        val userId: UserId,
        val userInitial: String,
        val userProfileImageUrl: String?
    )
    
    /**
     * 현재 사용자 정보를 UI에 최적화된 형태로 스트림 제공
     */
    suspend fun getCurrentUserStream(): Flow<CustomResult<UserData, Exception>> {
        Log.d("LoadUserDataService", "Starting to collect current user stream")
        
        return userUseCases.getCurrentUserStreamUseCase()
            .map { result ->
                when (result) {
                    is CustomResult.Success -> {
                        val user = result.data
                        Log.d("LoadUserDataService", "User received: ${user.id}")
                        val userData = UserData(
                            userId = UserId.from(user.id),
                            userInitial = user.name.value.firstOrNull()?.toString() ?: "U",
                            userProfileImageUrl = null // 프로필 이미지는 고정 경로로 로딩
                        )
                        CustomResult.Success(userData)
                    }
                    
                    is CustomResult.Failure -> {
                        Log.e("LoadUserDataService", "Failed to get current user", result.error)
                        CustomResult.Failure(result.error)
                    }
                    
                    is CustomResult.Loading -> {
                        Log.d("LoadUserDataService", "Loading user data...")
                        CustomResult.Loading
                    }
                    
                    is CustomResult.Initial -> {
                        Log.d("LoadUserDataService", "Initial state")
                        CustomResult.Loading
                    }
                    
                    is CustomResult.Progress -> {
                        Log.d("LoadUserDataService", "Progress: ${result.progress}%")
                        CustomResult.Loading
                    }
                }
            }
            .catch { e ->
                Log.e("LoadUserDataService", "Unexpected error in getCurrentUserStream", e)
                CustomResult.Failure(e)
            }
    }
}